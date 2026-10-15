# Project Socrates 0.4.0-preview.1 — experimental test release

Minecraft Java **1.20.1**, Forge **47.4.10**, Java **17**.
This is a real compiled Forge mod with a separate optional Python planner.
It is an early companion, not yet a general-purpose AI Minecraft player.

## Install safely in Prism on Windows

1. Create a **new** Prism instance named `Socrates Test`: Minecraft 1.20.1,
   Forge 47.4.10. Select a **Java 17** installation in instance settings.
2. Open that instance's Minecraft folder. Put `socrates-0.4.0-preview.1.jar` in `mods`.
   Install only the normal JAR, not the source JAR or ZIP. Remove any older
   `companion` prototype from this TEST instance to avoid duplicate mod IDs.
3. Start a new disposable Creative world with cheats enabled.
4. Run `/socrates spawn` once, then **right-click Socrates** to open his interaction menu.
5. Run through `docs/PRISM-TEST-CHECKLIST.md` before adding other mods.

No part of this project installs into or modifies your main Prism instance.
Do not begin by testing against your survival save.

## Right-click interaction menu

Right-click your existing Socrates for a character preview, conversation box,
**Talk / Topics, Follow, Stay, Set home here, Live at home, Tasks / Wood, Inventory / Gear, Unload
items, Appearance**, and an explicit autonomy toggle. Everyday menu controls work
for the nearby owner without operator permissions. The initial spawn command
still requires cheats; routine interactions afterward do not.

Click **Talk**, type normally, then Send or Enter. Replies appear in the menu;
scroll the conversation with the mouse wheel. The optional Python service is
still required for conversation. Movement, home, tasks and appearance work without
it. The menu keeps the world running, refreshes status while nearby, and disables
controls if you move out of range. Escape or Close returns to the world.

**Set home here** means where Socrates is standing. **Live at home** selects his
existing resident routine. **Collect drops** under Tasks / Wood gathers dropped items. **Gather oak logs** is the separate tree-harvesting job.
Stay cancels his task and turns autonomy off.

In **Appearance**, apply a username or `local:filename.png`. **Open skin folder**
opens this instance's `config/socrates/skins`; put the PNG there first. This is
not an operating-system file-picker/import wizard. Restart the client to retry
failed cached lookups or reload a changed PNG. Autonomy remains off by default;
configured cloud planning can incur API charges.

When upgrading a TEST instance, close Minecraft, replace the older Socrates JAR
with 0.4.0-preview.1 (keep only one version), and reopen the disposable test world. Existing
companions retain their saved owner and state.

## Optional command controls

| Command | Effect |
|---|---|
| `/socrates spawn` | Creates your companion in loaded space beside you |
| `/socrates status` | Mode, position, home, task, autonomy and inventory |
| `/socrates follow` | Adventure mode; follows owner within 48 blocks |
| `/socrates stay` | Stops voluntary movement, clears task, disables autonomy |
| `/socrates home` | Sets home at **Socrates's** current position |
| `/socrates mode resident` | Daytime wandering near home; returns closer at night |
| `/socrates mode adventure` | Follow plus basic avoidance of Monster-class mobs |
| `/socrates mode helper` | Waits for a supported task |
| `/socrates mode stay` | Same as Stay |
| `/socrates skin Socrates` | Resolve a Minecraft username's skin asynchronously |
| `/socrates skin "local:test.png"` | Import local standard 64x64 or legacy 64x32 PNG |
| `/socrates collect` | Collect nearby **dropped items**, not mine blocks |
| `/socrates explore` | Wander near home for up to 1,200 loaded game ticks |
| `/socrates unload` | Transfer inventory to you; stand within four blocks |
| `/socrates talk follow me` | Ask the Python planner |
| `/socrates talk remember Jacob likes building` | Store an explicit fact |
| `/socrates talk what do you remember?` | Recall saved facts |
| `/socrates autonomy on` | Opt into periodic planner proposals while idle |
| `/socrates autonomy off` | Disable periodic requests |

These command shortcuts require permission level 2. The right-click owner menu does not.
There is no teleport, dimension following, forced chunk loading, combat, general mining,
general building or Create machinery integration. The bounded oak and equipment adapters
below are the supported exceptions to the original resource/crafting limitations.

## Character and skins

Classic four-pixel arms, vanilla player geometry, held-item and humanoid armor
layers. Entity movement/head animation comes from Minecraft's renderer.
Default username is `Socrates`. Username lookup needs internet access to Mojang;
failures use Steve and log a warning. This does not impersonate or log in as that account.

Put PNG files in your TEST instance's `.minecraft/config/socrates/skins/`.
Use simple filenames, e.g. `test.png`, then quote `"local:test.png"` in the command.
PNG dimensions are checked before decoding; files are limited to 1 MiB.
Skin cache is capped at 64 entries per client session. Restart the client after
replacing a PNG or retrying a failed username lookup. Skins are local-client assets;
there is no PNG synchronization for multiplayer. No slim-arm model switching.

## Optional AI service

Install Python 3.10+; no pip packages are required. From the source project's root:

```powershell
python ai/service.py --instance "C:\YOUR\PRISM\INSTANCE\.minecraft"
```

Use the exact TEST instance Minecraft folder. Keep the terminal running while
playing. The service creates a local random token and SQLite database under
`config/socrates`. Do not share these generated files. Stop with Ctrl+C.

Default is **offline**: supported explicit instructions, memories, and a persistent personal
travel-kit objective. This is authored deterministic planning, not open-ended AI agency.
For Gemini/OpenAI conversation, see `ai/README.md`; configure provider, model and
API key in Python's environment. No credentials belong in source code or Minecraft chat.
Cloud mode sends instructions, a bounded state snapshot and up to eight saved facts to the chosen provider.
Actual provider API calls have not been tested with live credentials.

The mod talks only to `127.0.0.1:8765`, bypasses proxies for that local connection,
rejects redirects, limits response size, and validates a small action allowlist.
The service checks a bearer token, rejects browser Origin headers, limits request
size and concurrent planning. World mutations occur only on Minecraft's server
thread, with ownership, dimension, distance and state-revision checks.
Stay invalidates pending movement plans. The bridge is single-player only.

Autonomy is off by default. When enabled, an idle companion with its owner nearby
requests a personal-goal step every 200 game ticks (about ten seconds at normal tick
rate). Autonomy uses a local authored equipment/wood-reserve policy even when a cloud
conversation provider is selected. Explicit cloud conversations can incur charges.
Resident routines work without Python. Proposed-goal rows remain proposals; oak
outcomes are authoritative in world NBT and mirrored to SQLite on subsequent observed
requests, not through a reliable immediate push/acknowledgement protocol.

## Limits of safety and navigation

Legacy collect/explore tasks require a home within 16 blocks. Collection searches eight blocks around
Socrates and targets dropped items within 12 blocks of home. Resident/exploration
use vanilla restricted wandering and return-home goals; this is not a physical
barrier. Knockback, fleeing and terrain can move him outside the normal radius.
He is not invulnerable, cannot guarantee safe paths and can get stuck.
Stay permits gravity, pushes and emergency floating. Oak harvesting breaks approved trunk blocks; other legacy tasks do not break/place blocks.
A nine-slot inventory persists and drops on ordinary death. `/kill` permits a fresh
spawn after death; unloaded companions must be found rather than duplicated.
Status for an unloaded companion reports its recorded spawn, not live tracking.

## Build and tests

Use a Java 17 **JDK**, not just a runtime. ForgeGradle 6.0.54 and Gradle 8.8 are pinned;
the wrapper distribution has a SHA-256 check. Initial build needs internet.

```powershell
.\gradlew.bat clean build
.\gradlew.bat runGameTestServer -x downloadAssets
cd ai
python -m unittest -v
```

Output: `build/libs/socrates-0.4.0-preview.1.jar`. `build-windows.ps1` wraps the build.
Linux/macOS use `./gradlew` instead of `.\gradlew.bat`.

For the optional Java→Python integration test, start Python in a separate terminal
with `--instance run-gametest` from this project, then run:

```powershell
.\gradlew.bat runGameTestServer -PbridgeTests -x downloadAssets
```

This creates a disposable `run-gametest/world` in the project. Skipping asset downloads
is appropriate for headless tests. Use `runClient` normally for graphical development.
See `docs/TEST-RESULTS-0.4.0-preview.1.md` for current results and `docs/TEST-RESULTS.md` for historical 0.3.0 evidence.

### Conversation topics (0.4.0-preview.1)

Right-click Socrates, then **Talk / Topics**. Choose a topic or type freely below.
Memories reads the existing world/owner/companion-scoped SQLite facts. Remember a
fact fills in `remember `; finish your sentence and press Send. Planning topics
are conversation only; use an action button to start work. Offline mode provides
small original topic replies; open-ended AI conversation needs a configured
Gemini/OpenAI provider. No MCA installation is required. Personality progression,
relationship levels, branching dialogue and village gossip are planned, not added.
The screen transcript currently lasts only while that screen is open.

## New preview: shared inventory, oak harvesting and a personal objective

This is an experimental capability preview. Keep using disposable worlds.

- **Inventory / Gear** opens nine storage slots, hand/offhand and four armor slots,
  plus your inventory. Click, drag or shift-click normally. Giving an axe by
  shift-click equips it; armor shift-click equips an empty matching slot.
- Give him an axe, then **Tasks / Wood → Gather oak logs** (1–64, default 15),
  or type **go get me 15 oak logs** in conversation with the updated Python service.
  The direct task button works without Python. Conversation parsing supports this
  bounded request, not every possible phrasing or resource.
- Harvesting searches loaded terrain within 12 blocks horizontally of its starting
  point. It accepts small straight oak trunks (2–6 blocks), rooted in dirt/grass
  and with nonpersistent oak leaves. This is a tree heuristic, not a guarantee
  against damaging player-built trees. Do not run it near log constructions.
  Large/branched/tall trees, planting and scaffolding are unsupported.
- He walks into reach, checks line of sight, uses vanilla break progress, invokes
  Forge's survival FakePlayer block-breaking path, wears the actual axe, and counts
  fresh oak drops inserted into his inventory. Existing inventory logs do not count.
  He stops at the requested quantity and resumes ordinary following. Retrieve the
  logs through Inventory or Unload; handover is not an automatic delivery transaction.
- Blockers include a missing/broken axe, full inventory, unreachable targets and
  denied block breaks. Stay cancels work. Job ID, quantity, progress, state and
  approved targets persist; a job expires after 12,000 loaded work ticks.
- **Prepare own kit** under Tasks / Wood takes one crafting/equipment step. With
  an accessible crafting table within reach, it can turn oak logs into planks,
  make sticks, a wooden axe and pickaxe, and an iron chestplate from eight iron
  ingots. Installed recipes determine output and remainders; no free materials.
  Armor in storage can be equipped. It does not obtain iron, build/place a table,
  craft every tool/armor type, smelt or operate modded workstations.
- **Autonomy on** gives him the initial personal objective of preparing those
  travel tools and maintaining eight oak logs in reserve. Python persists the
  objective and chooses supported steps from real observations. Player-requested
  completed logs are held for you before personal crafting. This is one authored
  progression, not general self-directed survival or arbitrary personal ambitions.
- Talk / Topics → **Your goals** describes the persisted objective. Basic world
  awareness includes health, inventory/equipment, dimension/position, task/outcome
  and accessible workbench status. It does not provide a full visual world model.

Update **both** the JAR and Python `ai/service.py` for this preview. Preserve the
existing instance's `config/socrates` token/database; restart the Python service.
Player2 research and the integration recommendation are in `docs/PLAYER2-EVALUATION.md`.
Player2 is not installed or required by this preview.
