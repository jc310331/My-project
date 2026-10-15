# Disposable Linux production client

This is a cloud test harness, **not** the user's Prism instance. Use only
`/workspace/socrates-client` and disposable worlds. Windows instructions remain
in `source/README.md` and its Prism checklist. The client uses a dummy offline
test identity, not a personal account or multiplayer session.

## Prepare

From `/workspace/My-project`, run `bash dev/prepare-cloud.sh` and build with
`bash dev/gradle-cloud.sh build`. The production harness verifies Mojang's
manifest, client, library and asset SHA-1 metadata over verified HTTPS:

```bash
python3 dev/production-client.py --prepare
```

Use Forge's official 1.20.1-47.4.10 installer. The tested installer SHA-256 is
`1912760b4cb6b803d8a826de603c9076b1da71ec2765e9a1f8c1ca78f65278e3`.
It was also checked against the official `.sha1` before use.

```bash
curl -fL https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-installer.jar \
  -o /workspace/toolchains/forge-1.20.1-47.4.10-installer.jar
printf '%s  %s\n' 1912760b4cb6b803d8a826de603c9076b1da71ec2765e9a1f8c1ca78f65278e3 \
  /workspace/toolchains/forge-1.20.1-47.4.10-installer.jar | sha256sum --check
```

Forge expects a `launcher_profiles.json` in that new test folder. Create
`{"profiles":{}}` only if it does not already exist; preserve existing files.
Run the tested installer command from `/workspace/socrates-runtime`:

```bash
/workspace/toolchains/jdk-17.0.16+8/bin/java \
  -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts \
  -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 \
  -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 \
  -jar /workspace/toolchains/forge-1.20.1-47.4.10-installer.jar \
  --installClient /workspace/socrates-client
```

The host/port above are the current platform proxy. If the environment changes,
use its supplied proxy host/port; do not copy credentials or disable verification.
The installer may log a denied optional mirror and then successfully fall back to
Forge's official host. Require its final success and process exit 0.

Copy **only one** Socrates version into the test folder's `mods/`. Build JAR:
`source/build/libs/socrates-0.3.1.jar`. Do not include source JARs. The two fixture
PNGs in `dev/fixtures/` can go into the test folder's `config/socrates/skins/`.

## Start and inspect

Start the dummy Xorg display as described in the root README. In separate managed
sessions, run these from the repository root:

```bash
python3 -u dev/minecraft-proxy.py
SOCRATES_PROVIDER=offline python3 -u source/ai/service.py --instance /workspace/socrates-client
```

Minecraft's authentication/skin libraries use an explicit SOCKS proxy rather than
the environment HTTP proxy. The disposable adapter listens on **127.0.0.1:1080**
and routes only named Mojang profile/key/texture destinations through the existing
platform proxy. Its allowlist remains enforced. Legacy HTTP texture requests are
upgraded to verified HTTPS on the same fixed texture host. No TLS or profile
signature checks are disabled. This adapter is a cloud test helper, not a mod or
a requirement for normal Windows Prism use.

Launch the real installed production Forge profile:

```bash
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 LP_NUM_THREADS=2 \
MESA_SHADER_CACHE_DIR=/workspace/socrates-runtime/shader-cache \
python3 dev/production-client.py --launch --skin-proxy
```

Create a disposable Creative world with cheats. Add `--world SocratesSmoke` only
after that disposable save exists. The recorded first fixture used a copy of an
already-generated **test** world, enabled cheats in its copied `level.dat`, removed
the old GameTest companions, and built a stone platform at Y=99. No personal world
was used. Do not assume the copied world spawns safely; the first test player
drowned before it was switched to Creative. Prefer a newly created Creative world.

`dev/client-input.py` targets only the Minecraft window on display :99:

```bash
python3 dev/client-input.py --command 'socrates spawn'
python3 dev/client-input.py --command 'socrates stay'
python3 dev/client-input.py --command 'socrates skin "local:smoke-64.png"'
python3 dev/client-input.py --key Escape
python3 dev/client-input.py --click 640 445
```

Mouse coordinates depend on window size and GUI scale; inspect before clicking.
Use `import -display :99 -window root /workspace/socrates-runtime/check.png` and
inspect the resulting screenshot. Wait for rendered state before recording evidence.
No localhost preview link is necessary. Logs and screenshots must distinguish
fallback Steve from a successfully fetched username skin.

Use Save and Quit to Title and wait for all dimensions to save before replacing
a mod JAR or copying worlds. Quit the game before replacing its JAR; restart after
a failed cached skin lookup. Stop the service/proxy/display processes you started
when finished; they do not survive publication or a new task.
