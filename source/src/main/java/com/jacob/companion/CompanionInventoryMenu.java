package com.jacob.companion;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Vanilla server-owned slot transactions, including shift-click and carried-stack handling. */
public final class CompanionInventoryMenu extends AbstractContainerMenu {
    private final SocratesEntity companion;
    private static final EquipmentSlot[] GEAR={EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    public CompanionInventoryMenu(int id, Inventory player, FriendlyByteBuf buffer) {
        this(id,player,find(player,buffer.readVarInt()));
    }
    private static SocratesEntity find(Inventory inventory,int id) {
        var entity=inventory.player.level().getEntity(id);
        if(!(entity instanceof SocratesEntity s))throw new IllegalStateException("Companion no longer loaded");
        return s;
    }
    public CompanionInventoryMenu(int id, Inventory player, SocratesEntity companion) {
        super(CompanionMod.INVENTORY.get(),id);this.companion=companion;
        for(int i=0;i<9;i++)addSlot(new Slot(companion.getInventory(),i,8+i*18,18));
        var gear=new SimpleContainer(6) {
            @Override public ItemStack getItem(int slot) {return companion.getItemBySlot(GEAR[slot]);}
            @Override public ItemStack removeItem(int slot,int count) {var result=getItem(slot).split(count);setChanged();return result;}
            @Override public ItemStack removeItemNoUpdate(int slot) {var result=getItem(slot);setItem(slot,ItemStack.EMPTY);return result;}
            @Override public void setItem(int slot,ItemStack stack) {companion.setItemSlot(GEAR[slot],stack);companion.setDropChance(GEAR[slot],1.0f);setChanged();}
            @Override public boolean isEmpty() {for(int i=0;i<6;i++)if(!getItem(i).isEmpty())return false;return true;}
        };
        for(int i=0;i<6;i++) {
            final int gearIndex=i;
            addSlot(new Slot(gear,i,8+i*27,52) {
                @Override public boolean mayPlace(ItemStack stack) {return gearIndex<2||stack.canEquip(GEAR[gearIndex],companion);}
                @Override public int getMaxStackSize() {return 1;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,col+row*9+9,8+col*18,96+row*18));
        for(int i=0;i<9;i++)addSlot(new Slot(player,i,8+i*18,154));
        companion.getInventory().startOpen(player.player);
    }
    @Override public boolean stillValid(Player player) {return InteractionActions.canControl(player,companion);}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(!stillValid(player)||index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<15) {if(!moveItemStackTo(stack,15,51,true))return ItemStack.EMPTY;}
        else {
            boolean moved=false;
            if(stack.getItem() instanceof ArmorItem) {
                for(int i=11;i<15;i++)if(slots.get(i).mayPlace(stack)&&!slots.get(i).hasItem()) {moved=moveItemStackTo(stack,i,i+1,false);break;}
            } else if(stack.getItem() instanceof AxeItem&&!slots.get(9).hasItem())moved=moveItemStackTo(stack,9,10,false);
            if(!moved&&!moveItemStackTo(stack,0,9,false))return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);return original;
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(stillValid(player))super.clicked(slot,button,type,player);
    }
    @Override public void removed(Player player) {super.removed(player);companion.getInventory().stopOpen(player);}
}
