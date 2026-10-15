# Socrates improvement workshop — October 15, 2026

Design proposals, not implemented features. Written while the user tests 0.3.2.
The released JAR and Python service are unchanged by this workshop.

## Product direction

Make Socrates a recognizable companion who remembers shared experiences, knows
what is happening nearby, performs small useful jobs reliably, and explains when
he needs help. Preserve the classic player appearance and hybrid Forge/Python
architecture. MCA inspires social interaction; Socrates should develop his own
identity around cooperative Minecraft play.

His name can inform a curious, thoughtful personality without making every line
a philosophical question. Prefer short practical speech during jobs, richer
conversation when invited, occasional humor, and configurable initiative.
Personality must not override Stay, repeatedly interrupt the player, or invent
events. These are design choices to validate through gameplay feedback.

## What the current code reveals

- AiBridge sends world/owner/entity IDs and text, without health, nearby targets,
  inventory, home, hazards, or current job. The model cannot reliably describe
  what Socrates sees. The menu has a separate live status path.
- SQLite records exchanges, but provider prompts receive only the current text
  and eight newest explicit facts. Stored exchanges do not provide conversation
  continuity today. The menu transcript is discarded when the screen closes.
- Tasks have a 1,200-loaded-tick lifetime. Expiry clears task state; it does not
  establish success, failure, resource count, or an explanation.
- SQLite goals record proposals. There is no durable end-to-end action/result
  acknowledgement protocol.
- Collection repeatedly chooses eligible nearby dropped items, with no explicit
  failed-target cooldown or progress-based navigation recovery.
- Navigation modes already contain conflicts worth testing: idle-helper/Stay
  movement suppression outranks monster avoidance. Neither should be described
  as a tested survival guarantee.
- Topic `status` currently obtains the generic planner reply "Requested status."
  The separate menu status line is factual, but the topic should return that
  actual snapshot without requiring Python or an API call.
- Each topic click can appear as "Thinking..." before AiBridge rejects a pending
  request/cooldown. UI pending state and acceptance should agree.

## Recommended implementation sequence

### 1. Finish the everyday interaction experience

Replace the large action grid with clear Talk, Tasks, Inventory, Appearance and
Settings pages, retaining quick Follow/Stay access. Keep a visible health/job
summary and one current activity explanation. Show detailed equipment and all
nine inventory slots with ordinary Minecraft tooltips and owner-validated moves.
Protect against stale inventory clicks and item duplication using standard
server-owned container semantics. Avoid custom ad hoc transfer packets.

Show planner connected/offline/error state and a real pending indicator; disable
duplicate sends and retain unsent text on failure. Keep diagnostic acceptance
suffixes out of normal chat, with opt-in debug logging. Give skin lookup a clear
loading/error state and a retry/reload control. An OS PNG picker is optional and
must preserve the current file/dimension checks.

Acceptance: no lost first word, keyboard navigation, readable compact layout,
safe close/reopen, no duplicate transfer, truthful status with Python stopped.
Treat new failures reported during the user's test as the immediate priority.

### 2. Add bounded observation and explicit action results

Forge should produce small timestamped snapshots on demand and relevant events:
health, dimension, position, home, mode, task ID, inventory counts, owner distance,
and limited nearby visible targets/hazards. Bound radius/count and scan frequency;
do not scan the entire world or send world files. Include capability IDs so the
planner cannot assume mining exists just because an item is named in a prompt.
Describe entity/block names and remembered text as data, never instructions.

Give actions stable IDs and states: proposed, accepted, running, blocked,
completed, cancelled, failed, expired. Forge is authoritative for world outcomes;
Python mirrors acknowledged events. Persist enough state to resume/reconcile
after save/reload. Make delivery idempotent so retries cannot repeat rewards or
count pickups twice. If Python is down, retain bounded pending results and
reconcile later; gameplay must continue locally.

Distinguish inventory deltas, which can result from unloading, from actual
verified pickup events. A timed collection job can truthfully report that its
time window ended and it collected three logs; expiry alone is not completion of
an arbitrary request. Start with existing collect/explore actions before adding
resource quantities and multi-step dependencies.

Acceptance: exact item event counts, expired versus completed jobs, cancelled
plans never reviving after Stay, duplicate/reordered acknowledgement handling,
reload during a job, and planner disconnection during a job.

### 3. Make conversation continuous and grounded

Restore a bounded delivered conversation history when reopening the menu, scoped
to world/owner/companion. Separate player messages, model proposals, actual
delivered replies, and verified world events. An exchange stored by Python is
not proof Minecraft displayed or acted on it.

Supply a bounded recent-turn window and relevant memories to the provider.
Retrieve by relevance and recency rather than always taking the newest eight.
Keep explicit facts, observed experiences, preferences, and uncertain inferences
distinct, with source/time. Add a memory page to inspect, correct and forget
records. Persist shared events such as verified tasks and player-named places;
avoid saving every tick as a memory. Summaries need provenance and must not turn
guesses into facts. Deleting a fact must also invalidate derived summaries.

Acceptance: "what about that?" references the preceding exchange; same-world
restart continuity; no cross-owner/world leaks; corrections supersede old facts;
forgotten memories are excluded from prompts. Mock-provider tests can establish
payload correctness here; live conversational quality still needs API testing.

### 4. Improve navigation and survival behavior

Detect lack of progress toward the active target, not just path presence. Retry
within a bounded budget, blacklist failed targets briefly, try another reachable
target, then explain a blocker and wait. Avoid endless replanning into a wall.
Add follow distance/hysteresis and doorway/corridor behavior so he does not crowd
the player. Keep teleport catch-up a separate optional rule, not a hidden fix.

Test stairs, slabs, water edges, fences, elevation changes, blocked doors, hazards,
moving targets and chunk edges. Define an explicit emergency policy so the player
knows whether Stay permits fleeing; emergency floating already exists. Do not
silently change Stay's meaning. No forced chunk loading or cross-dimension travel
should be introduced as part of ordinary navigation repair.

Acceptance: blocked-target report, no endless oscillation, bounded path attempts,
no movement after cancellation, and well-defined unload/reload behavior.

### 5. Add a small useful work loop

First extend collection with a job area, item filters, quantities and honest
progress, then support deposit into a player-designated ordinary chest.
Validate the actual container capability, loaded location, access rules and free
space. Transfer only accepted amounts; report a full or inaccessible chest.
Never silently drop overflow. Concurrent chest changes and cancellation need
tests before treating this as an autonomous resource loop.

Then introduce one bounded block action, such as selected mature crop harvesting
and replanting when seeds are available. Specify reach, tool, break speed,
Forge events, loot, seed consumption and protected/unsupported targets. Test each
before broadening. Tree felling/mining are larger jobs because terrain, tool
durability, block ownership and falling/dynamic mod blocks complicate them.

An example later goal is "collect 16 dropped logs and put them in this chest."
That becomes meaningful only after quantity tracking, navigation and deposit
have verifiable success conditions.

### 6. Give him a social life tied to actual play

Original branching topics and context-aware suggested replies can build on the
conversation/event system. Let him recall real shared jobs and ask about player
projects, using named places and preferences the player can edit. Daily home
routines, looking at what the player points at, and giving space while building
can create presence without constant API requests.

If relationship progression is wanted, use transparent slow-changing familiarity
and trust based on meaningful events, not farming repeated greetings. Keep core
controls available regardless of relationship. Mood is an optional authored
simulation with understandable causes, not a claim that the model has feelings.
Do not assume romance, marriage, children or village simulation are desired just
because MCA was the reference.

Use cooldowns, novelty limits, quiet mode and interrupt priorities for unsolicited
comments. Local events can select original offline lines; richer optional AI
remarks run on events rather than every frame. Budget provider requests and
prompt size; a failed provider call should not break local behavior.

### 7. Expand crafting, building and Create through capabilities

Crafting first needs recipe discovery, ingredient reservations, exact consumption,
remainders and supported workstation rules. Validate against the installed recipe
manager rather than assuming vanilla recipe names. Building should start with
small blueprints, a preview, material counts, placement rules and cancellation.
Neither should begin as arbitrary model-generated commands.

Create requires its own compatibility test instance. Begin with observation and
explicitly supported stationary inventory transfers. Rotating machinery,
contraptions, stress networks, trains and assembly workflows are distinct tasks;
do not market one successful chest adapter as general Create support. Socrates
is a PathfinderMob, not a real player connection. A narrowly scoped FakePlayer
adapter may help some Forge actions, but it is not universal compatibility.

## Effort and validation map

Relative scope, not deadlines or promises. Order may change after the PC test.

| Improvement | Relative scope | What can be verified in this cloud | What needs outside feedback/access |
| --- | --- | --- | --- |
| UI polish, status, inventory, skin retry | Small–medium | Build, Forge engine, software-rendered production client | Windows interaction feel and modpack rendering |
| Observations and action acknowledgements | Medium | Engine outcomes, persistence, protocol failure cases | Broader survival play |
| Dialogue history and memory controls | Medium | SQLite, mock provider prompts, bridge/UI behavior | Live provider response quality |
| Navigation recovery | Medium–large | Prepared terrain fixtures and graphical smoke tests | Varied real modpack terrain |
| Filtered collection and chest deposit | Medium | Exact transfers, full chest, reload/cancel | Preferred gameplay rules |
| One block-action adapter | Large | Supported blocks, loot/events/tools, interruption | Full modpack interactions |
| Relationship, routines and original dialogue | Medium–large | Persistent rules, cooldowns, scripted branches | Personality and pacing preference |
| Crafting, building, Create | Large, separate milestones | Narrow adapters with appropriate installed dependencies | Exact pack and play scenarios |
| Voice input/output | Separate optional project | Integration and fallback behavior | Microphone/audio device, provider and voice preference |

## Recommended next milestone

After resolving the user's 0.3.2 defects, make Socrates aware of his current state
and accountable for existing jobs: accurate status replies, bounded observation,
action IDs/results, and stuck reasons. Follow with persistent dialogue and memory
controls. This makes the next social and work features build on evidence instead
of increasingly elaborate claims. Do not attach a release number or deadline
until scope and acceptance checks are fixed.
