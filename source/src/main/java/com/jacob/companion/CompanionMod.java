package com.jacob.companion;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(CompanionMod.MODID)
public class CompanionMod {
    public static final String MODID="companion";
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MODID);
    public static final RegistryObject<EntityType<SocratesEntity>> SOCRATES=ENTITIES.register("socrates",()->EntityType.Builder.of(SocratesEntity::new,MobCategory.CREATURE).sized(0.6f,1.8f).clientTrackingRange(8).build("companion:socrates"));
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,MODID);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<CompanionInventoryMenu>> INVENTORY=MENUS.register("inventory",()->net.minecraftforge.common.extensions.IForgeMenuType.create(CompanionInventoryMenu::new));
    public CompanionMod() {
        var bus=FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);MENUS.register(bus); bus.addListener(this::attributes); MinecraftForge.EVENT_BUS.register(this);
        InteractionNetwork.register();
    }
    private void attributes(EntityAttributeCreationEvent event) { event.put(SOCRATES.get(),SocratesEntity.createAttributes().build()); }
    private SocratesEntity find(ServerPlayer p) {
        for(var level:p.getServer().getAllLevels()) for(var e:level.getAllEntities())
            if(e instanceof SocratesEntity s && p.getUUID().equals(s.getOwner())) return s;
        return null;
    }
    private int action(CommandContext<CommandSourceStack> ctx,String action) throws CommandSyntaxException {
        var p=ctx.getSource().getPlayerOrException(); var s=find(p);
        if(action.equals("spawn")) {
            // Durable UUID prevents creating duplicates when the existing entity's chunk is unloaded.
            var data=p.getServer().overworld().getDataStorage().computeIfAbsent(CompanionRegistry::load,CompanionRegistry::new,"socrates_registry");
            if(s!=null || data.contains(p.getUUID())) { ctx.getSource().sendFailure(Component.literal("Socrates already exists. Load his last location; use status for details.")); return 0; }
            s=SOCRATES.get().create(p.serverLevel()); if(s==null) return 0;
            s.moveTo(p.getX()+2,p.getY(),p.getZ(),p.getYRot(),0);
            if(!p.level().noCollision(s) || !p.level().getWorldBorder().isWithinBounds(s.blockPosition())) { ctx.getSource().sendFailure(Component.literal("No room beside you. Move to an open area.")); return 0; }
            s.setOwner(p.getUUID()); s.setHome(s.blockPosition());
            if(!p.serverLevel().addFreshEntity(s)) return 0;
            data.put(p.getUUID(),s.getUUID(),p.level().dimension().location()+" "+s.blockPosition().toShortString());
        } else {
            if(s==null) { var registry=p.getServer().overworld().getDataStorage().computeIfAbsent(CompanionRegistry::load,CompanionRegistry::new,"socrates_registry"); ctx.getSource().sendFailure(Component.literal("Socrates is not loaded. Recorded spawn: "+registry.location(p.getUUID())+". No chunks are force-loaded.")); return 0; }
            switch(action) {
                case "talk" -> { AiBridge.ask(p,s,StringArgumentType.getString(ctx,"message"));return 1; }
                case "autonomy" -> { String enabled=StringArgumentType.getString(ctx,"enabled");if(!enabled.equals("on")&&!enabled.equals("off"))return 0;s.setAutonomy(enabled.equals("on")); }
                case "collect", "explore" -> {if(!s.setTask(action)){ctx.getSource().sendFailure(Component.literal("Set home nearby first; tasks stay within 16 blocks of home."));return 0;}}
                case "unload" -> { if(p.distanceToSqr(s)>16) {ctx.getSource().sendFailure(Component.literal("Stand within four blocks."));return 0;} for(int i=0;i<s.getInventory().getContainerSize();i++){var stack=s.getInventory().removeItemNoUpdate(i);if(!p.getInventory().add(stack))p.drop(stack,false);} }
                case "follow" -> s.setMode(SocratesEntity.Mode.ADVENTURE);
                case "stay" -> s.setMode(SocratesEntity.Mode.STAY);
                case "home" -> s.setHome(s.blockPosition());
                case "mode" -> { try { s.setMode(SocratesEntity.Mode.valueOf(StringArgumentType.getString(ctx,"mode").toUpperCase(java.util.Locale.ROOT))); } catch(IllegalArgumentException e) { ctx.getSource().sendFailure(Component.literal("Modes: resident, adventure, helper, stay")); return 0; } }
                case "skin" -> {
                    String skin=StringArgumentType.getString(ctx,"skin");
                    if(!skin.matches("[A-Za-z0-9_]{3,16}|local:[A-Za-z0-9_-]{1,48}\\.png")) { ctx.getSource().sendFailure(Component.literal("Use a username or local:filename.png")); return 0; }
                    s.setSkinUsername(skin);
                }
            }
        }
        String status="Socrates: "+s.getMode()+" | "+s.level().dimension().location()+" "+s.blockPosition().toShortString()+" | skin="+s.getSkinUsername()+" | home="+s.getHome()+" | task="+s.getTask()+" | autonomy="+s.isAutonomy()+" | inventory="+s.getInventory();
        ctx.getSource().sendSuccess(()->Component.literal(status),false); return 1;
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if(event.getEntity() instanceof SocratesEntity s && !s.level().isClientSide && s.getOwner()!=null)
            s.getServer().overworld().getDataStorage().computeIfAbsent(CompanionRegistry::load,CompanionRegistry::new,"socrates_registry").remove(s.getOwner());
    }
    @SubscribeEvent public void registerCommands(RegisterCommandsEvent event) {
        var root=Commands.literal("socrates").requires(s->s.hasPermission(2));
        for(String a:new String[]{"spawn","follow","stay","home","status","collect","explore","unload"}) root.then(Commands.literal(a).executes(c->action(c,a)));
        root.then(Commands.literal("mode").then(Commands.argument("mode",StringArgumentType.word()).suggests((c,b)->{for(var m:SocratesEntity.Mode.values())b.suggest(m.name().toLowerCase(java.util.Locale.ROOT));return b.buildFuture();}).executes(c->action(c,"mode"))));
        root.then(Commands.literal("skin").then(Commands.argument("skin",StringArgumentType.string()).executes(c->action(c,"skin"))));
        root.then(Commands.literal("talk").then(Commands.argument("message",StringArgumentType.greedyString()).executes(c->action(c,"talk"))));
        root.then(Commands.literal("autonomy").then(Commands.argument("enabled",StringArgumentType.word()).suggests((c,b)->b.suggest("on").suggest("off").buildFuture()).executes(c->action(c,"autonomy"))));
        event.getDispatcher().register(root);
    }
}
