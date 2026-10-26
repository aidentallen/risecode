package dev.riserecode.ui.panorama;

import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** Native buttons retain narration, tab navigation and accessible text labels. */
public final class PanoramaControl extends Button {
    public enum Action { BREATH, ATMOSPHERE, MUTE, RESET }
    private final Action action;
    public PanoramaControl(Action action,int x,int width){
        super(x,10,width,20,Component.literal(label(action)),button->activate(action),DEFAULT_NARRATION);this.action=action;
        setTooltip(Tooltip.create(Component.literal(switch(action){case BREATH->"Take a breath · F1 / Escape to return";case ATMOSPHERE->"Switch atmosphere · three second blend";case MUTE->"Mute environmental audio";case RESET->"Reset view";})));
        visible=active=false;
    }
    public boolean returnControl(){return action==Action.BREATH;}
    private static String label(Action a){return switch(a){case BREATH->"Take a breath";case ATMOSPHERE->"Misty Dawn";case MUTE->"Mute";case RESET->"Reset view";};}
    private static void activate(Action a){switch(a){case BREATH->LivingPanorama.toggleScenic();case ATMOSPHERE->LivingPanorama.preferences().next();case MUTE->LivingPanorama.preferences().mute.setValue(!LivingPanorama.preferences().mute.getValue());case RESET->LivingPanorama.resetView();}Theme.manager().changed();}
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(action==Action.ATMOSPHERE)setMessage(Component.literal(LivingPanorama.preferences().label()));
        if(action==Action.MUTE)setMessage(Component.literal(LivingPanorama.preferences().mute.getValue()?"Unmute":"Mute"));
        if(action!=Action.BREATH){MenuTheme.button(g,this);return;}
        setMessage(Component.literal(LivingPanorama.scenic()?"Return to menu (F1)":"Take a breath (F1)"));
        double alpha=LivingPanorama.scenic()?.22:1;
        RenderUtil.drawRoundedRect(g,getX(),getY(),getWidth(),getHeight(),7,Theme.opacity(0xC0102824,alpha));
        int color=Theme.opacity(0xFFD1E8CB,alpha);double x=getX()+7,y=getY()+5;
        RenderUtil.line(g,x+1,y+10,x+9,y+1,1.2,color);RenderUtil.line(g,x+1,y+10,x-1,y+5,1.2,color);RenderUtil.line(g,x-1,y+5,x+3,y+1,1.2,color);RenderUtil.line(g,x+3,y+1,x+9,y+1,1.2,color);RenderUtil.line(g,x+9,y+1,x+9,y+6,1.2,color);RenderUtil.line(g,x+9,y+6,x+1,y+10,1.2,color);
    }
}
