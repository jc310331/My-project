#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../source"
export JAVA_HOME=/workspace/toolchains/jdk-17.0.16+8
export GRADLE_USER_HOME=/workspace/gradle-cache
export MESA_SHADER_CACHE_DIR=/workspace/socrates-runtime/shader-cache
args=(-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts)
# Use only the proxy host and port. Never copy proxy credentials into scripts/logs.
if [[ -n "${HTTPS_PROXY:-}" ]]; then
  mapfile -t proxy_parts < <(python3 - <<'PY'
import os, urllib.parse
p = urllib.parse.urlsplit(os.environ['HTTPS_PROXY'])
if p.username or p.password:
    raise SystemExit('Authenticated proxy URLs need supported platform configuration')
print(p.hostname)
print(p.port or 8080)
PY
  )
  [[ ${#proxy_parts[@]} -eq 2 ]]
  args+=("-Dhttps.proxyHost=${proxy_parts[0]}" "-Dhttps.proxyPort=${proxy_parts[1]}"
         "-Dhttp.proxyHost=${proxy_parts[0]}" "-Dhttp.proxyPort=${proxy_parts[1]}"
         '-Dhttp.nonProxyHosts=localhost|127.*|[::1]')
fi
exec bash /workspace/toolchains/gradle-8.8/bin/gradle "${args[@]}" --console=plain "$@"
