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

/** The reference's compact inline indicator, with interpolated color and a soft pulse. */
public final class ToggleSwitch extends UIComponent {
    private final String label;
    private final BooleanSupplier value;
    private final Consumer<Boolean> setter;
    private final Animation slide;
    public ToggleSwitch(BooleanSetting setting, Runnable changed) {
        this(setting.getName(), setting::getValue, next -> { setting.setValue(next); changed.run(); });
    }
    public ToggleSwitch(String label, BooleanSupplier value, Consumer<Boolean> setter) {
        this.label = label; this.value = value; this.setter = setter;
        slide = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.TOGGLES, value.getAsBoolean() ? 1 : 0, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        slide.animateTo(value.getAsBoolean() ? 1 : 0);
        double active = slide.update(deltaSeconds);
        String text = Theme.label(Theme.displayName(label));
        double textScale = Math.min(0.88, (width - 24) / Math.max(1, RenderUtil.textWidth(font, text, false)));
        RenderUtil.text(graphics, font, text, x + hovered * 0.7 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER), y + (height - 9 * Theme.number("Font Scale", 1) * textScale) / 2, textScale,
                Theme.opacity(Theme.blendPalette(Theme.TEXT, 0xFFFFFFFF, hovered), opacity));
        double dotX = x + RenderUtil.textWidth(font, text, false) * textScale + 6 + hovered * 0.7 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER);
        double dotY = y + height / 2 - 2;
        RenderUtil.drawGlow(graphics, dotX, dotY, 4, 4, 2, Theme.opacity(Theme.accent(), opacity * active));
        RenderUtil.drawRoundedRect(graphics, dotX, dotY, 4, 4, 2,
                Theme.opacity(Theme.blendPalette(Theme.MUTED, Theme.accent(), active), opacity));
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        dev.riserecode.ui.sound.UiSounds.play(); setter.accept(!value.getAsBoolean()); return true;
    }
}
