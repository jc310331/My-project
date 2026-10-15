# Player2 / Player2NPC evaluation — October 15, 2026

Status: source inspection, not an installed-mod or live-service test. The user is
open to Player2 depending on quality and pricing; no provider commitment or paid
usage is authorized by that preference alone. Current Socrates source is retained.

## Findings

Player2NPC is directly relevant to the requested companion. Its public README
explicitly describes natural-language resource requests ("get me 10 wood"), a
player-like inventory, block interactions, following and combat. It is backed by
PlayerEngine, combining Automatone/Baritone-style navigation with AltoClef-derived
resource/task systems. This is much broader than Socrates 0.3.2's actuator layer.

The default repository branches are older Fabric examples. Inspecting branches
revealed a much more relevant Forge path:

| Repository / branch | Inspected commit | Declared target |
| --- | --- | --- |
| Goodbird-git/Player2NPC, master | ec439c11b4fd31ebd3b1b698f28f632eec4081fc | Fabric, MC 1.20.1 |
| Goodbird-git/Player2NPC, arch-1.20.1 | 32f6645ff42f3b556f779155eff454ac38abe35c | Fabric + Forge; Forge 47.4.9; mod version 1.20.1-1.1.7 |
| Goodbird-git/PlayerEngine, 1.20.1-arch | 4a885a4f19c1ccce37a0004771965fd4b098a484 | Fabric + Forge; Forge 47.4.6; version 1.20.1-1.1.11 |
| elefant-ai/player2-sdk-ts, default | 4db6c339bd0ffca387b784970a14f25590f4589e | Player2 API types/client, not a Minecraft action engine |

NPC Forge metadata requires Architectury >=9.2.14 and PlayerEngine
>=1.20.1-1.1.11. MC 1.20.1 / Forge 47 is a plausible match for the user's Forge
47.4.10 pack; compatibility with that patch level and Create/Embeddium/Oculus has
NOT been established. The code uses substantial entity/world mixins, so ordinary
loader-version compatibility is insufficient evidence.

PlayerEngine's newer branch contains resource mining/collection, inventory and
table crafting, food/survival chains, and structure-building classes. This is
actual source evidence of systems, not a runtime success claim or proof that its
NPC pursues meaningful independent long-term goals. Its main README says complex
building is unsupported; branch code is newer than that summary.

Important building distinction: `BuildStructureCommand` explicitly says materials
need not be collected. `BuildStructureTask` contains direct `world.setBlock`
paths for schematic/generated construction. That behavior needs a separate
survival-rules review before it fits a companion who earns and consumes supplies.
Do not infer that its mining/crafting systems also create free materials merely
from this building-specific finding.

## Service and pricing

The inspected PlayerEngine authentication code supports local Player2 login at
127.0.0.1:4315 and web device authorization. Its request helper sends inference
requests to https://api.player2.game using an authorization token and game key.
Local login does not establish offline/local inference. The official SDK also
exposes chat, NPC, text-to-speech and speech-to-text APIs.

Both the SDK and engine explicitly handle HTTP 402 / insufficient AI Power or
credits and direct users to top up. Current free allowances, subscription prices,
credit costs, reset schedules, regional prices and refund rules are UNVERIFIED.
No pricing figure should be quoted based on these sources. No account was logged
in, token supplied, request billed or voice generated in this review.

The user-provided player2.game and CurseForge pages returned proxy tunnel 403
responses under the running cloud policy, including a retry after the relevant
hostnames were added to the configuration draft. Draft saving is not live policy
activation. Added domains: player2.game, www.player2.game, www.curseforge.com.
They remain pending environment review/save/publication. GitHub repositories were
accessible and provided the findings above. No fabricated page review is claimed.

## License and reproducibility

PlayerEngine includes an LGPLv3 license and its Forge metadata declares LGPLv3.
Integration or redistribution must preserve relevant license/source obligations.
Player2NPC metadata is inconsistent across inspected branches: the old Fabric
metadata says LGPL-3.0-or-later, the old Forge branch says CC BY-NC, and the
Architectury Forge metadata still says "Insert License Here". Resolve the intended
license before copying or redistributing that NPC branch. API SDK is MIT.

The Architectury NPC build references a local file
`../libs/playerengine-forge-1.0.54.jar` which is not present in that branch's tree;
its runtime metadata instead asks for PlayerEngine >=1.20.1-1.1.11. This needs a
matched, reproducible artifact/dependency setup before a build or install claim.
GitHub releases pages inspected did not expose downloadable release assets.
CurseForge file versions/dependencies could not be confirmed because access was
blocked. Do not install the older committed JAR and call it a test of the newer
Architectury stack. No external binaries were installed or executed in this review.

## Recommendation

Run a separate Forge 1.20.1 Player2NPC/PlayerEngine trial before expanding
Socrates into broad mining, crafting, survival and building. The existing engine
could save substantial work if its behavior and licensing fit. Preserve current
Socrates and its saves; an integration would need a tested inventory/entity-state
migration and must not run two competing navigation controllers.

Three distinct options:

1. Test the existing NPC as a gameplay benchmark. Lowest initial customization,
   quickest way to judge whether its embodied behavior meets the user's goal.
2. Integrate PlayerEngine as Socrates's action/navigation backend while retaining
   the Forge entity identity, owner UI and Python planning/SQLite architecture.
   Promising, but an engineering integration, not a drop-in reskin.
3. Add Player2 as an optional Python conversation provider. Smaller scope, but
   provider replacement alone does not add mining, crafting or autonomy.

Keep provider selection optional until measured quality and pricing are known.
The current self-contained Socrates preview remains useful as a tested baseline
and fallback rather than a reason to duplicate the larger engine blindly.

## Trial acceptance checklist

Use a fresh test instance and disposable world, never the main pack/save.

- Give an axe through inventory; ask for exactly 15 oak logs. Check physical logs
  broken, actual pickups, durability, exact quantity, return and handover.
- Start without a tool; see whether it acquires prerequisites and crafts from real
  materials. Check which independent goals it chooses while idle.
- Give iron and a workbench; verify armor consumes ingredients and is equipped.
- Interrupt work; reload the save; test full inventory, broken tool and blocked
  paths. Check that cancelled tasks do not resume unexpectedly.
- Compare text quality, memory continuity, initiative and chatter with Socrates.
- Test building separately for survival material consumption and placement rules.
- Add Create 6.0.8, Embeddium and Oculus incrementally; inspect crashes/rendering.
- Record account credits before/after a fixed-duration text session; measure voice
  separately. Combine observed usage with verified current unit prices and limits.
  Do not extrapolate a precise hourly cost from undocumented assumptions.

## Sources

- User URLs: https://player2.game/ and
  https://www.curseforge.com/minecraft/mc-mods/player2npc (access blocked here).
- https://github.com/Goodbird-git/Player2NPC/tree/arch-1.20.1
- https://github.com/Goodbird-git/PlayerEngine/tree/1.20.1-arch
- https://github.com/Goodbird-git/PlayerEngine/blob/1.20.1-arch/common/src/main/java/com/player2/playerengine/commands/BuildStructureCommand.java
- https://github.com/Goodbird-git/PlayerEngine/blob/1.20.1-arch/common/src/main/java/com/player2/playerengine/tasks/construction/build_structure/BuildStructureTask.java
- https://github.com/elefant-ai/player2-sdk-ts
