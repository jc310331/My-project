# Project Socrates 0.3.1 — results, 2026-10-15

These results were obtained in an isolated Linux cloud task. No personal Prism
instance or survival world was accessed. Historical 0.3.0 evidence is preserved.
All new evidence is in `../evidence/2026-10-15/`.

## Passed

- Java 17.0.16+8, Forge 47.4.10, Minecraft 1.20.1, ForgeGradle 6.0.54,
  Gradle 8.8. Official JDK and Gradle downloads were SHA-256 verified.
- Clean baseline build and 0.3.1 build, including `reobfJar`, exit 0.
- **Nine required Minecraft/Forge engine GameTests passed**, exit 0, in final
  `build-engine-final-diagonal.log`, including the rebuilt reobfuscated JAR.
  This includes the original seven, an opposite-entity-ID/exact-pickup-count
  regression, and an off-center diagonal approach regression. Earlier eight-test
  corrected runs also passed before the additional production defect was found. `test NO-SOURCE` is not counted as a test suite.
- Original Python suite: **15 passed**. Final hardened suite: **17 passed**.
  Redirect transport test covers 301/302/303/307/308 and verifies that a fake
  credential reaches only the original local endpoint. Non-ASCII authorization
  returns 403 and a subsequent authorized request still works.
- Standalone planner startup and authenticated loopback follow/memory/recall.
- Official Forge installer successfully prepared a separate production client at
  `/workspace/socrates-client`. The reobfuscated 0.3.1 JAR was installed in its
  `mods/` directory and launched with production `forgeclient`, not dev classes.
- Graphical client opened a disposable single-player world under Xorg dummy/Mesa
  llvmpipe. Actual OpenGL 4.5 core-profile startup and rendered world verified.
- Classic player shape/four-pixel arms, imported **64x64 and legacy 64x32 PNGs**,
  held iron pickaxe and iron chestplate display were visually inspected.
  Screenshots are in `screenshots/`. Fixtures are in the handoff root's `dev/fixtures/` directory.
- Real-owner `/socrates spawn`, `stay`, skin setting, status, and following were
  exercised. Async `/socrates talk follow me` was accepted on the server thread;
  Socrates moved from block `2,100,-4` to `5,100,-5` after the owner moved to
  `8.5,100,-3.5` on the flat test platform. This does not validate arbitrary terrain.
- Async talk stored `Socrates cloud smoke milestone` and recalled it in chat.
- Save/quit and reopen exercised an actual production world lifecycle. Status
  preserved mode/home/skin; equipment and memory checks were repeated after reload.

## Defects reproduced and corrected

The original collection test failed in fresh worlds while passing on another run.
Minecraft's `Mob.serverAiStep` staggers ordinary goal updates by server tick plus
entity ID. Goals using `tickCount % 10/20` could therefore never request a path in
one scheduling phase. Follow, home-return and collection now request per-tick goal
updates while retaining their existing path-request intervals.

A deterministic regression creates consecutive entity IDs, places two separate
three-log stacks and requires each companion to collect exactly three, with both
original items removed. The first scheduler correction exposed a second issue:
an already-discarded item could be inserted again on the next tick before goal
eligibility was refreshed. Collection now revalidates eligibility on **each** tick.
Per-run tagged birch stacks distinguish this regression from other fixtures and
old world drops. Failed attempts and the corrected runs are retained separately.

A production pickup then stopped around 1.6 blocks from the item. Vanilla navigation
accepts a path ending one block away, which can leave an off-center approach outside
the unchanged 1.5-block pickup reach. The first centered fixture missed this; a
fixture using the measured off-center approach reproduced it. Collection now
requests `createPath(item, 0)` instead of the one-block-tolerance entity overload.
The corrected engine test passes, and the final production JAR collected exactly
three oak logs from the previously failing location. Unload transferred them; a
read-only player inventory check reported exactly three. Failed/corrected client
and engine evidence are retained.

Python provider requests now reject redirects. Previously a 301/302/303 could
forward an Authorization header to a redirect destination. The regression uses
only a fake local credential; no real key was exposed. Non-ASCII bearer headers
also previously caused an unhandled exception instead of an HTTP error.

## Unverified / remaining limits

- Username-skin testing initially failed because Mojang public-key retrieval was
  denied and legacy texture HTTP transport failed. The domain requirement was saved,
  key access later succeeded, and the cloud-only proxy adapter now upgrades texture
  requests to verified HTTPS. **Notch username skin was visually verified** on the
  final production JAR, with TLS and profile-signature checks enabled. Default
  **Socrates username display also passed in a fresh client session**. Both
  screenshots are included; the fresh default-skin log has no signature or texture
  download errors. No verification was bypassed.
- Windows Prism installation and Create 6.0.8/Embeddium/Oculus/full pack have not
  been tested. This production test was Linux, Forge plus Socrates only.
- No live Gemini/OpenAI calls; keys were absent. Offline and mocked-provider tests
  do not establish provider API compatibility or availability.
- Audio device initialization failed in this cloud image. Visual checks succeeded;
  audio was not validated. No full animation/pose matrix or long-duration gameplay
  test was performed.
- Complex terrain, danger avoidance, navigation recovery, cross-dimension behavior,
  overnight routines and long-duration autonomy remain unverified.
- Strong observation and acknowledged proposed/accepted/running/completed/failed
  goal lifecycle are not implemented. SQLite still records proposals only.
- Collection is dropped items only. No mining, crafting, building, combat or Create
  machine interaction was added. These require separately validated adapters.

## Artifacts

Production JAR: `compiled/socrates-0.3.1.jar` in the handoff root.
SHA-256: `2cb84ad65d72e99c551711de66c71d3d8404409936b5356e00ad4279eeb86e66`.
Complete source, fixtures, setup helpers and dated evidence are included in the
versioned handoff. The original 0.3.0 JAR/hash remain unchanged.

Saved environment configuration is distinct from publication and fresh-task
restoration. Neither publication nor an unattended background runner is verified.
