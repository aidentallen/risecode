package dev.riserecode;

import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.ColorUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Deterministic native screenshot fixture shared verbatim with the 0.8.0 baseline. */
public final class FidelityScreen extends Screen {
    public FidelityScreen() { super(Component.literal("Rendering fidelity")); }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float partial) { }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial) {
        g.fill(0,0,width,height,0xFF132027);
        RenderUtil.text(g,font,"risecode · Visual fidelity · Unicode Ω",40,24,1,Theme.TEXT);
        int index=0;
        for(var mode:new ColorSetting.Mode[]{ColorSetting.Mode.SOLID,ColorSetting.Mode.GRADIENT,ColorSetting.Mode.RAINBOW,ColorSetting.Mode.ASTOLFO,ColorSetting.Mode.SINE}) {
            var paint=new ColorSetting.Paint(0xDA59D5CB,0x99B487E9,mode,ColorUtil.GradientDirection.HORIZONTAL,0,100);
            int top=60+index++*55;
            RenderUtil.drawPaintRect(g,40,top,160,30,9,paint,1);
            RenderUtil.drawDropShadow(g,230,top,160,30,9,0x60000000);
            RenderUtil.drawPaintRect(g,230,top,160,30,9,paint,0.7);
            RenderUtil.drawRoundedBorder(g,230,top,160,30,9,0xFFE0EEFF,1);
            RenderUtil.paintText(g,font,mode.name()+" glow",420,top+8,1,paint,1,true);
        }
        RenderUtil.text(g,font,"Aa Bb Cc 0123456789 — glyph batching",40,380,1,0xFFE0EEFF);
    }
}
