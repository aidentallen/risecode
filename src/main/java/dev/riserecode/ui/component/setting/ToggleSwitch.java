package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ToggleSwitch extends UIComponent {
    private final String label;
    private final BooleanSupplier value;
    private final Consumer<Boolean> setter;
    private final Animation slide;

    public ToggleSwitch(BooleanSetting setting, Runnable changed) {
        this(setting.getName(), setting::getValue, enabled -> { setting.setValue(enabled); changed.run(); });
    }

    public ToggleSwitch(String label, BooleanSupplier value, Consumer<Boolean> setter) {
        this.label = label; this.value = value; this.setter = setter;
        slide = new Animation(value.getAsBoolean() ? 1 : 0, 0.18, MathUtil.Easing.IN_OUT_CUBIC);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        slide.animateTo(value.getAsBoolean() ? 1 : 0);
        double progress = slide.update(deltaSeconds);
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, label, width - 40),
                x + hovered * 2, y + height / 2 - 5, Theme.opacity(Theme.TEXT, opacity), false, false);
        double left = x + width - 30;
        double top = y + height / 2 - 7;
        RenderUtil.drawRoundedRect(graphics, left, top, 30, 14, 7,
                Theme.opacity(Theme.lerp(Theme.HOVER, Theme.BLUE, progress), opacity));
        RenderUtil.drawRoundedBorder(graphics, left, top, 30, 14, 7, Theme.opacity(Theme.BORDER, opacity));
        RenderUtil.drawRoundedRect(graphics, left + 2 + 16 * progress, top + 2, 10, 10, 5,
                Theme.opacity(Theme.TEXT, opacity));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        setter.accept(!value.getAsBoolean());
        return true;
    }
}
