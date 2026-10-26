package dev.riserecode;

import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.*;
import dev.riserecode.util.RenderUtil;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Runs unchanged against the installed 0.24.2 baseline and the candidate production JAR. */
public final class Release025FontReference {
    private static final class Probe extends Screen {
        Probe(){super(Component.literal("Zero-boldness reference"));}
        @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float dt){g.fill(0,0,width,height,0xFF18212C);}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float dt){
            for(int i=0;i<3;i++)RenderUtil.text(g,font,"RiseRecode · ABC xyz Áß ΩЖ 中 😀",20,20+i*35,.75+i*.5,0xFFF5F5F5);
        }
    }
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();Client.INSTANCE.getThemeManager().reset();ThemeManager.assign(Client.INSTANCE.getThemeManager().getSetting("Reduced Motion"),true);});
        for(var face:new ThemeManager.Typeface[]{ThemeManager.Typeface.GOOGLE_SANS,ThemeManager.Typeface.QRAFTYS}){
            c.runOnClient(mc->{ThemeManager.assign(Client.INSTANCE.getThemeManager().getSetting("Font Selection"),face);mc.gui.setScreen(new Probe());});c.waitTicks(8);c.takeScreenshot("025-reference-"+face);
        }
        c.runOnClient(mc->{mc.gui.setScreen(null);Client.INSTANCE.getThemeManager().reset();});
    }
}
