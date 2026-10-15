# Project Socrates

Continue the supplied 0.3.0 implementation under `source/`; do not restart from
the old 0.2 prototype. Read `PROGRESS.md`, `START-HERE.md`, `source/README.md`,
`source/docs/ARCHITECTURE.md`, and `source/docs/TEST-RESULTS.md` before changes.
The original handoff is preserved in Git commit 824bd45, including its JAR and logs.

Target: single-player Minecraft Java 1.20.1, Forge 47.4.10, Java 17, Windows Prism.
Classic four-pixel arms; default skin username Socrates; username and PNG support.
Create 6.0.8, Embeddium, and Oculus compatibility needs separate verification.

Forge owns entities, navigation, inventory, and validated world mutations.
Python owns planning, conversation, and SQLite memory. Keep communication local
and authenticated. Never expose the planner port or commit credentials, local
bridge configuration, memory databases, generated worlds, or personal saves.
Never touch the user's main modpack or survival world. Use disposable instances.

Use the existing checkout in this isolated cloud task. Do not create worktrees
unless requested. Preserve original evidence; put new results in dated directories.
Keep implemented, compiled, engine-tested, visually verified, and planned features
distinct. GameTests of development classes do not verify an installed production JAR.
Never infer completion of a world action from a planner proposal or elapsed time.

Priorities: reproduce build/tests; graphical production-JAR smoke test; fix defects;
then observation, acknowledged persistent goals, navigation recovery, and bounded
resource actions. Mining/crafting/building/combat/Create interaction require
separately tested adapters. Do not expand autonomy to hide failed required checks.

Keep `PROGRESS.md` current and deliver complete source, versioned artifacts, setup
instructions, and honest evidence. Retain code and history in portable checkpoints.
Do not promise background work between sessions without an actual persistent runner.
Ask only for genuine missing decisions/access after useful independent work.
