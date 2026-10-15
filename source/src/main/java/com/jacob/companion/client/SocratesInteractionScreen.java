package com.jacob.companion.client;

import com.jacob.companion.InteractionActions.Action;
import com.jacob.companion.InteractionNetwork;
import com.jacob.companion.SocratesEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.loading.FMLPaths;

/** Owner interaction screen; the world keeps running and every action is server-validated. */
public final class SocratesInteractionScreen extends Screen {
    private final SocratesEntity companion;
    private final List<Component> conversation=new ArrayList<>();
    private String status;
    private int left,top,panelWidth,panelHeight,scroll,statusY,conversationY,refreshTicks;
    private EditBox message,skin,quantity;
    private Button send,close,autonomy;
    private boolean appearance,topics,work,compact,pendingMessageFocus;

    public SocratesInteractionScreen(SocratesEntity companion,String status) {
        super(Component.literal("Socrates"));this.companion=companion;this.status=status;
        conversation.add(Component.literal("Socrates: What would you like to do?"));
    }
    public UUID companionId() {return companion.getUUID();}
    @Override public boolean isPauseScreen() {return false;}

    @Override protected void init() {
        String draft=message==null?"":message.getValue();
        panelWidth=Math.min(440,width-20);panelHeight=Math.min(310,height-20);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        compact=panelHeight<290;statusY=top+(compact?154:188);conversationY=statusY+16;
        int actionsX=left+110,actionWidth=(panelWidth-134)/2;
        close=addRenderableWidget(Button.builder(Component.literal("Close"),b->onClose()).bounds(left+panelWidth-58,top+10,48,20).build());
        if(appearance) {
            skin=addRenderableWidget(new EditBox(font,actionsX,top+64,panelWidth-124,20,Component.literal("Skin username or local PNG")));
            skin.setMaxLength(58);skin.setValue(companion.getSkinUsername());
            addRenderableWidget(Button.builder(Component.literal("Apply skin"),b->act(Action.SKIN,skin.getValue().trim())).bounds(actionsX,top+90,actionWidth,20).build());
            addRenderableWidget(Button.builder(Component.literal("Back"),b->{appearance=false;rebuildWidgets();}).bounds(actionsX+actionWidth+8,top+90,actionWidth,20).build());
            addRenderableWidget(Button.builder(Component.literal("Open skin folder"),b->openSkinFolder()).bounds(actionsX,top+118,panelWidth-124,20).build());
        } else if(work) {
            quantity=addRenderableWidget(new EditBox(font,actionsX,top+48,panelWidth-124,20,Component.literal("Oak log quantity, 1 to 64")));
            quantity.setMaxLength(2);quantity.setValue("15");
            addAction("Gather oak logs",actionsX,top+74,actionWidth,()->act(Action.HARVEST,quantity.getValue()));
            addAction("Back",actionsX+actionWidth+8,top+74,actionWidth,()->{work=false;rebuildWidgets();});
            addAction("Collect drops",actionsX,top+100,actionWidth,()->act(Action.COLLECT,""));
            addAction("Explore",actionsX+actionWidth+8,top+100,actionWidth,()->act(Action.EXPLORE,""));
            addAction("Prepare own kit",actionsX,top+126,panelWidth-124,()->say("prepare your equipment"));
        } else if(topics) {
            int y=top+(compact?42:48),step=compact?22:26;
            addAction("Say hello",actionsX,y,actionWidth,()->say("Hello, Socrates."));
            addAction("Your abilities",actionsX+actionWidth+8,y,actionWidth,()->say("What can you help with?"));
            addAction("Memories",actionsX,y+step,actionWidth,()->say("What do you remember?"));
            addAction("Current status",actionsX+actionWidth+8,y+step,actionWidth,()->say("status"));
            addAction("Make a plan",actionsX,y+step*2,actionWidth,()->say("What should we do next?"));
            addAction("Remember a fact",actionsX+actionWidth+8,y+step*2,actionWidth,()->{message.setValue("remember ");focusMessage();});
            addAction("Your goals",actionsX,y+step*3,actionWidth,()->say("What are your goals?"));
            addAction("Back",actionsX+actionWidth+8,y+step*3,actionWidth,()->{topics=false;rebuildWidgets();});
        } else {
            int y=top+(compact?42:48),step=compact?22:26;
            addAction("Talk / Topics",actionsX,y,actionWidth,()->{topics=true;rebuildWidgets();focusMessage();});
            addAction("Follow",actionsX+actionWidth+8,y,actionWidth,()->act(Action.FOLLOW,""));
            addAction("Stay",actionsX,y+step,actionWidth,()->act(Action.STAY,""));
            addAction("Set home here",actionsX+actionWidth+8,y+step,actionWidth,()->act(Action.HOME,""));
            addAction("Live at home",actionsX,y+step*2,actionWidth,()->act(Action.RESIDENT,""));
            addAction("Tasks / Wood",actionsX+actionWidth+8,y+step*2,actionWidth,()->{work=true;rebuildWidgets();});
            addAction("Inventory / Gear",actionsX,y+step*3,actionWidth,()->act(Action.INVENTORY,""));
            addAction("Unload items",actionsX+actionWidth+8,y+step*3,actionWidth,()->act(Action.UNLOAD,""));
            addAction("Appearance",actionsX,y+step*4,actionWidth,()->{appearance=true;rebuildWidgets();});
            autonomy=addRenderableWidget(Button.builder(autonomyLabel(),b->act(status.endsWith("Autonomy: on")?Action.AUTONOMY_OFF:Action.AUTONOMY_ON,""))
                .bounds(actionsX+actionWidth+8,y+step*4,actionWidth,20)
                .tooltip(Tooltip.create(Component.literal("Periodic planning. Configured cloud providers may incur charges. Stay turns this off."))).build());
        }
        message=addRenderableWidget(new EditBox(font,left+12,top+panelHeight-34,panelWidth-82,20,Component.literal("Message to Socrates")));
        message.setMaxLength(1000);message.setHint(Component.literal("Say something to Socrates..."));
        message.setValue(draft);
        send=addRenderableWidget(Button.builder(Component.literal("Send"),b->sendTalk()).bounds(left+panelWidth-64,top+panelHeight-34,52,20).build());
    }

    private void addAction(String label,int x,int y,int w,Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());
    }
    private void act(Action action,String text) {InteractionNetwork.request(companionId(),action,text);}
    private void say(String text) {message.setValue(text);sendTalk();}
    private void sendTalk() {
        String text=message.getValue().trim();if(!nearby()||text.isEmpty())return;
        conversation.add(Component.literal("You: "+text));trimConversation();scroll=0;
        act(Action.TALK,text);message.setValue("");focusMessage();
    }
    private void focusMessage() {pendingMessageFocus=true;setFocused(message);}
    @Override public boolean mouseClicked(double x,double y,int button) {
        boolean handled=super.mouseClicked(x,y,button);
        // ContainerEventHandler focuses the clicked button after onPress returns.
        // Restore text focus after that dispatch so the first word is not lost.
        if(pendingMessageFocus) {setFocused(message);pendingMessageFocus=false;}
        return handled;
    }
    public void receive(String text,String status) {
        if(!status.isEmpty())this.status=status;
        if(!text.isEmpty()) {
            conversation.add(Component.literal(text.startsWith("Socrates:")?text:"Socrates: "+text));trimConversation();scroll=0;
        }
        if(autonomy!=null)autonomy.setMessage(autonomyLabel());
    }
    private Component autonomyLabel() {return Component.literal(status.endsWith("Autonomy: on")?"Autonomy: on":"Autonomy: off");}
    private void trimConversation() {while(conversation.size()>30)conversation.remove(0);}
    private void openSkinFolder() {
        try {
            var directory=FMLPaths.CONFIGDIR.get().resolve("socrates/skins");
            java.nio.file.Files.createDirectories(directory);
            net.minecraft.Util.getPlatform().openFile(directory.toFile());
        } catch(Exception ignored) {receive("Could not open the skin folder. Use config/socrates/skins in this test instance.",status);}
    }
    @Override public void tick() {
        if(pendingMessageFocus) {setFocused(message);pendingMessageFocus=false;}
        message.tick();if(quantity!=null&&work)quantity.tick();if(skin!=null&&appearance)skin.tick();
        for(var child:children())if(child instanceof AbstractWidget widget&&widget!=close)widget.active=nearby();
        send.active=nearby()&&!message.getValue().isBlank();
        if(nearby()&&++refreshTicks>=20) {refreshTicks=0;act(Action.REFRESH,"");}
    }
    private boolean nearby() {
        return minecraft.player!=null&&minecraft.player.isAlive()&&!minecraft.player.isSpectator()&&companion.isAlive()&&!companion.isRemoved()
            &&minecraft.player.level()==companion.level()&&minecraft.player.distanceToSqr(companion)<=36;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(key==257||key==335) {
            if(work&&quantity!=null&&quantity.isFocused()) {if(nearby())act(Action.HARVEST,quantity.getValue());return true;}
            if(appearance&&skin!=null&&skin.isFocused()) {
                if(nearby())act(Action.SKIN,skin.getValue().trim());return true;
            }
            if(message.isFocused()) {sendTalk();return true;}
        }
        return super.keyPressed(key,scan,modifiers);
    }
    private List<FormattedCharSequence> lines() {
        List<FormattedCharSequence> lines=new ArrayList<>();
        for(var entry:conversation)lines.addAll(font.split(entry,panelWidth-32));
        return lines;
    }
    @Override public boolean mouseScrolled(double x,double y,double amount) {
        int visible=Math.max(1,(top+panelHeight-42-conversationY)/font.lineHeight);
        scroll=Math.max(0,Math.min(Math.max(0,lines().size()-visible),scroll+(amount>0?3:-3)));
        return true;
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) {
        renderBackground(graphics);
        graphics.fill(left-2,top-2,left+panelWidth+2,top+panelHeight+2,0xff977348);
        graphics.fill(left,top,left+panelWidth,top+panelHeight,0xff202a30);
        graphics.drawString(font,title,left+12,top+14,0xffffd28a,false);
        graphics.drawString(font,Component.literal(nearby()?"Your companion":"Move closer to interact"),left+12,top+32,0xffb9c9ce,false);
        int portraitBottom=top+(compact?148:180);
        graphics.fill(left+10,top+48,left+100,portraitBottom,0xff151e24);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,left+55,portraitBottom-8,compact?36:48,
            left+55-mouseX,top+90-mouseY,companion);
        if(appearance) {
            graphics.drawString(font,Component.literal("Username or local:filename.png"),left+110,top+49,0xffdce6e7,false);
            if(!compact)graphics.drawWordWrap(font,Component.literal("PNG: 64x64 or 64x32, up to 1 MiB. Put it in the skin folder, then apply its local: name."),left+110,top+145,panelWidth-124,0xffbac9ce);
        }
        graphics.drawString(font,font.plainSubstrByWidth(status,panelWidth-24),left+12,statusY,0xffa9bec4,false);
        int startY=conversationY,bottom=top+panelHeight-42;
        graphics.fill(left+10,startY-3,left+panelWidth-10,bottom,0xff131b20);
        var lines=lines();int visible=Math.max(1,(bottom-startY)/font.lineHeight);
        int end=Math.max(0,lines.size()-scroll),start=Math.max(0,end-visible);
        for(int index=start;index<end;index++)graphics.drawString(font,lines.get(index),left+16,startY+(index-start)*font.lineHeight,0xffe2e8e9,false);
        super.render(graphics,mouseX,mouseY,partialTick);
    }
}
