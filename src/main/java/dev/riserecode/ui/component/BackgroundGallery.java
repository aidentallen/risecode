package dev.riserecode.ui.component;

import dev.riserecode.ui.menu.BackgroundAssets;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Preview tiles use the exact same assets as the active menu backdrop. */
public final class BackgroundGallery extends UIComponent {
    private final ThemeManager manager;
    public BackgroundGallery(ThemeManager manager) { this.manager = manager; }
    @Override public void render(GuiGraphicsExtractor g, Font font, double mx, double my, double dt, double alpha) {
        var values = ThemeManager.Background.values(); double w = (width - 9) / 4;
        for (int i = 0; i < values.length; i++) {
            double left = x + i % 4 * (w + 3), top = y + i / 4 * 41;
            RenderUtil.drawRoundedRect(g, left, top, w, 37, 3, Theme.opacity(Theme.SURFACE, alpha));
            if(values[i]==ThemeManager.Background.NONE){RenderUtil.line(g,left+8,top+19,left+w-8,top+5,1,Theme.opacity(Theme.SECONDARY,alpha*.6));}
            else if(dev.riserecode.ui.menu.AnimatedPreview.animated(values[i])){dev.riserecode.ui.menu.AnimatedPreview.draw(g,values[i],left,top,w,24,Theme.opacity(-1,alpha),false);}
            else if (values[i] != ThemeManager.Background.SOLID && values[i] != ThemeManager.Background.GRADIENT) {
                g.pose().pushMatrix(); g.pose().translate((float)left, (float)top); g.pose().scale((float)w, 24);
                g.blit(RenderPipelines.GUI_TEXTURED, BackgroundAssets.asset(values[i]), 0, 0, 0, 0, 1, 1, 1, 1, Theme.opacity(-1, alpha)); g.pose().popMatrix();
            } else {
                var paint=values[i]==ThemeManager.Background.SOLID?Theme.paint("Background Color"):
                        new dev.riserecode.setting.ColorSetting.Paint(manager.color("Gradient Start"),manager.color("Gradient End"),dev.riserecode.setting.ColorSetting.Mode.GRADIENT,dev.riserecode.util.ColorUtil.GradientDirection.VERTICAL,0,24);
                RenderUtil.drawPaintRect(g,left,top,w,24,3,paint,alpha);
            }
            RenderUtil.text(g, font, Theme.displayName(values[i].name()), left + 3, top + 28, 0.57, Theme.opacity(Theme.TEXT, alpha));
            boolean hovered=mx>=left&&mx<left+w&&my>=top&&my<top+37;
            if(hovered){RenderUtil.drawGlow(g,left,top,w,37,3,Theme.opacity(Theme.accent(),alpha));RenderUtil.drawRoundedBorder(g,left,top,w,37,3,Theme.opacity(Theme.accent(),alpha),1);}
            if (manager.choice("Background", ThemeManager.Background.class) == values[i]) { RenderUtil.drawRoundedBorder(g,left,top,w,37,3,Theme.opacity(Theme.accent(),alpha),1); }
        }
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !contains(mx, my)) { return false; }
        int col = Math.clamp((int)((mx-x)/((width-9)/4+3)),0,3), row=(int)((my-y)/41), index=row*4+col;
        if (index < 0 || index >= ThemeManager.Background.values().length) { return false; }
        @SuppressWarnings("unchecked") var setting=(EnumSetting<ThemeManager.Background>)manager.getSetting("Background");
        setting.setValue(ThemeManager.Background.values()[index]); manager.changed(); dev.riserecode.ui.sound.UiSounds.play(); return true;
    }
}
