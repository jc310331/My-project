# Isolated Prism test

Create a NEW Prism instance: Minecraft 1.20.1, Forge 47.4.10, Java 17.
Do not copy your survival save. Start a disposable Creative world with cheats.
Install only the produced Socrates JAR initially. Never install the source ZIP.

1. `/socrates spawn`: confirm Socrates label, normal wide-arm player geometry.
2. `/socrates follow`: walk over flat ground, stairs and around a solid wall.
3. `/socrates stay`: wait one minute. No voluntary wandering; external pushes,
   gravity and emergency floating may still move him.
4. `/socrates home`: stores SOCRATES'S current position, not your position.
5. `/socrates mode resident`: observe bounded wandering and return toward home.
6. `/socrates mode helper`: waits for tasks. Try `/socrates collect` with dropped logs nearby, then `/socrates unload`.
7. `/socrates skin Notch`: wait for skin download. Check logs if Steve fallback remains.
8. Put a 64x64 or 64x32 Minecraft skin in `.minecraft/config/socrates/skins/test.png`.
   Run `/socrates skin "local:test.png"`. Local textures stay client-local.
   Restart client after overwriting the same PNG (cache has no hot reload).
9. `/item replace entity @e[type=companion:socrates,limit=1,sort=nearest] weapon.mainhand with minecraft:iron_pickaxe`
10. `/item replace entity @e[type=companion:socrates,limit=1,sort=nearest] armor.chest with minecraft:iron_chestplate`
11. Check held-item/armor visibility, leg swing and head tracking.
12. Save/quit/reload. Verify name, mode, skin, owner, home and equipment persist.
13. Leave his chunk, try spawn: duplicate creation must be rejected. Return to him.
14. Adventure avoids nearby Monster-class entities; this is not combat or guaranteed safety.
15. Only after baseline passes, add Create 6.0.8, then Embeddium/Oculus, then the rest
    of the modpack to this TEST instance. Record exact versions and any crash logs.

Do not mark visual checks passed merely because the JAR compiled or a server test passed.
