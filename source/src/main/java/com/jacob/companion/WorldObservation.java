package com.jacob.companion;

import com.google.gson.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.registries.ForgeRegistries;

public final class WorldObservation {
    public static JsonObject snapshot(SocratesEntity s) {
        var o=new JsonObject();o.addProperty("health",s.getHealth());o.addProperty("mode",s.getMode().name());o.addProperty("task",s.getTask());
        o.addProperty("dimension",s.level().dimension().location().toString());
        var position=new JsonArray();position.add(s.blockPosition().getX());position.add(s.blockPosition().getY());position.add(s.blockPosition().getZ());o.add("position",position);
        var counts=new java.util.TreeMap<String,Integer>();
        for(int i=0;i<9;i++)count(counts,s.getInventory().getItem(i));
        for(var slot:EquipmentSlot.values())count(counts,s.getItemBySlot(slot));
        var items=new JsonObject();counts.forEach(items::addProperty);o.add("items",items);
        o.addProperty("has_axe",KitCrafting.has(s,x->x.getItem() instanceof net.minecraft.world.item.AxeItem));
        o.addProperty("has_pickaxe",KitCrafting.has(s,x->x.getItem() instanceof net.minecraft.world.item.PickaxeItem));
        o.addProperty("chest_armor",!s.getItemBySlot(EquipmentSlot.CHEST).isEmpty());
        o.addProperty("crafting_table",KitCrafting.tableNearby(s));
        var job=new JsonObject();job.addProperty("reserved",s.oakJob().reservedForOwner());job.addProperty("personal",s.oakJob().personal());job.addProperty("id",s.oakJob().id().toString());job.addProperty("state",s.oakJob().state());job.addProperty("summary",s.oakJob().summary());o.add("job",job);
        return o;
    }
    private static void count(java.util.Map<String,Integer> counts,net.minecraft.world.item.ItemStack stack) {
        if(!stack.isEmpty())counts.merge(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(),stack.getCount(),Integer::sum);
    }
}
