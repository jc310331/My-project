# Architecture — current 0.4.0-preview.1

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

## 0.4.0-preview.1 additions

CompanionInventoryMenu uses ordinary Minecraft server-owned slot transactions,
with nine storage slots, six equipment slots and the player's inventory. The server
checks owner/alive/dimension/range for validity and clicks. No private inventory is
included in generic public entity packets. Shift-click can equip axes/armor.

OakHarvestJob is a separate bounded MOVE/LOOK goal, reached through a proxy because
Mob registers goals before subclass fields initialize. It stores job ID, origin,
quantity, actual inserted fresh-drop count, state, remaining work ticks, approved
trunk positions and personal/owner handover markers in entity NBT. Cancellation
invalidates the task. Completed jobs return to ordinary following. Failed path
targets are skipped for that attempt; a new explicit job resets the work area.

The adapter recognizes only rooted straight 2–6-block vanilla oak trunks with
nonpersistent oak leaves, within loaded terrain near the starting point. This is
heuristic classification, not construction protection. It uses line of sight,
reach, actual break progress and a survival Forge FakePlayer's destroyBlock path
for events, tool wear and drops. Only fresh oak drops near that specific break,
actually inserted into storage, count toward progress. Full storage blocks breaks.

KitCrafting constructs a bounded supported recipe grid from cloned inventory,
checks the installed recipe/result and remainders, simulates available output room,
then commits consumed inputs/output on the server thread. No ingredients are
created. A visible crafting table must be within reach. Supported equipment is a
wooden axe/pickaxe and iron chestplate; general recipe automation is not implemented.

WorldObservation snapshots health, mode/task, dimension/position, combined item
counts, tool/armor presence, workbench reach and oak outcome. Python accepts an
optional bounded observations field while legacy four-field requests remain valid.
The only new actions are craft_kit and oak_logs:N for N in 1..64; Java validates
this grammar independently. Free-form commands/coordinates are never accepted.

Personal autonomy is authored locally in Python even when the conversation
provider is cloud-backed. It persists one initial travel-kit/wood-reserve objective
and phases in SQLite. The owner enables it explicitly. It chooses existing tested
adapters, pauses for blockers/low observed health and preserves completed requested
logs until the inventory has been emptied once. It does not promise general
self-directed survival or invented long-term ambitions. It is checked about every
200 ticks while idle and with the owner nearby. Stay cancels work and autonomy.

SQLite action_results is a scoped idempotent mirror of oak observations delivered
with later requests. NBT is authoritative; there is no reliable immediate result
push/retry/outbox protocol yet. Legacy proposed-goal rows remain proposals.
Player2/PlayerEngine was inspected externally but no external code/dependency was
copied into this preview. Evaluate that engine before duplicating broader systems.
