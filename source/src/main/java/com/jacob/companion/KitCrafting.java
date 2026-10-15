package com.jacob.companion;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;

/** A small recipe-validated personal equipment progression. No free materials or remote bench. */
public final class KitCrafting {
    public static boolean has(SocratesEntity s,java.util.function.Predicate<ItemStack> test) {
        for(var slot:EquipmentSlot.values())if(test.test(s.getItemBySlot(slot)))return true;
        for(int i=0;i<9;i++)if(test.test(s.getInventory().getItem(i)))return true;return false;
    }
    public static boolean tableNearby(SocratesEntity s) {
        for(var p:BlockPos.betweenClosed(s.blockPosition().offset(-3,-2,-3),s.blockPosition().offset(3,2,3))) {
            if(!s.level().hasChunkAt(p)||!s.level().getBlockState(p).is(Blocks.CRAFTING_TABLE)||s.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>20.25)continue;
            var hit=s.level().clip(new net.minecraft.world.level.ClipContext(s.getEyePosition(),Vec3.atCenterOf(p),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,s));
            if(hit.getBlockPos().equals(p))return true;
        }
        return false;
    }
    public static String step(SocratesEntity s) {
        if(!(s.level() instanceof ServerLevel level))return "Crafting is server-only.";
        // Equip existing armor before spending resources on more gear.
        for(int i=0;i<9;i++) {
            var stack=s.getInventory().getItem(i);
            if(stack.getItem() instanceof ArmorItem armor&&s.getItemBySlot(armor.getEquipmentSlot()).isEmpty()) {
                s.setItemSlot(armor.getEquipmentSlot(),stack.split(1));s.setDropChance(armor.getEquipmentSlot(),1);return "Equipped my "+armor.getDescription().getString()+".";
            }
        }
        boolean axe=has(s,x->x.getItem() instanceof AxeItem),pick=has(s,x->x.getItem() instanceof PickaxeItem);
        Item desired=!axe?Items.WOODEN_AXE:!pick?Items.WOODEN_PICKAXE:s.getItemBySlot(EquipmentSlot.CHEST).isEmpty()&&s.getInventory().countItem(Items.IRON_INGOT)>=8?Items.IRON_CHESTPLATE:null;
        if(desired==null)return "My supported travel kit is ready. Armor needs suitable materials.";
        if(!tableNearby(s))return "I need an accessible crafting table within reach to make equipment.";
        Item[] grid=new Item[9];Arrays.fill(grid,Items.AIR);
        if(desired==Items.IRON_CHESTPLATE) {
            Arrays.fill(grid,Items.IRON_INGOT);grid[1]=Items.AIR;
        } else {
            int planks=s.getInventory().countItem(Items.OAK_PLANKS),sticks=s.getInventory().countItem(Items.STICK);
            if(planks<(sticks<2?5:3)) {
                if(s.getInventory().countItem(Items.OAK_LOG)==0)return "I need oak logs or planks to prepare my tools.";
                grid[0]=Items.OAK_LOG;desired=Items.OAK_PLANKS;
            } else if(sticks<2) {grid[0]=Items.OAK_PLANKS;grid[3]=Items.OAK_PLANKS;desired=Items.STICK;}
            else if(desired==Items.WOODEN_AXE) {grid[0]=grid[1]=grid[3]=Items.OAK_PLANKS;grid[4]=grid[7]=Items.STICK;}
            else {grid[0]=grid[1]=grid[2]=Items.OAK_PLANKS;grid[4]=grid[7]=Items.STICK;}
        }
        var simulated=new net.minecraft.world.SimpleContainer(9);
        for(int i=0;i<9;i++)simulated.setItem(i,s.getInventory().getItem(i).copy());
        var menu=new AbstractContainerMenu(null,-1) {
            public boolean stillValid(Player player){return false;}
            public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
        };
        var crafting=new TransientCraftingContainer(menu,3,3);
        for(int cell=0;cell<9;cell++) {
            if(grid[cell]==Items.AIR)continue;
            int slot=-1;for(int i=0;i<9;i++)if(simulated.getItem(i).is(grid[cell])){slot=i;break;}
            if(slot<0)return "I do not have all the ingredients for my next tool.";
            crafting.setItem(cell,simulated.removeItem(slot,1));
        }
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,crafting,level);
        if(recipe.isEmpty())return "This modpack does not provide the supported recipe.";
        var output=recipe.get().assemble(crafting,level.registryAccess());
        if(!output.is(desired)||output.isEmpty())return "The installed recipe has a different result; I will not substitute it.";
        var actor=FakePlayerFactory.get(level,new GameProfile(s.getUUID(),"[Socrates]"));
        net.minecraftforge.common.ForgeHooks.setCraftingPlayer(actor);
        net.minecraft.core.NonNullList<ItemStack> remains;
        try {remains=recipe.get().getRemainingItems(crafting);}finally{net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);}
        if(!simulated.addItem(output.copy()).isEmpty())return "I need inventory space to craft.";
        for(var rest:remains)if(!simulated.addItem(rest.copy()).isEmpty())return "I need room for crafting leftovers.";
        for(int i=0;i<9;i++)s.getInventory().setItem(i,simulated.getItem(i));
        net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(actor,output,crafting);
        return "Crafted "+output.getCount()+" "+output.getHoverName().getString()+" for my equipment.";
    }
}
