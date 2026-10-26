package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Slider extends UIComponent {
    private final DoubleSetting setting;
    private final Runnable changed;
    private final Animation position;
    private boolean dragging;

    public Slider(DoubleSetting setting, Runnable changed) {
        this.setting = setting; this.changed = changed;
        position = new Animation(normalized(), 0.1, MathUtil.Easing.OUT_QUART);
    }

    private double normalized() {
        double range = setting.getMax() - setting.getMin();
        return range == 0 ? 0 : (setting.getValue() - setting.getMin()) / range;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        position.animateTo(normalized());
        double progress = position.update(deltaSeconds);
        String value = String.format(Locale.ROOT, "%.2f", setting.getValue());
        int valueWidth = RenderUtil.textWidth(font, value, false);
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, setting.getName(), width - valueWidth - 10),
                x + hovered * 2, y + 2, Theme.opacity(Theme.SECONDARY, opacity), false, false);
        RenderUtil.drawText(graphics, font, value, x + width - valueWidth, y + 2,
                Theme.opacity(Theme.TEXT, opacity), false, false);
        double trackX = x + 5;
        double trackWidth = width - 10;
        double trackY = y + 23;
        RenderUtil.drawRoundedRect(graphics, trackX, trackY, trackWidth, 4, 2, Theme.opacity(Theme.HOVER, opacity));
        if (progress > 0.01) {
            RenderUtil.drawRoundedRect(graphics, trackX, trackY, Math.max(4, trackWidth * progress), 4, 2,
                    Theme.opacity(Theme.BLUE, opacity));
        }
        double thumb = trackX + trackWidth * progress;
        RenderUtil.drawDropShadow(graphics, thumb - 5, trackY - 3, 10, 10, 5,
                Theme.opacity(Theme.BLUE, opacity * (0.22 + hovered * 0.18)));
        RenderUtil.drawRoundedRect(graphics, thumb - 5, trackY - 3, 10, 10, 5, Theme.opacity(Theme.TEXT, opacity));
    }

    private void setFromMouse(double mouseX) {
        double progress = MathUtil.clamp01((mouseX - x - 5) / Math.max(1, width - 10));
        setting.setValue(Math.max(setting.getMin(), Math.min(setting.getMax(),
                MathUtil.lerp(setting.getMin(), setting.getMax(), progress))));
        changed.run();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        dragging = true;
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) { return false; }
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean consumed = dragging;
        dragging = false;
        return consumed;
    }
}
