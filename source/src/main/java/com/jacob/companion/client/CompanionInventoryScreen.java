package com.jacob.companion.client;

import com.jacob.companion.CompanionInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    private static final ResourceLocation TEXTURE=new ResourceLocation("minecraft","textures/gui/container/generic_54.png");
    public CompanionInventoryScreen(CompanionInventoryMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);imageHeight=178;inventoryLabelY=84;
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int x,int y) {
        g.blit(TEXTURE,leftPos,topPos,0,0,176,35);
        g.fill(leftPos,topPos+35,leftPos+176,topPos+88,0xffc6c6c6);
        for(int i=0;i<6;i++)g.blit(TEXTURE,leftPos+7+i*27,topPos+51,7,17,18,18);
        g.blit(TEXTURE,leftPos,topPos+88,0,126,176,90);
        String[] labels={"Hand","Off","Head","Body","Legs","Feet"};
        for(int i=0;i<6;i++)g.drawString(font,labels[i],leftPos+7+i*27,topPos+40,0xff404040,false);
    }
    @Override public void render(GuiGraphics g,int x,int y,float partial) {renderBackground(g);super.render(g,x,y,partial);renderTooltip(g,x,y);}
}
