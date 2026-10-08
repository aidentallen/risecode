package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Palette cycle control. Preserves the setting's existing alpha channel. */
public final class ColorSwatch extends UIComponent {
    private static final int[] PALETTE = {Theme.BLUE, Theme.FUCHSIA, 0xFF22D3EE, 0xFF34D399, 0xFFFBBF24, Theme.TEXT};
    private final ColorSetting setting;
    private final Runnable changed;
    private final Animation pulse = Animation.ui(1, 0.18, MathUtil.Easing.OUT_QUART);
    private int previous;

    public ColorSwatch(ColorSetting setting, Runnable changed) { this.setting = setting; this.changed = changed; previous = setting.getValue(); }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        if (previous != setting.getValue()) { previous = setting.getValue(); pulse.reset(0); pulse.animateTo(1); }
        double progress = pulse.update(deltaSeconds);
        String label = Theme.displayName(setting.getName());
        RenderUtil.text(graphics, font, label, x + hovered * 0.7, y + (height - 9) / 2, 0.88,
                Theme.opacity(Theme.TEXT, opacity));
        double left = x + RenderUtil.textWidth(font, label, false) * 0.88 + 6;
        double size = 6 - (1 - progress) * 1.5;
        RenderUtil.drawRoundedRect(graphics, left, y + (height - size) / 2, size, size, 2,
                Theme.opacity(setting.getValue() | 0xFF000000, opacity));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        int index = -1;
        for (int i = 0; i < PALETTE.length; i++) {
            if ((PALETTE[i] & 0xFFFFFF) == (setting.getValue() & 0xFFFFFF)) { index = i; break; }
        }
        setting.setValue((setting.getValue() & 0xFF000000) | (PALETTE[(index + 1) % PALETTE.length] & 0xFFFFFF));
        changed.run();
        return true;
    }
}
