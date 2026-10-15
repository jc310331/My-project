package com.jacob.companion.client;

import com.jacob.companion.InteractionNetwork;
import com.jacob.companion.SocratesEntity;
import net.minecraft.client.Minecraft;

public final class InteractionClient {
    public static void open(InteractionNetwork.Open packet) {
        var minecraft=Minecraft.getInstance();
        if(minecraft.level==null)return;
        var entity=minecraft.level.getEntity(packet.entityId());
        if(entity instanceof SocratesEntity companion && companion.getUUID().equals(packet.entity()))
            minecraft.setScreen(new SocratesInteractionScreen(companion,packet.status()));
    }
    public static void reply(InteractionNetwork.Reply packet) {
        if(Minecraft.getInstance().screen instanceof SocratesInteractionScreen screen && screen.companionId().equals(packet.entity()))
            screen.receive(packet.text(),packet.status());
    }
}
