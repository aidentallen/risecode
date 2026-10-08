package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** All seven palettes are visible before applying one. */
public final class PresetGallery extends UIComponent {
    private final ThemeManager manager;
    public PresetGallery(ThemeManager manager) { this.manager = manager; }
    @Override public void render(GuiGraphicsExtractor g, Font font, double mx, double my, double dt, double opacity) {
        var presets = ThemeManager.Preset.values(); double w = (width - 6) / 2;
        for (int i = 0; i < presets.length; i++) {
            double left = x + (i % 2) * (w + 6), top = y + (i / 2) * 24; int[] colors = ThemeManager.palette(presets[i]);
            RenderUtil.drawRoundedRect(g, left, top, w, 21, 3, Theme.opacity(colors[2], opacity));
            RenderUtil.drawGradientRect(g, left + 3, top + 3, 8, 15, Theme.opacity(colors[0], opacity), Theme.opacity(colors[1], opacity));
            RenderUtil.text(g, font, presets[i] == ThemeManager.Preset.CLASSIC_RISECODE ? "Classic risecode" : Theme.displayName(presets[i].name()), left + 15, top + 7, 0.7, Theme.opacity(Theme.TEXT, opacity));
            if (manager.choice("Appearance Preset", ThemeManager.Preset.class) == presets[i]) { RenderUtil.drawRoundedBorder(g, left, top, w, 21, 3, Theme.opacity(colors[0], opacity), 0.8); }
        }
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !contains(mx, my)) { return false; }
        double w = (width - 6) / 2; int index = (int)((my - y) / 24) * 2 + (mx - x >= w + 6 ? 1 : 0);
        if (index < ThemeManager.Preset.values().length) { manager.applyPreset(ThemeManager.Preset.values()[index]); }
        return true;
    }
}
