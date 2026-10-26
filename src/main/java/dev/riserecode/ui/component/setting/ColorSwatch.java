package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Palette cycle control. Preserves the setting's existing alpha channel. */
public final class ColorSwatch extends UIComponent {
    private static final int[] PALETTE = {Theme.BLUE, Theme.FUCHSIA, 0xFF22D3EE, 0xFF34D399, 0xFFFBBF24, Theme.TEXT};
    private final ColorSetting setting;
    private final Runnable changed;

    public ColorSwatch(ColorSetting setting, Runnable changed) { this.setting = setting; this.changed = changed; }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, setting.getName(), width - 30),
                x + hovered * 2, y + 9, Theme.opacity(Theme.SECONDARY, opacity), false, false);
        RenderUtil.drawRoundedRect(graphics, x + width - 22, y + 5, 20, 20, 6,
                Theme.opacity(setting.getValue() | 0xFF000000, opacity));
        RenderUtil.drawRoundedBorder(graphics, x + width - 22, y + 5, 20, 20, 6, Theme.opacity(Theme.BORDER, opacity));
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
