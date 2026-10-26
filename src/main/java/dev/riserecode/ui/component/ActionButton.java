package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ActionButton extends UIComponent {
    private final String label;
    private final Runnable action;

    public ActionButton(String label, Runnable action) { this.label = label; this.action = action; }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        RenderUtil.drawRoundedRect(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(Theme.lerp(Theme.SURFACE, Theme.HOVER, hovered), opacity));
        RenderUtil.drawRoundedBorder(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(Theme.BORDER, opacity));
        RenderUtil.drawText(graphics, font, label, x + (width - RenderUtil.textWidth(font, label, false)) / 2
                + hovered * 2, y + height / 2 - 5, Theme.opacity(Theme.TEXT, opacity), false, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        action.run(); return true;
    }
}
