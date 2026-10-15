# Project Socrates development checkout

The latest supplied source is in [source/](source/README.md). Original 0.3.0
production artifact: [compiled/socrates-0.3.0.jar](compiled/socrates-0.3.0.jar).
Original upstream verification is in `source/evidence/`; current work and remaining
acceptance checks are tracked in [PROGRESS.md](PROGRESS.md).

## Cloud development

From `/workspace/My-project`:

```bash
bash dev/prepare-cloud.sh
bash dev/gradle-cloud.sh clean build
cd source
python3 -m unittest discover -s ai -v
SOCRATES_PROVIDER=offline python3 -u ai/service.py --instance run-gametest
```

Keep the service in a separate terminal/session for the 13-test bridge suite:

```bash
cd /workspace/My-project
bash dev/gradle-cloud.sh runGameTestServer -PbridgeTests -x downloadAssets
```

The base engine suite omits `-PbridgeTests`. These commands create only disposable
project-local test worlds. Java 17 and Gradle 8.8 downloads are checksum-verified;
the cloud helper uses the environment's proxy and system Java trust store, with
TLS verification enabled. Standard Windows builds still use the original wrapper
and the instructions in `source/README.md`.

## Disposable cloud graphics

The base cloud image supplies Xorg's dummy driver and Mesa software rendering.
In one managed session, from this checkout:

```bash
mkdir -p /workspace/socrates-runtime/shader-cache
/usr/lib/xorg/Xorg :99 -config /workspace/My-project/dev/xorg-dummy.conf \
  -logfile /workspace/socrates-runtime/Xorg.log -nolisten tcp -noreset
```

In another terminal:

```bash
export DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1
export MESA_SHADER_CACHE_DIR=/workspace/socrates-runtime/shader-cache
python3 dev/graphics-smoke.py
```

This tests a real GLX context, **not Minecraft rendering**. A development
`bash dev/gradle-cloud.sh runClient` is a separate check; installed-production-JAR
and Windows Prism/modpack validation remain separate acceptance requirements.
See `source/docs/PRISM-TEST-CHECKLIST.md`. No public localhost preview is needed.

Latest milestone: [0.3.2 interaction menu and conversation topics](source/docs/TEST-RESULTS-0.3.2.md).
