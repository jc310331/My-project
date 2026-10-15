> Original 0.3.0 handoff retained below. The latest continuation is 0.3.1; read ../START-HERE.md and docs/TEST-RESULTS-0.3.1.md.

# Project Socrates — handoff to the next coding agent / Dot

Read README.md, docs/TEST-RESULTS.md and docs/ARCHITECTURE.md first.
This source is the latest **0.3.0** continuation of Jacob's unverified v0.2 archive.
Use it as the starting point. Do not restart from v0.2 without a concrete reason.

## User objective
A genuinely autonomous AI companion for SINGLE-PLAYER Minecraft Java 1.20.1,
Forge 47.4.10, Java 17. Owner uses Prism on Windows; modpack includes Create 6.0.8,
Embeddium and Oculus. Name Socrates, classic four-pixel arms, default username skin
Socrates; username changes and PNG imports. Long term: conversation/memory,
self-initiated goals, exploring, gathering, crafting, building and modded interactions.

## Work completed
Actual Forge MDK obtained, full Java 17 JDK and Gradle acquired, source repaired
and expanded, compiled/reobfuscated JAR built. Seven real headless Minecraft
GameTests and 15 Python tests passed. Evidence is included. The local AI service
was tested from Minecraft over authenticated loopback HTTP, including SQLite recall.
Current autonomy and collection are deliberately limited; review the result report.

## Important remaining checks
A graphical client could not start in the cloud environment (GLFW initialization
failure). Skins/animations/render layers have NOT been visually verified. Production
JAR installation in Prism and full modpack compatibility remain untested.
Gemini/OpenAI adapters have NOT been tested against live provider accounts.
Never convert implemented or compiled features into claims of gameplay verification.

## How to continue
Establish a persistent source repository/environment if needed, preserving this
release and logs. Rebuild using Java 17 and the included Gradle wrapper, rerun the
engine/Python tests, then perform a disposable graphical-client smoke test.
Progress through milestones with explicit acceptance checks. Fix concrete defects
before expanding capabilities. Keep a progress log and evidence with each release.

Never change Jacob's main modpack or survival save. Use a separate test instance
and new world. Do not embed credentials; use local service environment variables.
Avoid exposing the loopback bridge. No requirement to keep a public server running.

## Build/testing
Windows: .\gradlew.bat clean build
Linux: ./gradlew clean build
Engine: ./gradlew runGameTestServer -x downloadAssets
Python: from ai/, python -m unittest -v
Optional bridge suite: start `python ai/service.py --instance run-gametest` from
the project root, then `./gradlew runGameTestServer -PbridgeTests -x downloadAssets`.
See README for Windows equivalents. The bridge tests require that local service;
the normal six-test engine suite does not. All test worlds live inside this project.

The prior cloud development session disconnected during the failed graphical
launch and subsequently recovered. The latest source and JAR were recovered;
this handoff does not require that old transient environment to keep running.
