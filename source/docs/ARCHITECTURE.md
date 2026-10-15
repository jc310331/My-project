# Architecture — current 0.3.2

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
1. Complete Windows Prism/modpack and broader terrain/animation testing.
   Linux production-JAR rendering, PNGs/equipment, flat-platform owner following,
   and async offline conversation have now been exercised; see the 0.3.1 report.
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

## 0.3.1 scheduler correction
Custom follow, home-return and collection goals request updates every tick, while
path requests remain throttled to 10/20 ticks. This avoids an entity-ID phase in
Minecraft's alternating goal scheduler that never coincided with the modulo timer.
Collection also revalidates the item on every tick before inserting it; goal
eligibility itself is checked less frequently. An engine regression uses opposite
entity-ID phases and per-run tagged stacks to assert exact counts/no duplication.

Dropped-item navigation requests an exact target block (reach tolerance zero),
while pickup still requires the original distance and line-of-sight checks. An
off-center engine fixture reproduces the production approach that previously
stopped short; the final production JAR was also retested at that location.

## 0.3.2 interaction menu

SocratesEntity.mobInteract opens an owner-only client screen through a bounded
PLAY_TO_CLIENT packet. Client buttons send a UUID, action enum and bounded text;
there are no arbitrary commands. The PLAY_TO_SERVER handler enqueues work on the
server thread, finds the loaded entity in the player's dimension, and validates
owner, alive/non-spectator state and six-block range. Unload additionally requires
four blocks. Menu access does not require operator permission. Command shortcuts
retain their original permissions.

InteractionActions performs existing entity/inventory operations. Talk enters the
existing asynchronous AiBridge, whose state revision and ownership checks remain.
Responses are delivered to the matching menu and retained in chat diagnostics;
menu replies omit diagnostic acceptance suffixes. Every 20 client ticks a bounded,
quiet REFRESH requests authoritative mode/task/item-count/autonomy status. Rejected
refreshes do not reveal another owner's status or overwrite known state.

The client screen does not pause the world. Its conversation transcript is local
to the open screen (30 entries); SQLite facts/exchanges remain in Python. Client
rendering/input/portrait code is isolated behind Dist.CLIENT dispatch; the real
headless engine suite checks dedicated-server loading. No new autonomy, mining,
crafting, building or Create capabilities were added by the menu.
