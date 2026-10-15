# Project Socrates 0.3.0 — experimental test release

Minecraft Java **1.20.1**, Forge **47.4.10**, Java **17**.
This is a real compiled Forge mod with a separate optional Python planner.
It is an early companion, not yet a general-purpose AI Minecraft player.

## Install safely in Prism on Windows

1. Create a **new** Prism instance named `Socrates Test`: Minecraft 1.20.1,
   Forge 47.4.10. Select a **Java 17** installation in instance settings.
2. Open that instance's Minecraft folder. Put `socrates-0.3.0.jar` in `mods`.
   Install only the normal JAR, not the source JAR or ZIP. Remove any older
   `companion` prototype from this TEST instance to avoid duplicate mod IDs.
3. Start a new disposable Creative world with cheats enabled.
4. Run `/socrates spawn`, then `/socrates follow`.
5. Run through `docs/PRISM-TEST-CHECKLIST.md` before adding other mods.

No part of this project installs into or modifies your main Prism instance.
Do not begin by testing against your survival save.

## Controls

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

All commands require permission level 2 (enable cheats in the test world).
There is no teleport, dimension following, forced chunk loading, combat, mining,
crafting, building, inventory GUI or Create machinery integration in this version.

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

Default is **offline**: simple explicit instructions, memories, and alternating
collect/explore proposals. This is deterministic logic, not an LLM.
For Gemini/OpenAI conversation, see `ai/README.md`; configure provider, model and
API key in Python's environment. No credentials belong in source code or Minecraft chat.
Cloud mode sends instructions and up to eight saved facts to the chosen provider.
Actual provider API calls have not been tested with live credentials.

The mod talks only to `127.0.0.1:8765`, bypasses proxies for that local connection,
rejects redirects, limits response size, and validates a small action allowlist.
The service checks a bearer token, rejects browser Origin headers, limits request
size and concurrent planning. World mutations occur only on Minecraft's server
thread, with ownership, dimension, distance and state-revision checks.
Stay invalidates pending movement plans. The bridge is single-player only.

Autonomy is off by default. When enabled, an idle companion with its owner nearby
requests a plan every 1,200 game ticks (about a minute at normal tick rate).
Configured cloud mode can incur API charges. Resident routines work without Python.
The SQLite goal ledger stores **proposed** goals; it does not claim task completion.
Active task state and remaining ticks are saved in the entity's world NBT.

## Limits of safety and navigation

Tasks require a home within 16 blocks. Collection searches eight blocks around
Socrates and targets dropped items within 12 blocks of home. Resident/exploration
use vanilla restricted wandering and return-home goals; this is not a physical
barrier. Knockback, fleeing and terrain can move him outside the normal radius.
He is not invulnerable, cannot guarantee safe paths and can get stuck.
Stay permits gravity, pushes and emergency floating. No task breaks or places blocks.
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

Output: `build/libs/socrates-0.3.0.jar`. `build-windows.ps1` wraps the build.
Linux/macOS use `./gradlew` instead of `.\gradlew.bat`.

For the optional Java→Python integration test, start Python in a separate terminal
with `--instance run-gametest` from this project, then run:

```powershell
.\gradlew.bat runGameTestServer -PbridgeTests -x downloadAssets
```

This creates a disposable `run-gametest/world` in the project. Skipping asset downloads
is appropriate for headless tests. Use `runClient` normally for graphical development.
See `docs/TEST-RESULTS.md` for what was actually verified and what remains untested.
