package com.jacob.companion;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class SocratesEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> SKIN = SynchedEntityData.defineId(SocratesEntity.class, EntityDataSerializers.STRING);
    public enum Mode { RESIDENT, ADVENTURE, HELPER, STAY }
    private final net.minecraft.world.SimpleContainer inventory=new net.minecraft.world.SimpleContainer(9);
    private String task="";
    private int taskTicks;
    private int planRevision;
    private boolean autonomy;
    public int getPlanRevision() {return planRevision;}
    public String getTask() {return task;}
    public net.minecraft.world.SimpleContainer getInventory() {return inventory;}
    public void setAutonomy(boolean enabled) {autonomy=enabled;planRevision++;}
    public boolean isAutonomy() {return autonomy;}
    public boolean setTask(String value) {
        if(!java.util.Set.of("collect","explore").contains(value) || home==null || home.distSqr(blockPosition())>256)return false;
        setMode(Mode.HELPER);task=value;taskTicks=1200;return true;
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide) {
            if(taskTicks>0 && --taskTicks==0) {task="";getNavigation().stop();planRevision++;}
            if(autonomy && mode!=Mode.STAY && task.isEmpty() && tickCount%1200==0 && owner!=null) {
                var player=getServer().getPlayerList().getPlayer(owner);
                if(player!=null && player.level()==level() && distanceToSqr(player)<1024)AiBridge.ask(player,this,"[autonomy]");
            }
        }
    }
    @Override protected void dropCustomDeathLoot(net.minecraft.world.damagesource.DamageSource source,int looting,boolean hit) {
        super.dropCustomDeathLoot(source,looting,hit);
        for(int i=0;i<inventory.getContainerSize();i++)spawnAtLocation(inventory.removeItemNoUpdate(i));
    }
    private Mode mode = Mode.RESIDENT;
    private UUID owner;
    private BlockPos home;
    public SocratesEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.literal("Socrates"));
        setCustomNameVisible(true);
    }
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,0.27).add(Attributes.FOLLOW_RANGE,48);
    }
    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(SKIN,"Socrates"); }
    public String getSkinUsername() { return entityData.get(SKIN); }
    public void setSkinUsername(String value) { entityData.set(SKIN,value); }
    public void setOwner(UUID value) { owner=value; }
    public UUID getOwner() { return owner; }
    public Mode getMode() { return mode; }
    public void setMode(Mode value) { mode=value; task="";taskTicks=0;planRevision++;if(value==Mode.STAY)autonomy=false;getNavigation().stop(); }
    public void setHome(BlockPos value) { home=value.immutable();planRevision++;restrictTo(home,16); }
    public BlockPos getHome() { return home; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            public boolean canUse() { return mode==Mode.STAY || (mode==Mode.HELPER && task.isEmpty()); }
            public void start() { getNavigation().stop(); }
            public void tick() { getNavigation().stop(); }
        });
        goalSelector.addGoal(2,new AvoidEntityGoal<>(this,net.minecraft.world.entity.monster.Monster.class,8.0f,1.1,1.3) {
            @Override public boolean canUse() { return (mode==Mode.ADVENTURE || mode==Mode.HELPER) && super.canUse(); }
        });
        goalSelector.addGoal(3,new Goal() {
            private Player target;
            { setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
            public boolean canUse() {
                target=owner==null?null:level().getPlayerByUUID(owner);
                return mode==Mode.ADVENTURE && target!=null && !target.isSpectator() && distanceToSqr(target)>9 && distanceToSqr(target)<2304;
            }
            public boolean canContinueToUse() { return canUse(); }
            // Mob staggers normal goals by entity ID; tickCount%10 would otherwise
            // never run for one of the two phases. Navigation remains throttled.
            @Override public boolean requiresUpdateEveryTick() { return true; }
            public void tick() {
                getLookControl().setLookAt(target,30,30);
                if(tickCount%10==0) getNavigation().moveTo(target,1.1);
            }
            public void stop() { getNavigation().stop(); }
        });
        goalSelector.addGoal(4,new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE)); }
            public boolean canUse() { return (mode==Mode.RESIDENT || (mode==Mode.HELPER && !task.isEmpty())) && home!=null && home.distSqr(blockPosition())>(level().isDay()?144:4); }
            @Override public boolean requiresUpdateEveryTick() { return true; }
            public void tick() { if(tickCount%20==0) getNavigation().moveTo(home.getX()+0.5,home.getY(),home.getZ()+0.5,1); }
            public void stop() { getNavigation().stop(); }
        });
        goalSelector.addGoal(5,new Goal() {
            private net.minecraft.world.entity.item.ItemEntity item;
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            public boolean canUse() {
                if(mode!=Mode.HELPER || !task.equals("collect") || home==null)return false;
                item=level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,getBoundingBox().inflate(8),i->i.isAlive() && !i.hasPickUpDelay() && home.distSqr(i.blockPosition())<=144 && inventory.canAddItem(i.getItem())).stream().min(java.util.Comparator.comparingDouble(SocratesEntity.this::distanceToSqr)).orElse(null);
                return item!=null;
            }
            public boolean canContinueToUse() {return mode==Mode.HELPER && task.equals("collect") && item!=null && item.isAlive() && !item.hasPickUpDelay() && distanceToSqr(item)<144 && home.distSqr(item.blockPosition())<=144 && inventory.canAddItem(item.getItem());}
            @Override public boolean requiresUpdateEveryTick() { return true; }
            public void tick() {
                // Goal eligibility is only refreshed on alternating server ticks.
                // Revalidate here too: an item discarded on the previous tick
                // must never be inserted into the inventory a second time.
                if(!canContinueToUse()) {getNavigation().stop();return;}
                getLookControl().setLookAt(item,30,30);
                if(distanceToSqr(item)<2.25 && hasLineOfSight(item)) {
                    var remaining=inventory.addItem(item.getItem().copy());
                    if(remaining.isEmpty())item.discard();else item.setItem(remaining);
                } else if(tickCount%10==0) {
                    // The entity overload accepts a path ending one block away;
                    // an off-center diagonal approach can then stop outside the
                    // existing 1.5-block pickup reach. Request the target block.
                    getNavigation().moveTo(getNavigation().createPath(item,0),0.8);
                }
            }
            public void stop() {getNavigation().stop();item=null;}
        });
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,0.7) {
            @Override public boolean canUse() { return (mode==Mode.RESIDENT || (mode==Mode.HELPER && task.equals("explore"))) && level().isDay() && super.canUse(); }
            @Override public boolean canContinueToUse() { return (mode==Mode.RESIDENT || (mode==Mode.HELPER && task.equals("explore"))) && level().isDay() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Inventory",inventory.createTag());tag.putString("Task",task);tag.putInt("TaskTicks",taskTicks);tag.putBoolean("Autonomy",autonomy);
        tag.putString("SkinUsername",getSkinUsername()); tag.putString("SocratesMode",mode.name());
        if(owner!=null) tag.putUUID("SocratesOwner",owner);
        if(home!=null) tag.putLong("SocratesHome",home.asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if(tag.contains("SkinUsername")) setSkinUsername(tag.getString("SkinUsername"));
        try { mode=Mode.valueOf(tag.getString("SocratesMode")); } catch(IllegalArgumentException ignored) { mode=Mode.RESIDENT; }
        if(tag.hasUUID("SocratesOwner")) owner=tag.getUUID("SocratesOwner");
        if(tag.contains("SocratesHome")) setHome(BlockPos.of(tag.getLong("SocratesHome")));
        inventory.fromTag(tag.getList("Inventory",10));
        String savedTask=tag.getString("Task");task=java.util.Set.of("collect","explore").contains(savedTask) && mode==Mode.HELPER?savedTask:"";
        taskTicks=task.isEmpty()?0:Math.max(0,Math.min(1200,tag.getInt("TaskTicks")));
        if(taskTicks==0)task="";
        autonomy=mode!=Mode.STAY && tag.getBoolean("Autonomy");
    }
}
