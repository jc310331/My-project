package com.jacob.companion;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraftforge.gametest.*;

@GameTestHolder("companion") @PrefixGameTestTemplate(false)
public class WorkGameTests {
    private static SocratesEntity worker(GameTestHelper h) {
        for(int x=0;x<9;x++)for(int z=0;z<9;z++) {
            h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
            for(int y=1;y<8;y++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        }
        var s=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(4,1,4));
        s.setOwner(UUID.randomUUID());s.setHome(s.blockPosition());return s;
    }
    private static void tree(GameTestHelper h,int x,int z) {
        h.setBlock(new BlockPos(x,0,z),Blocks.DIRT);
        for(int y=1;y<=3;y++)h.setBlock(new BlockPos(x,y,z),Blocks.OAK_LOG);
        h.setBlock(new BlockPos(x,4,z),Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,false).setValue(LeavesBlock.DISTANCE,1));
    }
    @GameTest(template="floor",timeoutTicks=30)
    public static void inventoryShiftClickAndAccess(GameTestHelper h) {
        var s=worker(h);var p=h.makeMockSurvivalPlayer();p.setUUID(s.getOwner());p.setPos(s.position());
        var menu=new CompanionInventoryMenu(1,p.getInventory(),s);
        p.getInventory().setItem(9,new ItemStack(Items.IRON_AXE));p.getInventory().setItem(10,new ItemStack(Items.OAK_LOG,15));
        menu.quickMoveStack(p,15);menu.quickMoveStack(p,16);
        h.assertTrue(s.getMainHandItem().is(Items.IRON_AXE)&&s.getInventory().countItem(Items.OAK_LOG)==15,"Giving axe/logs failed");
        menu.quickMoveStack(p,0);menu.quickMoveStack(p,9);
        h.assertTrue(s.getMainHandItem().isEmpty()&&s.getInventory().isEmpty()&&p.getInventory().countItem(Items.OAK_LOG)==15&&p.getInventory().countItem(Items.IRON_AXE)==1,"Take back lost/duplicated items");
        p.getInventory().setItem(10,new ItemStack(Items.IRON_CHESTPLATE));menu.quickMoveStack(p,16);
        h.assertTrue(s.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),"Armor shift equip failed");
        p.setPos(s.getX()+7,s.getY(),s.getZ());
        h.assertTrue(!menu.stillValid(p)&&menu.quickMoveStack(p,12).isEmpty(),"Out of range menu allowed transfer");
        p.setPos(s.position());p.setUUID(UUID.randomUUID());h.assertTrue(!menu.stillValid(p),"Wrong owner retained inventory access");h.succeed();
    }
    @GameTest(template="floor",timeoutTicks=30)
    public static void craftTravelToolsAndArmorFromRealMaterials(GameTestHelper h) {
        var s=worker(h);h.setBlock(new BlockPos(4,1,6),Blocks.CRAFTING_TABLE);
        s.getInventory().addItem(new ItemStack(Items.OAK_LOG,5));
        for(int i=0;i<8;i++)KitCrafting.step(s);
        h.assertTrue(KitCrafting.has(s,x->x.is(Items.WOODEN_AXE))&&KitCrafting.has(s,x->x.is(Items.WOODEN_PICKAXE)),"Tools not crafted");
        h.assertTrue(s.getInventory().countItem(Items.OAK_LOG)==3,"Crafting consumed the wrong number of logs");
        s.getInventory().addItem(new ItemStack(Items.IRON_INGOT,8));KitCrafting.step(s);KitCrafting.step(s);
        h.assertTrue(s.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE)&&s.getInventory().countItem(Items.IRON_INGOT)==0,"Iron armor not crafted/equipped exactly");h.succeed();
    }
    @GameTest(template="floor",timeoutTicks=30)
    public static void craftingNeedsWorkbenchAndDoesNotInventIngredients(GameTestHelper h) {
        var s=worker(h);s.getInventory().addItem(new ItemStack(Items.OAK_LOG,2));KitCrafting.step(s);
        h.assertTrue(s.getInventory().countItem(Items.OAK_LOG)==2&&!KitCrafting.has(s,x->x.is(Items.WOODEN_AXE)),"Crafted remotely without table");
        h.setBlock(new BlockPos(4,1,6),Blocks.CRAFTING_TABLE);s.getInventory().clearContent();KitCrafting.step(s);
        h.assertTrue(s.getInventory().isEmpty(),"Invented recipe ingredients");h.succeed();
    }
    @GameTest(template="floor",timeoutTicks=1000)
    public static void harvestExactlyFifteenAndWearTool(GameTestHelper h) {
        var s=worker(h);for(int[] p:new int[][]{{1,1},{4,1},{7,1},{1,7},{7,7},{4,7}})tree(h,p[0],p[1]);
        s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
        h.assertTrue(s.startOakJob(15),"Job rejected");
        h.succeedWhen(()->{
            h.assertTrue(s.oakJob().state().equals("completed"),s.oakJob().summary()+" pos="+s.position());
            h.assertTrue(s.getInventory().countItem(Items.OAK_LOG)==15&&s.oakJob().obtained()==15,"Wrong exact log quantity");
            h.assertTrue(s.getMainHandItem().getDamageValue()==15,"Axe durability not consumed once per log: "+s.getMainHandItem().getDamageValue());
            h.assertTrue(s.getTask().isEmpty(),"Completed job still running");
            h.assertTrue(s.oakJob().reservedForOwner(),"Requested logs were not reserved");
            s.getInventory().clearContent();s.oakJob().observeInventory();s.getInventory().addItem(new ItemStack(Items.OAK_LOG,2));
            h.assertTrue(!s.oakJob().reservedForOwner(),"Handed-over job reserved newly given personal materials");
            var saved=new CompoundTag();s.save(saved);var copy=CompanionMod.SOCRATES.get().create(h.getLevel());copy.load(saved);
            h.assertTrue(!copy.oakJob().reservedForOwner(),"Delivery marker lost on reload");
            int logs=0;for(int x=0;x<9;x++)for(int z=0;z<9;z++)for(int y=1;y<=3;y++)if(h.getLevel().getBlockState(h.absolutePos(new BlockPos(x,y,z))).is(Blocks.OAK_LOG))logs++;
            h.assertTrue(logs==3,"Job did not stop at 15 breaks, remaining="+logs);
        });
    }
    @GameTest(template="floor",timeoutTicks=80)
    public static void harvestNeedsAxeAndStayCancels(GameTestHelper h) {
        var s=worker(h);tree(h,5,4);s.startOakJob(15);
        h.runAfterDelay(25,()->{
            h.assertTrue(s.oakJob().state().equals("blocked")&&s.oakJob().obtained()==0,"Missing axe did not block");
            var tag=new CompoundTag();s.save(tag);var copy=CompanionMod.SOCRATES.get().create(h.getLevel());copy.load(tag);
            h.assertTrue(copy.oakJob().id().equals(s.oakJob().id())&&copy.oakJob().state().equals("blocked")&&copy.getTask().equals("oak_logs"),"Blocked job persistence lost");
            s.setMode(SocratesEntity.Mode.STAY);s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
        });
        h.runAfterDelay(60,()->{h.assertTrue(s.oakJob().state().equals("cancelled")&&s.getInventory().isEmpty(),"Stay failed to cancel harvesting");h.assertBlockPresent(Blocks.OAK_LOG,new BlockPos(5,1,4));h.succeed();});
    }
    @GameTest(template="floor",timeoutTicks=120)
    public static void harvestHonorsBreakCancellation(GameTestHelper h) {
        var s=worker(h);tree(h,5,4);s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> deny=e->{if(e.getPlayer().getUUID().equals(s.getUUID()))e.setCanceled(true);};
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(deny);
        s.startOakJob(1);
        h.runAfterDelay(100,()->{
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(deny);
            h.assertTrue(s.getInventory().isEmpty()&&s.oakJob().obtained()==0,"Denied break produced items");
            h.assertBlockPresent(Blocks.OAK_LOG,new BlockPos(5,1,4));h.assertTrue(s.getMainHandItem().getDamageValue()==0,"Denied break damaged axe");h.succeed();
        });
    }
    @GameTest(template="floor",timeoutTicks=120)
    public static void brokenAxeBlocksAndFullInventoryDoesNotBreak(GameTestHelper h) {
        var s=worker(h);tree(h,5,4);var axe=new ItemStack(Items.IRON_AXE);axe.setDamageValue(axe.getMaxDamage()-1);s.setItemSlot(EquipmentSlot.MAINHAND,axe);s.startOakJob(3);
        h.runAfterDelay(80,()->{
            h.assertTrue(s.getInventory().countItem(Items.OAK_LOG)==1&&s.getMainHandItem().isEmpty(),"Axe did not break on first log");
            h.assertTrue(s.oakJob().state().equals("blocked"),"Broken axe did not block job");
            s.setMode(SocratesEntity.Mode.STAY);s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_AXE));
            for(int i=0;i<9;i++)s.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));s.startOakJob(1);
        });
        h.runAfterDelay(110,()->{h.assertTrue(s.oakJob().obtained()==0&&s.getMainHandItem().getDamageValue()==0&&s.oakJob().state().equals("blocked"),"Full inventory destroyed wood");h.succeed();});
    }
}
