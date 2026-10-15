# Socrates 0.4.0-preview.1 — October 15, 2026

Experimental capability preview, continuing the supplied source. No user PC,
main modpack or survival world was accessed. Forge 47.4.10 / MC 1.20.1,
Temurin Java 17.0.16+8, Gradle 8.8, isolated Linux Xorg/Mesa production client.

## Build and automated evidence

`bash dev/gradle-cloud.sh build runGameTestServer -PbridgeTests -x downloadAssets`:
**20 required actual Forge engine tests passed**, build/reobfuscation succeeded.
Final log: `evidence/0.4.0/engine-release.log`.
`python3 -m unittest discover -s source/ai -v`: **23 passed**;
`evidence/0.4.0/python-final.log`.

Seven added engine tests cover inventory shift transfers, equipment, owner/range
checks; exact 15 actual harvested logs and 15 axe durability; owner handover and
NBT persistence; blocked missing axe and cancellation/save; Forge BreakEvent
cancellation; axe breaking/full inventory; recipe-validated wooden tools and iron
chestplate from real inputs; missing workbench/ingredients. These augment the
previous 13 tests, including the real authenticated Python bridge. That bridge
now also tests bounded quantity parsing and observation-driven personal actions.

Python covers bounded requests/actions, persisted personal goal/phase, no autonomy
without observations, equipment/readiness decisions, idempotent result snapshots,
reservation of player-requested logs, malformed snapshots and low-health pausing.
Existing authentication, isolation, provider validation and redirect tests pass.
The old alternating-autonomy test was replaced because that behavior was deliberately
removed, not because it was disabled to hide a failure. The old ability text check
was updated to the new supported scope. Earlier failures remain in evidence:
first engine run found an equipment-slot index shadowing bug; fixed before final
20-test pass. Earlier Python failures reflected those two superseded requirements.

## Production JAR

Final JAR SHA-256: `639d7c509d292772328a6c345b428a07e2a421662c65a23cd5360751ab815946`.
Final installed client hash matches this artifact. Final client log:
`evidence/0.4.0/production-release.log`; earlier preview session:
`evidence/0.4.0/production-preview.log`.

The earlier preview JAR was graphically exercised for real owner inventory access,
manual storage transfer, axe handover/equipment swapping, and typing "go get me 15
oak logs" through the menu/Python bridge. Five prepared small oak trees supplied
exactly 15 logs. The production log reports actual completion, and the menu shows
Items: 15; the entity resumes ordinary following. This was before the final owner
handover-marker refinement and expanded bridge-test class. It is not described
as a byte-identical final-artifact graphical run.

The final artifact was installed only after save/quit, then the same disposable
world reopened. Inventory, axe durability, armor and completed job persisted.
Standard manual transfer retrieved the requested logs, then gave two new logs for
personal crafting; the new handover marker permits these to be used personally.
With autonomy enabled and no per-step commands, the final artifact converted the
two supplied logs into planks and sticks, crafted a wooden pickaxe, gathered wood
through two personal oak jobs (2 then 6), and reached its eight-log reserve.
Its final inventory visibly held eight oak logs, three planks, two sticks and the
wooden pickaxe, alongside its previously given axe and armor. The menu's Your
goals response read the persisted phase; the service later reported travel kit
and reserve ready. Autonomy was switched off, and the world saved before shutdown.
Iron chestplate crafting and wooden axe crafting were engine-tested, not repeated
in this final graphical session. The personal-goal loop is an authored progression,
not evidence of emergent general agency.

A failed GUI automation sequence clicked outside the intended screen after the
companion moved, broke test-platform blocks in Creative, and left the camera under
water. It was corrected by restoring the disposable platform/positions and
inspecting the screen before further input. That sequence is not a passed mod
interaction test or a user-world incident.

## Limitations and external evaluation

Oak gathering is a bounded small-straight-tree adapter, not general forestry,
mining or terrain modification. Tree classification is heuristic and can match
constructed trees. Use disposable worlds, away from log builds. No scaffolding,
replanting, arbitrary resources, general survival, combat or Create machinery.
Job outcomes are stored in NBT; later observation requests mirror them to SQLite.
A reliable immediate acknowledgement/outbox protocol is not implemented.

Kit crafting supports oak planks/sticks, wooden axe/pickaxe and iron chestplate
with an accessible nearby workbench and actual ingredients. It does not mine
iron, smelt, obtain/place a workbench or implement arbitrary recipes. The starter
personal objective is an authored policy, not emergent open-ended agency. It
works while Minecraft is running and chunks are loaded, not between sessions.

Live Gemini/OpenAI/Player2 calls remain untested. Windows Prism, Create 6.0.8,
Embeddium and Oculus compatibility remain unverified for this preview. Full
animation/skin regressions were not repeated; prior reports remain their evidence.
Player2NPC/PlayerEngine were inspected as source references only; no external code,
assets, binaries or runtime dependency were added. See PLAYER2-EVALUATION.md for
Forge branches, licensing/dependency issues and pricing/runtime unknowns.
