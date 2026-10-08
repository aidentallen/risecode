package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Inline cycle selector: left advances, right reverses, with a short value transition. */
public final class EnumSelector<E extends Enum<E>> extends UIComponent {
    private final EnumSetting<E> setting;
    private final Runnable changed;
    private final Animation change = Animation.ui(1, 0.16, MathUtil.Easing.OUT_QUART);
    private E displayed;
    public EnumSelector(EnumSetting<E> setting, Runnable changed) {
        this.setting = setting; this.changed = changed; displayed = setting.getValue();
    }
    private String option() {
        if (displayed instanceof dev.riserecode.render.ChunkMotionTracker.Curve curve) {
            return switch (curve) { case OUT_QUART -> "Ease-Out-Quart"; case IN_OUT_CUBIC -> "Ease-In-Out-Cubic"; case OUT_ELASTIC -> "Elastic-Out"; };
        }
        if (displayed instanceof MathUtil.Easing easing) {
            return switch (easing) {
                case LINEAR -> "Linear";
                case OUT_QUART -> "Ease-Out-Quart";
                case IN_OUT_CUBIC -> "Ease-In-Out-Cubic";
                case OUT_ELASTIC -> "Elastic";
                case OUT_BOUNCE -> "Bounce";
            };
        }
        return Theme.displayName(displayed.name());
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        if (displayed != setting.getValue()) { displayed = setting.getValue(); change.reset(0); change.animateTo(1); }
        double progress = change.update(deltaSeconds);
        String label = Theme.displayName(setting.getName()) + ": ";
        String option = option();
        double scale = Math.min(0.88, (width - 3) / Math.max(1, RenderUtil.textWidth(font, label + option, false)));
        double top = y + (height - 9 * Theme.number("Font Scale", 1) * scale) / 2;
        RenderUtil.text(graphics, font, label, x + hovered * 0.7, top, scale, Theme.opacity(Theme.TEXT, opacity));
        double valueX = x + RenderUtil.textWidth(font, label, false) * scale + hovered * 0.7;
        RenderUtil.text(graphics, font, option, valueX + (1 - progress) * 3, top,
                scale, Theme.opacity(Theme.blendPalette(Theme.TEXT, Theme.accent(), hovered * 0.6), opacity * progress));
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY) || (button != 0 && button != 1)) { return false; }
        var options = setting.getOptions();
        setting.setValue(options.get(Math.floorMod(options.indexOf(setting.getValue()) + (button == 0 ? 1 : -1), options.size())));
        changed.run(); return true;
    }
}
