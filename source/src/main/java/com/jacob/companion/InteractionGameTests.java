package com.jacob.companion;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.*;
import com.jacob.companion.InteractionActions.Action;

@GameTestHolder("companion")
@PrefixGameTestTemplate(false)
public class InteractionGameTests {
    private static Player owner(GameTestHelper helper) {
        var player=helper.makeMockSurvivalPlayer();player.setUUID(UUID.randomUUID());
        var pos=helper.absolutePos(new BlockPos(2,1,2));player.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);
        return player;
    }
    private static SocratesEntity companion(GameTestHelper helper,Player player) {
        var s=helper.spawn(CompanionMod.SOCRATES.get(),new BlockPos(3,1,2));
        s.setOwner(player.getUUID());s.setHome(s.blockPosition());s.setMode(SocratesEntity.Mode.STAY);return s;
    }
    @GameTest(template="floor",timeoutTicks=20)
    public static void ownerMenuControlsWithoutCreative(GameTestHelper h) {
        var player=owner(h);var s=companion(h,player);
        h.assertTrue(!player.isCreative(),"Fixture should be a survival owner");
        h.assertTrue(!player.hasPermissions(2),"Fixture should not have operator permission");
        h.assertTrue(InteractionActions.apply(player,s,Action.FOLLOW,"").accepted()&&s.getMode()==SocratesEntity.Mode.ADVENTURE,"Follow failed");
        h.assertTrue(InteractionActions.apply(player,s,Action.HOME,"").accepted()&&s.getHome().equals(s.blockPosition()),"Home failed");
        h.assertTrue(InteractionActions.apply(player,s,Action.COLLECT,"").accepted()&&s.getTask().equals("collect"),"Collect failed");
        InteractionActions.apply(player,s,Action.AUTONOMY_ON,"");int revision=s.getPlanRevision();
        h.assertTrue(InteractionActions.apply(player,s,Action.STAY,"").accepted(),"Stay rejected");
        h.assertTrue(s.getMode()==SocratesEntity.Mode.STAY&&!s.isAutonomy()&&s.getTask().isEmpty()&&s.getPlanRevision()>revision,"Stay must invalidate pending plans and stop autonomy");
        revision=s.getPlanRevision();
        h.assertTrue(InteractionActions.apply(player,s,Action.REFRESH,"").accepted()&&s.getPlanRevision()==revision,"Status refresh must not change or cancel plans");
        h.succeed();
    }
    @GameTest(template="floor",timeoutTicks=20)
    public static void menuRejectsWrongOwnerDistanceAndDimension(GameTestHelper h) {
        var player=owner(h);var s=companion(h,player);var stranger=owner(h);
        h.assertTrue(!InteractionActions.apply(stranger,s,Action.FOLLOW,"").accepted(),"Another owner controlled companion");
        player.setPos(s.getX()+7,s.getY(),s.getZ());
        h.assertTrue(!InteractionActions.apply(player,s,Action.FOLLOW,"").accepted(),"Remote control accepted");
        player.setPos(s.getX()+1,s.getY(),s.getZ());
        var other=CompanionMod.SOCRATES.get().create(h.getLevel().getServer().getLevel(Level.NETHER));
        other.setOwner(player.getUUID());other.setPos(player.position());other.setMode(SocratesEntity.Mode.STAY);
        h.assertTrue(!InteractionActions.apply(player,other,Action.FOLLOW,"").accepted(),"Cross-dimension control accepted");
        h.assertTrue(!InteractionActions.apply(player,s,Action.FOLLOW,"op @a").accepted(),"Extra payload accepted");
        h.assertTrue(s.getMode()==SocratesEntity.Mode.STAY,"Rejected requests mutated state");h.succeed();
    }
    @GameTest(template="floor",timeoutTicks=20)
    public static void menuUnloadIsNearAndDoesNotDuplicate(GameTestHelper h) {
        var player=owner(h);var s=companion(h,player);s.getInventory().addItem(new ItemStack(Items.OAK_LOG,3));
        player.setPos(s.getX()+5,s.getY(),s.getZ());
        h.assertTrue(!InteractionActions.apply(player,s,Action.UNLOAD,"").accepted()&&s.getInventory().countItem(Items.OAK_LOG)==3,"Distant unload drained items");
        player.setPos(s.getX()+1,s.getY(),s.getZ());
        h.assertTrue(InteractionActions.apply(player,s,Action.UNLOAD,"").accepted(),"Nearby unload rejected");
        InteractionActions.apply(player,s,Action.UNLOAD,"");
        h.assertTrue(player.getInventory().countItem(Items.OAK_LOG)==3&&s.getInventory().countItem(Items.OAK_LOG)==0,"Unload lost or duplicated items");h.succeed();
    }
    @GameTest(template="floor",timeoutTicks=20)
    public static void menuBoundsConversationAndSkin(GameTestHelper h) {
        var player=owner(h);var s=companion(h,player);
        h.assertTrue(!InteractionActions.apply(player,s,Action.TALK," ").accepted(),"Blank conversation accepted");
        h.assertTrue(!InteractionActions.apply(player,s,Action.TALK,"a".repeat(1001)).accepted(),"Unbounded conversation accepted");
        h.assertTrue(InteractionActions.apply(player,s,Action.TALK,"follow me").accepted()&&s.getMode()==SocratesEntity.Mode.STAY,"Talk executed an action before planner validation");
        h.assertTrue(!InteractionActions.apply(player,s,Action.SKIN,"local:../private.png").accepted(),"Skin path traversal accepted");
        h.assertTrue(InteractionActions.apply(player,s,Action.SKIN,"local:test.png").accepted()&&s.getSkinUsername().equals("local:test.png"),"Local skin request rejected");
        h.succeed();
    }
}
