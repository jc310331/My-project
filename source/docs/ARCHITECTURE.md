# Architecture — current 0.3.0

Forge owns the physical entity, navigation, equipment, nine-slot inventory, home,
mode, current task and remaining task ticks. Entity state is persisted in world NBT.
CompanionRegistry (SavedData) stores per-owner identity plus a world UUID used for
memory isolation. Client-only classes own player rendering and cached skin textures.

Python owns explicit memories, conversation/proposal records and a proposed-goal
ledger in SQLite. Default planning is deterministic offline logic. Optional fixed-host
Gemini and OpenAI adapters read credentials from environment variables.

AiBridge sends bounded requests to fixed 127.0.0.1:8765, uses a generated bearer
token, rejects redirects and limits response sizes. The service rejects browser
Origin headers, validates input UUIDs and bounds request size/concurrent planning.
Both sides validate a fixed action enum. No arbitrary command or world-mutation
script is accepted. Asynchronous responses apply on the Minecraft server thread
only after ownership, alive/loaded state, same dimension, distance and revision checks.
Stay invalidates pending plans. This bridge is restricted to single-player servers.

The current actions are none, follow, stay, resident, home, status, collect and
explore. Collect only picks up nearby dropped ItemEntities. Explore is restricted
vanilla wandering near home. Both tasks expire after 1,200 loaded ticks and save
remaining time. Autonomy is opt-in and requests a plan when idle with its owner nearby.
Offline autonomy alternates collect/explore proposals; it is not reasoning by an LLM.

## Next engineering priorities
1. Install the production JAR in a disposable client; verify rendering/skins,
   actual owner-follow/commands, save/reopen behavior, and full async talk flow.
2. Add precise action acknowledgements so SQLite distinguishes proposed, accepted,
   running, completed and failed goals. Do not treat proposals as accomplished work.
3. Improve stuck detection, real observation/state snapshots, unloaded entity
   location reporting, skin reload/error UX and bounded navigation robustness.
4. Add stronger terrain/boundary/inventory and command lifecycle tests.
5. Introduce deliberate block-interaction adapters with tool/reach/inventory checks,
   event/permission handling, loot correctness and interruption/recovery tests.
6. Test Create/Embeddium/Oculus incrementally before the full modpack.

Crafting, building and Create machinery require explicit tested adapters. A
PathfinderMob is not a network player; do not promise general mod compatibility
or human-like capabilities from the current goal set.
