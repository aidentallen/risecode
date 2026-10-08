package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.function.Supplier;

public final class ActionButton extends UIComponent {
    private final Animation press = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.PRESS, 0, 0.18, MathUtil.Easing.OUT_QUART);
    private final Supplier<String> label;
    private final Runnable action;

    public ActionButton(String label, Runnable action) { this(() -> label, action); }
    public ActionButton(Supplier<String> label, Runnable action) { this.label = label; this.action = action; }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        double down = press.update(deltaSeconds);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x + width / 2), (float) (y + height / 2));
        graphics.pose().scale((float) (1 - down * 0.025 * Animation.scale(dev.riserecode.ui.theme.ThemeManager.Channel.PRESS)), (float) (1 - down * 0.025 * Animation.scale(dev.riserecode.ui.theme.ThemeManager.Channel.PRESS)));
        graphics.pose().translate((float) (-x - width / 2), (float) (-y - height / 2));
        RenderUtil.drawSurfaceTransition(graphics, x, y, width, height, Theme.BUTTON_RADIUS, hovered, opacity);
        String text = Theme.label(label.get());
        double scale = Math.min(0.88, (width - 8) / Math.max(1, RenderUtil.textWidth(font, text, false)));
        RenderUtil.text(graphics, font, text, x + (width - RenderUtil.textWidth(font, text, false) * scale) / 2,
                y + (height - 9 * Theme.number("Font Scale", 1) * scale) / 2, scale,
                Theme.opacity(Theme.blendPalette(Theme.TEXT, Theme.accent(), hovered * 0.45), opacity));
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        press.reset(1); press.animateTo(0); action.run(); return true;
    }
}
