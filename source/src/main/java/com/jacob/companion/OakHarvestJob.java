package com.jacob.companion;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;

/** Deliberately narrow vanilla-oak adapter. Only freshly dropped, verified logs count. */
public final class OakHarvestJob extends Goal {
    private final SocratesEntity s;
    private UUID id=UUID.randomUUID();
    private BlockPos origin,target;
    private final Set<BlockPos> approved=new HashSet<>();
    private final Set<BlockPos> failed=new HashSet<>();
    private int requested,obtained,remainingTicks,stalled,lastScan;
    private boolean personal,handedOver;
    public boolean reservedForOwner() {return state.equals("completed")&&!personal&&!handedOver;}
    public void observeInventory() {if(reservedForOwner()&&s.getInventory().countItem(Items.OAK_LOG)==0)handedOver=true;}
    public void setPersonal(boolean value) {personal=value;}
    public boolean personal() {return personal;}
    private String state="idle",reason="No oak job yet.";
    private double previousDistance=Double.MAX_VALUE;
    private float breaking;
    private ItemStack breakingTool=ItemStack.EMPTY;
    public OakHarvestJob(SocratesEntity s) {this.s=s;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    public String state() {return state;}
    public int obtained() {return obtained;}
    public UUID id() {return id;}
    public String summary() {return "Oak logs "+obtained+"/"+requested+" — "+state+": "+reason;}
    public boolean active() {return state.equals("running")||state.equals("blocked");}
    public void start(int count) {
        id=UUID.randomUUID();origin=s.blockPosition();target=null;approved.clear();failed.clear();
        personal=false;handedOver=false;requested=count;obtained=0;remainingTicks=12000;stalled=0;lastScan=-200;breaking=0;
        state="running";reason="Looking for nearby small oak trees.";
    }
    public void cancel() {if(active()){state="cancelled";reason="Cancelled by a new instruction.";clearTarget();}}
    private void clearTarget() {
        if(target!=null&&!s.level().isClientSide)s.level().destroyBlockProgress(s.getId(),target,-1);
        target=null;breaking=0;stalled=0;previousDistance=Double.MAX_VALUE;s.getNavigation().stop();
    }
    private void blocked(String why) {boolean changed=!state.equals("blocked")||!reason.equals(why);state="blocked";reason=why;s.getNavigation().stop();if(changed)notifyOwner();}
    @Override public boolean canUse() {return active()&&s.getTask().equals("oak_logs");}
    @Override public boolean requiresUpdateEveryTick() {return true;}
    @Override public void stop() {clearTarget();}
    @Override public void tick() {
        if(!(s.level() instanceof ServerLevel level)||!active())return;
        if(--remainingTicks<=0) {state="expired";reason="Work time ended before the requested quantity was gathered.";clearTarget();s.finishOakJob();notifyOwner();return;}
        if(!equipAxe()) {blocked("I need a usable axe in my hand or inventory.");return;}
        if(!s.getInventory().canAddItem(new ItemStack(Items.OAK_LOG))) {blocked("My inventory is full.");return;}
        if(target==null||!level.getBlockState(target).is(Blocks.OAK_LOG)) {
            clearTarget();
            if(s.tickCount-lastScan<40)return;
            lastScan=s.tickCount;scan(level);
            target=approved.stream().filter(p->level.hasChunkAt(p)&&level.getBlockState(p).is(Blocks.OAK_LOG)&&!failed.contains(p))
                .min(Comparator.comparingDouble(p->p.distToCenterSqr(s.position()))).orElse(null);
            if(target==null) {blocked("No reachable approved oak trunks found in this work area. Move me to more trees and ask again.");return;}
        }
        state="running";reason="Gathering oak logs near the job's starting point.";
        if(!level.hasChunkAt(target)||!level.getWorldBorder().isWithinBounds(target)||target.distSqr(origin)>400) {failed.add(target);clearTarget();return;}
        var hit=level.clip(new ClipContext(s.getEyePosition(),Vec3.atCenterOf(target),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s));
        double distance=s.distanceToSqr(Vec3.atCenterOf(target));
        boolean inReach=s.getEyePosition().distanceToSqr(Vec3.atCenterOf(target))<=20.25;
        if(!inReach||!hit.getBlockPos().equals(target)) {
            breaking=0;level.destroyBlockProgress(s.getId(),target,-1);
            if(distance<previousDistance-0.04) {previousDistance=distance;stalled=0;}else stalled++;
            if(stalled>160) {failed.add(target);clearTarget();blocked("A trunk was unreachable; trying other trees.");return;}
            if(s.tickCount%20==0) {
                net.minecraft.world.level.pathfinder.Path path=null;
                for(var direction:Direction.Plane.HORIZONTAL) {
                    var candidate=target.relative(direction);
                    // Stand near the trunk at the companion's current floor height, not on top of it.
                    candidate=new BlockPos(candidate.getX(),s.blockPosition().getY(),candidate.getZ());
                    var attempt=s.getNavigation().createPath(candidate,0);
                    if(attempt!=null&&attempt.canReach()){path=attempt;break;}
                }
                if(path!=null)s.getNavigation().moveTo(path,0.9);
            }
            return;
        }
        s.getNavigation().stop();s.getLookControl().setLookAt(Vec3.atCenterOf(target));
        var actor=FakePlayerFactory.get(level,new GameProfile(s.getUUID(),"[Socrates]"));
        actor.setGameMode(GameType.SURVIVAL);actor.setPos(s.position());actor.setOnGround(s.onGround());
        actor.setItemSlot(EquipmentSlot.MAINHAND,s.getMainHandItem().copy());
        if(!ItemStack.matches(breakingTool,s.getMainHandItem())) {breaking=0;breakingTool=s.getMainHandItem().copy();}
        var block=level.getBlockState(target);
        breaking+=block.getDestroyProgress(actor,level,target);
        level.destroyBlockProgress(s.getId(),target,Math.min(9,(int)(breaking*10)));
        if(s.tickCount%6==0)s.swing(InteractionHand.MAIN_HAND);
        if(breaking<1)return;
        var box=new AABB(target).inflate(2);
        var before=new HashSet<UUID>();for(var item:level.getEntitiesOfClass(ItemEntity.class,box))before.add(item.getUUID());
        boolean broken;
        try {broken=actor.gameMode.destroyBlock(target);s.setItemSlot(EquipmentSlot.MAINHAND,actor.getMainHandItem().copy());}
        finally {actor.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);}
        if(!broken||level.getBlockState(target).is(Blocks.OAK_LOG)) {failed.add(target);clearTarget();blocked("A block-break rule prevented harvesting this trunk.");return;}
        int gained=0;
        for(var item:level.getEntitiesOfClass(ItemEntity.class,box)) {
            if(before.contains(item.getUUID())||!item.getItem().is(Items.OAK_LOG))continue;
            int take=Math.min(item.getItem().getCount(),requested-obtained-gained);if(take<=0)break;
            var offered=item.getItem().copy();offered.setCount(take);
            var remainder=s.getInventory().addItem(offered);int inserted=take-remainder.getCount();
            gained+=inserted;item.getItem().shrink(inserted);if(item.getItem().isEmpty())item.discard();
        }
        obtained+=gained;clearTarget();
        if(gained==0) {blocked("The block broke, but no oak logs reached my inventory.");}
        if(obtained>=requested) {state="completed";reason="Gathered the requested logs. They are in my inventory.";s.finishOakJob();notifyOwner();}
    }
    private boolean equipAxe() {
        if(s.getMainHandItem().getItem() instanceof AxeItem)return true;
        for(int i=0;i<9;i++)if(s.getInventory().getItem(i).getItem() instanceof AxeItem) {
            var axe=s.getInventory().getItem(i);s.getInventory().setItem(i,s.getMainHandItem());s.setItemSlot(EquipmentSlot.MAINHAND,axe);s.setDropChance(EquipmentSlot.MAINHAND,1);return true;
        }
        return false;
    }
    private void scan(ServerLevel level) {
        if(origin==null)return;
        // Limit searches to loaded blocks and a 12-block horizontal work area. No chunk loads.
        for(var p:BlockPos.betweenClosed(origin.offset(-12,-3,-12),origin.offset(12,3,12))) {
            if(!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||!level.getBlockState(p).is(Blocks.OAK_LOG))continue;
            var ground=level.getBlockState(p.below());
            if(!ground.is(Blocks.DIRT)&&!ground.is(Blocks.GRASS_BLOCK)&&!ground.is(Blocks.ROOTED_DIRT))continue;
            int height=0;while(height<7&&level.getBlockState(p.above(height)).is(Blocks.OAK_LOG))height++;
            if(height<2||height>6)continue;
            boolean leaves=false;
            var crown=p.above(height-1);
            for(var leaf:BlockPos.betweenClosed(crown.offset(-2,0,-2),crown.offset(2,2,2))) {
                if(!level.hasChunkAt(leaf))continue;
                var state=level.getBlockState(leaf);
                if(state.is(Blocks.OAK_LEAVES)&&!state.getValue(LeavesBlock.PERSISTENT)){leaves=true;break;}
            }
            if(leaves)for(int y=0;y<height&&approved.size()<256;y++)approved.add(p.above(y).immutable());
        }
    }
    private void notifyOwner() {
        if(s.getOwner()!=null) {var p=s.getServer().getPlayerList().getPlayer(s.getOwner());if(p!=null)InteractionNetwork.message(p,s,"Socrates: "+summary());}
    }
    public CompoundTag save() {
        var t=new CompoundTag();t.putBoolean("HandedOver",handedOver);t.putBoolean("Personal",personal);t.putUUID("Id",id);t.putString("State",state);t.putString("Reason",reason);
        t.putInt("Requested",requested);t.putInt("Obtained",obtained);t.putInt("Ticks",remainingTicks);
        if(origin!=null)t.putLong("Origin",origin.asLong());
        t.putLongArray("Approved",approved.stream().mapToLong(BlockPos::asLong).toArray());return t;
    }
    public void load(CompoundTag t) {
        personal=t.getBoolean("Personal");handedOver=t.getBoolean("HandedOver");
        if(t.hasUUID("Id"))id=t.getUUID("Id");
        state=t.getString("State");if(!Set.of("idle","running","blocked","completed","cancelled","expired").contains(state))state="idle";
        reason=t.getString("Reason");requested=Math.max(0,Math.min(64,t.getInt("Requested")));obtained=Math.max(0,Math.min(requested,t.getInt("Obtained")));
        remainingTicks=Math.max(0,Math.min(12000,t.getInt("Ticks")));origin=t.contains("Origin")?BlockPos.of(t.getLong("Origin")):null;
        approved.clear();for(long p:t.getLongArray("Approved")) {var pos=BlockPos.of(p);if(approved.size()<256&&origin!=null&&pos.distSqr(origin)<=400)approved.add(pos);}
        failed.clear();target=null;lastScan=-200;
        if(active()&&(origin==null||requested==0||remainingTicks==0)) {state="expired";reason="Invalid or expired saved job.";}
    }
}
