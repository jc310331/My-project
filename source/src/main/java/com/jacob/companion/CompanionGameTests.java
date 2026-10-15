package com.jacob.companion;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("companion")
@PrefixGameTestTemplate(false)
public class CompanionGameTests {
    private static void floor(GameTestHelper h) {
        for(int x=0;x<9;x++)for(int z=0;z<9;z++) {
            h.setBlock(new BlockPos(x,0,z),net.minecraft.world.level.block.Blocks.STONE);
            for(int y=1;y<4;y++)h.setBlock(new BlockPos(x,y,z),net.minecraft.world.level.block.Blocks.AIR);
        }
    }
    @GameTest(template="floor",timeoutTicks=20)
    public static void persistence(GameTestHelper h) { floor(h);
        var s=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(2,1,2));
        UUID owner=UUID.randomUUID();s.setOwner(owner);s.setHome(s.blockPosition());s.setMode(SocratesEntity.Mode.STAY);s.setSkinUsername("local:test.png");
        s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_PICKAXE));s.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.IRON_CHESTPLATE));
        CompoundTag tag=new CompoundTag();s.save(tag);
        var restored=CompanionMod.SOCRATES.get().create(h.getLevel());restored.load(tag);
        h.assertTrue(owner.equals(restored.getOwner()),"Owner lost");
        h.assertTrue(restored.getMode()==SocratesEntity.Mode.STAY,"Mode lost");
        h.assertTrue(s.getHome().equals(restored.getHome()),"Home lost");
        h.assertTrue("local:test.png".equals(restored.getSkinUsername()),"Skin lost");
        h.assertTrue(restored.getMainHandItem().is(Items.IRON_PICKAXE),"Held item lost");
        h.assertTrue(restored.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),"Armor lost");h.succeed();
    }
    @GameTest(template="floor",timeoutTicks=100)
    public static void stay(GameTestHelper h) { floor(h);
        var s=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(2,1,2));s.setMode(SocratesEntity.Mode.STAY);
        var start=s.position();
        h.runAfterDelay(80,()->{h.assertTrue(s.position().horizontalDistanceSqr()>0,"Entity position invalid");h.assertTrue(s.position().subtract(start).horizontalDistanceSqr()<0.04,"Stay wandered");h.succeed();});
    }
    @GameTest(template="floor",timeoutTicks=100)
    public static void navigation(GameTestHelper h) { floor(h);
        var s=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(1,1,1));s.setMode(SocratesEntity.Mode.ADVENTURE);var start=s.position();var end=h.absolutePos(new BlockPos(6,1,1));
        h.runAfterDelay(20,()->{h.assertTrue(s.getNavigation().moveTo(end.getX()+0.5,end.getY(),end.getZ()+0.5,1),"Path not found; pos="+s.position()+" end="+end+" ground="+s.onGround()+" below="+h.getLevel().getBlockState(s.blockPosition().below()));});
        h.runAfterDelay(60,()->{h.assertTrue(s.position().subtract(start).horizontalDistanceSqr()>1,"Entity did not walk; start="+start+" now="+s.position()+" noAI="+s.isNoAi()+" ticks="+s.tickCount+" done="+s.getNavigation().isDone());h.succeed();});
    }
    @GameTest(template="floor",timeoutTicks=120)
    public static void collect(GameTestHelper h) {
        floor(h);h.getLevel().setDayTime(1000);
        var s=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(2,1,2));s.setHome(s.blockPosition());
        h.assertTrue(s.setTask("collect"),"Task rejected");
        var pos=h.absolutePos(new BlockPos(5,1,2));
        var item=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,new ItemStack(Items.OAK_LOG,3));
        item.setNoPickUpDelay();h.getLevel().addFreshEntity(item);
        h.runAfterDelay(100,()->{h.assertTrue(s.getInventory().countItem(Items.OAK_LOG)==3,"Did not gather 3 logs; pos="+s.position()+" mode="+s.getMode()+" task="+s.getTask()+" ticks="+s.tickCount+" navDone="+s.getNavigation().isDone()+" item="+item.position()+" alive="+item.isAlive()+" delay="+item.hasPickUpDelay()+" count="+s.getInventory().countItem(Items.OAK_LOG)+" monsters="+h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,s.getBoundingBox().inflate(8)).size());h.assertTrue(!item.isAlive(),"Picked item duplicated");h.succeed();});
    }
    @GameTest(template="floor",timeoutTicks=20)
    public static void taskPersistenceAndStop(GameTestHelper h) {
        floor(h);var s=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(2,1,2));s.setHome(s.blockPosition());
        s.setTask("explore");s.setAutonomy(true);s.getInventory().addItem(new ItemStack(Items.OAK_LOG,7));
        var tag=new CompoundTag();s.save(tag);var copy=CompanionMod.SOCRATES.get().create(h.getLevel());copy.load(tag);
        h.assertTrue(copy.getTask().equals("explore"),"Goal lost");h.assertTrue(copy.isAutonomy(),"Autonomy setting lost");
        h.assertTrue(copy.getInventory().countItem(Items.OAK_LOG)==7,"Inventory lost");int revision=copy.getPlanRevision();
        copy.setMode(SocratesEntity.Mode.STAY);h.assertTrue(copy.getTask().isEmpty()&&!copy.isAutonomy()&&copy.getPlanRevision()>revision,"Stay did not cancel plans");h.succeed();
    }
    @GameTest(template="floor",timeoutTicks=120)
    public static void collectOnBothEntityTickPhases(GameTestHelper h) {
        floor(h);h.getLevel().setDayTime(1000);
        UUID batch=UUID.randomUUID();
        // Consecutive IDs force both branches of Mob's alternating goal scheduler.
        var first=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(2,1,2));
        var second=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(6,1,2));
        h.assertTrue((first.getId()&1)!=(second.getId()&1),"Fixture needs opposite entity ID phases");
        for(var companion:new SocratesEntity[]{first,second}) {
            companion.setHome(companion.blockPosition());
            h.assertTrue(companion.setTask("collect"),"Task rejected");
        }
        var a=h.absolutePos(new BlockPos(2,1,6));var b=h.absolutePos(new BlockPos(6,1,6));
        var logs=new ItemStack(Items.BIRCH_LOG,3);logs.getOrCreateTag().putUUID("SocratesTestBatch",batch);
        var itemA=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),a.getX()+0.5,a.getY(),a.getZ()+0.5,logs.copy());
        var itemB=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),b.getX()+0.5,b.getY(),b.getZ()+0.5,logs.copy());
        itemA.setDeltaMovement(0,0,0);itemB.setDeltaMovement(0,0,0);
        itemA.setNoPickUpDelay();itemB.setNoPickUpDelay();h.getLevel().addFreshEntity(itemA);h.getLevel().addFreshEntity(itemB);
        h.runAfterDelay(100,()->{
            h.assertTrue(countBatchLogs(first,batch)==3 && countBatchLogs(second,batch)==3,
                "Both scheduler phases must collect; counts="+countBatchLogs(first,batch)+","+countBatchLogs(second,batch));
            h.assertTrue(!itemA.isAlive()&&!itemB.isAlive(),"Collected items duplicated");h.succeed();
        });
    }
    private static int countBatchLogs(SocratesEntity companion,UUID batch) {
        int count=0;
        for(int slot=0;slot<companion.getInventory().getContainerSize();slot++) {
            var stack=companion.getInventory().getItem(slot);var tag=stack.getTag();
            if(stack.is(Items.BIRCH_LOG)&&tag!=null&&tag.hasUUID("SocratesTestBatch")&&batch.equals(tag.getUUID("SocratesTestBatch")))count+=stack.getCount();
        }
        return count;
    }
    @GameTest(template="floor",timeoutTicks=140)
    public static void collectDiagonalBlockCorner(GameTestHelper h) {
        floor(h);h.getLevel().setDayTime(1000);UUID batch=UUID.randomUUID();
        var companion=h.spawn(CompanionMod.SOCRATES.get(),new BlockPos(6,1,6));
        var origin=h.absolutePos(BlockPos.ZERO);
        // Reproduce the off-center approach measured in the production client.
        companion.moveTo(origin.getX()+6.26,origin.getY()+1,origin.getZ()+6.7,-70,0);
        companion.setHome(companion.blockPosition());h.assertTrue(companion.setTask("collect"),"Task rejected");
        var pos=h.absolutePos(new BlockPos(2,1,3));var logs=new ItemStack(Items.BIRCH_LOG,3);
        logs.getOrCreateTag().putUUID("SocratesTestBatch",batch);
        var item=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,logs);
        item.setDeltaMovement(0,0,0);item.setNoPickUpDelay();h.getLevel().addFreshEntity(item);
        h.runAfterDelay(120,()->{
            h.assertTrue(countBatchLogs(companion,batch)==3,"Diagonal pickup stopped short; pos="+companion.position()+" item="+item.position()+" alive="+item.isAlive());
            h.assertTrue(!item.isAlive(),"Diagonal pickup duplicated item");h.succeed();
        });
    }
    @GameTest(template="floor",timeoutTicks=20)
    public static void actionValidation(GameTestHelper h) {
        boolean rejected=false;
        try {AiBridge.validate(com.google.gson.JsonParser.parseString("{\"action\":\"kill\",\"reply\":\"bad\"}").getAsJsonObject());}catch(IllegalArgumentException e){rejected=true;}
        h.assertTrue(rejected,"Unsafe action accepted");
        rejected=false;
        try {AiBridge.validate(com.google.gson.JsonParser.parseString("{\"action\":\"stay\",\"reply\":\"ok\",\"command\":\"kill\"}").getAsJsonObject());}catch(IllegalArgumentException e){rejected=true;}
        h.assertTrue(rejected,"Extra parameters accepted");h.succeed();
    }
}
