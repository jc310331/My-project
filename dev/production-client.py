"""Disposable Linux production-client harness; official, hash-checked artifacts.

Use --prepare before Forge's official --installClient, then --launch afterward.
No online account, multiplayer, existing Prism instance or personal world is used.
"""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
import urllib.error
import urllib.request
import urllib.parse
import uuid
import zipfile

ROOT = Path('/workspace/socrates-client')
JAVA = '/workspace/toolchains/jdk-17.0.16+8/bin/java'

def download(meta, path):
    path = Path(path)
    expected = meta['sha1']
    if path.exists() and hashlib.sha1(path.read_bytes()).hexdigest() == expected:
        return
    for attempt in range(3):
        try:
            with urllib.request.urlopen(meta['url'], timeout=60) as response:
                data = response.read()
            break
        except urllib.error.HTTPError as error:
            if error.code not in (429, 500, 502, 503, 504) or attempt == 2:
                raise
            time.sleep(1 + attempt)
    if hashlib.sha1(data).hexdigest() != expected:
        raise ValueError('Checksum mismatch: ' + str(path))
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + '.part')
    temporary.write_bytes(data)
    temporary.replace(path)

def allowed(item):
    rules = item.get('rules')
    if rules is None:
        return True
    result = False
    for rule in rules:
        system = rule.get('os', {})
        if system.get('name', 'linux') != 'linux':
            continue
        if 'arch' in system and not re.search(system['arch'], 'x86_64'):
            continue
        if 'version' in system and not re.search(system['version'], os.uname().release):
            continue
        if any(value for value in rule.get('features', {}).values()):
            continue
        result = rule['action'] == 'allow'
    return result

def prepare():
    with urllib.request.urlopen('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json', timeout=30) as response:
        manifest = json.load(response)
    version = next(v for v in manifest['versions'] if v['id'] == '1.20.1')
    path = ROOT / 'versions/1.20.1/1.20.1.json'
    download(version, path)
    data = json.loads(path.read_text())
    jobs = [(data['downloads']['client'], ROOT / 'versions/1.20.1/1.20.1.jar')]
    for lib in data['libraries']:
        if allowed(lib) and 'artifact' in lib.get('downloads', {}):
            artifact = lib['downloads']['artifact']
            jobs.append((artifact, ROOT / 'libraries' / artifact['path']))
    asset = data['assetIndex']
    index = ROOT / 'assets/indexes' / (asset['id'] + '.json')
    download(asset, index)
    objects = json.loads(index.read_text())['objects']
    unique = {obj['hash'] for obj in objects.values()}
    for key in unique:
        jobs.append(({'sha1': key, 'url': 'https://resources.download.minecraft.net/' + key[:2] + '/' + key}, ROOT / 'assets/objects' / key[:2] / key))
    print('Preparing', len(jobs), 'checksum-verified client/library/asset files', flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        futures = [pool.submit(download, *job) for job in jobs]
        for count, future in enumerate(concurrent.futures.as_completed(futures), 1):
            future.result()
            if count % 250 == 0:
                print('Verified', count, '/', len(jobs), flush=True)
    print('Vanilla production prerequisites prepared', flush=True)

def launch(world=None, skin_proxy=False):
    version_name = '1.20.1-forge-47.4.10'
    vanilla = json.loads((ROOT / 'versions/1.20.1/1.20.1.json').read_text())
    forge = json.loads((ROOT / 'versions' / version_name / (version_name + '.json')).read_text())
    # Forge versions override same-group/artifact vanilla library versions.
    libraries = {}
    for lib in vanilla['libraries'] + forge['libraries']:
        if allowed(lib):
            key = ':'.join(lib['name'].split(':')[:2])
            if len(lib['name'].split(':')) > 3:
                key += ':' + lib['name'].split(':')[3]
            libraries[key] = lib
    classpath = []
    natives = ROOT / 'natives'
    natives.mkdir(exist_ok=True)
    for lib in libraries.values():
        artifact = lib.get('downloads', {}).get('artifact')
        if not artifact:
            continue
        path = ROOT / 'libraries' / artifact['path']
        # Some installer-produced artifacts have no published digest in version.json.
        if artifact.get('sha1'):
            download(artifact, path)
        if not path.is_file():
            raise FileNotFoundError(path)
        classpath.append(str(path))
        if 'natives-linux' in path.name:
            with zipfile.ZipFile(path) as archive:
                for entry in archive.infolist():
                    if entry.filename.endswith('.so'):
                        (natives / Path(entry.filename).name).write_bytes(archive.read(entry))
    # Standard launchers name the inherited main JAR after the selected version;
    # Forge's official ignoreList matches that name to exclude the raw client
    # module while loading the installer-patched Minecraft libraries.
    main_jar = ROOT / 'versions' / version_name / (version_name + '.jar')
    shutil.copy2(ROOT / 'versions/1.20.1/1.20.1.jar', main_jar)
    classpath.append(str(main_jar))
    values = {
        'natives_directory': str(natives), 'launcher_name': 'SocratesDisposableTest',
        'launcher_version': '1', 'classpath': os.pathsep.join(classpath),
        'classpath_separator': os.pathsep, 'library_directory': str(ROOT / 'libraries'),
        'auth_player_name': 'SocratesTest', 'version_name': version_name,
        'game_directory': str(ROOT), 'assets_root': str(ROOT / 'assets'),
        'assets_index_name': vanilla['assetIndex']['id'],
        'auth_uuid': str(uuid.uuid3(uuid.NAMESPACE_DNS, 'SocratesTest')).replace('-', ''),
        'auth_access_token': '0', 'clientid': '', 'auth_xuid': '',
        'user_type': 'legacy', 'version_type': 'release',
    }
    def expand(arguments):
        result = []
        for arg in arguments:
            if isinstance(arg, dict):
                if not allowed(arg):
                    continue
                items = arg['value']
                if isinstance(items, str):items = [items]
            else:items = [arg]
            for item in items:
                for key, value in values.items():
                    item = item.replace('${' + key + '}', value)
                if '${' in item:raise ValueError('Unresolved launcher argument')
                result.append(item)
        return result
    args = [JAVA, '-Xmx2G', '-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts']
    if os.environ.get('HTTPS_PROXY'):
        proxy = urllib.parse.urlsplit(os.environ['HTTPS_PROXY'])
        if proxy.username or proxy.password:
            raise ValueError('Use supported platform proxy configuration')
        for scheme in ('http', 'https'):
            args += ['-D' + scheme + '.proxyHost=' + proxy.hostname,
                     '-D' + scheme + '.proxyPort=' + str(proxy.port or 8080)]
        args += ['-Dhttp.nonProxyHosts=localhost|127.*|[::1]']
    args += expand(vanilla['arguments']['jvm'] + forge['arguments']['jvm'])
    args += [forge['mainClass']]
    args += expand(vanilla['arguments']['game'] + forge['arguments']['game'])
    args += ['--width', '1280', '--height', '720']
    if skin_proxy:
        args += ['--proxyHost', '127.0.0.1', '--proxyPort', '1080']
    if world:
        if not re.fullmatch(r'[A-Za-z0-9_-]{1,64}', world):
            raise ValueError('Use a simple disposable world name')
        args += ['--quickPlaySingleplayer', world]
    print('Launching production Forge client in disposable instance; no account session', flush=True)
    raise SystemExit(subprocess.call(args, cwd=ROOT))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--prepare', action='store_true')
    group.add_argument('--launch', action='store_true')
    parser.add_argument('--world', help='Existing disposable save name for quick play')
    parser.add_argument('--skin-proxy', action='store_true', help='Use the local skin-only Minecraft proxy adapter')
    args = parser.parse_args()
    ROOT.mkdir(parents=True, exist_ok=True)
    prepare() if args.prepare else launch(args.world, args.skin_proxy)
