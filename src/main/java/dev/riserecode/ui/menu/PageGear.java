package dev.riserecode.ui.menu;

import dev.riserecode.ui.component.Icon;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PageGear extends Button {
    private long renderedFrame=-1;
    public PageGear(Screen owner){super(owner.width-25,8,17,17,Component.literal("Page appearance"),button->PageAppearanceScreen.open(owner),DEFAULT_NARRATION);}
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(renderedFrame==MenuTheme.frameSerial()){return;}renderedFrame=MenuTheme.frameSerial();
        dev.riserecode.ui.theme.PageAppearance.editor(()->{RenderUtil.drawButton(g,getX(),getY(),getWidth(),getHeight(),MenuTheme.hover(this),1);Icon.GEAR.drawCentered(g,getX(),getY(),getWidth(),getHeight(),Theme.TEXT);});
    }
}
