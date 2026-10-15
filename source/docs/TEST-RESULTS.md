> Historical 0.3.0 report, preserved from the supplied handoff. Current continuation results are in [TEST-RESULTS-0.4.0-preview.1.md](TEST-RESULTS-0.4.0-preview.1.md).

# Verified results — 2026-10-14

## Environment
Isolated Linux cloud workspace, not Jacob's PC. Exact Forge 47.4.10 MDK;
Minecraft 1.20.1; Eclipse Temurin JDK 17.0.20.1; Gradle 8.8;
ForgeGradle 6.0.54; official 1.20.1 mappings. No main Prism installation or
survival world was accessed. Python tests use standard-library unittest.

## Passed
- Java compilation and Gradle build, including reobfJar; compiled mod JAR produced.
- Real Forge/Minecraft headless GameTest runtime: **all 7 required tests passed**.
- Python unit/HTTP integration tests: **15 passed**.
- Gradle distribution SHA-256 matched the distribution's published checksum.

Seven Minecraft GameTests:
1. NBT round trip: owner, mode, home, skin setting, held item, chest armor.
2. Stay: no voluntary horizontal wandering over 80 ticks on a prepared floor.
3. Navigation: path creation and actual horizontal movement on a flat prepared floor.
4. Collect: navigate to three dropped oak logs; store three; discard the original
   item entity so it is not duplicated.
5. Task/inventory persistence and Stay cancellation of task, autonomy and revision.
6. Java action validation rejects unsupported actions and extra fields.
7. Java-to-Python authenticated loopback round trip: follow intent and SQLite fact recall.

Fifteen Python tests cover exact simple intents, unknown requests, SQLite restart
persistence, world isolation, saved autonomous proposals, invalid scope, unsupported
or extra action fields, reply-length limit, mocked invalid cloud output, authenticated
HTTP, wrong token, browser Origin rejection, oversized bodies and malformed JSON.
No live cloud API credentials were used.

Evidence: ../evidence/final-engine.log and ../evidence/python-tests-final.log.
The final engine run exited 0 and reported BUILD SUCCESSFUL in 35s.
The build uses no ordinary JUnit test classes; Gradle `test NO-SOURCE` is not
counted as a passing test suite. Engine tests are explicit GameTests.

## Debugging history
The navigation fixture initially failed: the entity dropped below the expected
floor. A delayed navigation request alone did not fix movement. Explicitly placing
the test floor/air volumes fixed the fixture. The baseline three-test suite then
passed. Tests use Adventure mode without an owner to isolate directed navigation
from resident random wandering. This does not prove following a real player over
arbitrary obstacles. Earlier failed and corrected-run logs are included.

## Not verified / limitations
- Graphical client launch attempted; GLFW failed to initialize graphics in the
  container (XDG_RUNTIME_DIR/display unavailable). Forge's console fallback repeated
  prompts; the process was interrupted. See client-failure-excerpt.log.
- Player geometry, walking/head animations, armor rendering, username skin download,
  local PNG display and legacy conversion are implemented/compiled, NOT visually verified.
- The reobfuscated distributable JAR was built; GameTests ran dev classes, not an
  installed production JAR in Prism. Production installation still needs a smoke test.
- Full command/UI flow, real-owner follow, danger avoidance, complex terrain,
  overnight routines, long-duration autonomy, edge-of-boundary behavior and real
  unload/reload lifecycle need additional gameplay testing.
- Bridge transport was tested in-engine. Asynchronous `/socrates talk` application
  with a real single-player owner has not been exercised end-to-end in a client.
- Gemini/OpenAI adapters are implemented; live API calls are untested.
- Create 6.0.8, Embeddium, Oculus and Jacob's full modpack were not installed/tested.
- Collection means dropped items only. No mining, crafting, building, combat,
  cross-dimension following, Create adapters or general autonomous player abilities.

This is an experimental foundation, not completion of the long-term vision.
