package com.jacob.companion;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class InteractionNetwork {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(CompanionMod.MODID, "interaction"), () -> VERSION, VERSION::equals, VERSION::equals);

    public record Open(int entityId, UUID entity, String status) { }
    public record Request(UUID entity, InteractionActions.Action action, String text) { }
    public record Reply(UUID entity, String text, String status) { }

    public static void register() {
        CHANNEL.registerMessage(0, Open.class,
            (m,b) -> {b.writeVarInt(m.entityId());b.writeUUID(m.entity());b.writeUtf(m.status(),1000);},
            b -> new Open(b.readVarInt(),b.readUUID(),b.readUtf(1000)),
            InteractionNetwork::openClient, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, Request.class,
            (m,b) -> {b.writeUUID(m.entity());b.writeEnum(m.action());b.writeUtf(m.text(),1000);},
            b -> new Request(b.readUUID(),b.readEnum(InteractionActions.Action.class),b.readUtf(1000)),
            InteractionNetwork::handleRequest, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, Reply.class,
            (m,b) -> {b.writeUUID(m.entity());b.writeUtf(m.text(),2000);b.writeUtf(m.status(),1000);},
            b -> new Reply(b.readUUID(),b.readUtf(2000),b.readUtf(1000)),
            InteractionNetwork::replyClient, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void open(ServerPlayer player, SocratesEntity companion) {
        if (InteractionActions.canControl(player,companion))
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Open(companion.getId(),companion.getUUID(),InteractionActions.status(companion)));
    }

    public static void request(UUID entity, InteractionActions.Action action, String text) {
        CHANNEL.sendToServer(new Request(entity,action,text));
    }

    /** Keep command/chat feedback, and also deliver async responses into an open menu. */
    public static void message(ServerPlayer player, SocratesEntity companion, String text) {
        message(player,companion,text,text);
    }

    public static void message(ServerPlayer player, SocratesEntity companion, String text, String display) {
        player.sendSystemMessage(Component.literal(text));
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Reply(companion.getUUID(),display,InteractionActions.status(companion)));
    }

    private static void handleRequest(Request request, Supplier<NetworkEvent.Context> context) {
        var ctx=context.get();
        ctx.enqueueWork(() -> {
            var player=ctx.getSender();if(player==null)return;
            var entity=player.serverLevel().getEntity(request.entity());
            if(!(entity instanceof SocratesEntity companion)) {
                player.sendSystemMessage(Component.literal("Socrates is no longer loaded nearby."));return;
            }
            var result=InteractionActions.apply(player,companion,request.action(),request.text());
            if(!result.accepted()) {
                // Do not reveal another owner's companion status to a forged request.
                player.sendSystemMessage(Component.literal(result.message()));
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Reply(request.entity(),result.message(),""));
                return;
            }
            if(request.action()==InteractionActions.Action.INVENTORY) {
                net.minecraftforge.network.NetworkHooks.openScreen(player,new net.minecraft.world.SimpleMenuProvider(
                    (id,inventory,p)->new CompanionInventoryMenu(id,inventory,companion),Component.literal("Socrates — Inventory")),b->b.writeVarInt(companion.getId()));
                return;
            }
            if(request.action()==InteractionActions.Action.REFRESH) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),new Reply(request.entity(),"",InteractionActions.status(companion)));
                return;
            }
            message(player,companion,result.message());
            if(request.action()==InteractionActions.Action.TALK)AiBridge.ask(player,companion,request.text());
        });
        ctx.setPacketHandled(true);
    }

    private static void openClient(Open message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.jacob.companion.client.InteractionClient.open(message)));
        context.get().setPacketHandled(true);
    }

    private static void replyClient(Reply message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.jacob.companion.client.InteractionClient.reply(message)));
        context.get().setPacketHandled(true);
    }
}
