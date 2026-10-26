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
    private final double step;
    private final dev.riserecode.ui.component.TextField input = new dev.riserecode.ui.component.TextField(() -> "Value");
    private double original;
    private String cachedValue = "";
    private double cachedNumber = Double.NaN;
    public dev.riserecode.ui.component.TextField input() { return input; }
    public void beginEditing() {
        if (!input.isFocused()) { original = setting.getValue(); input.setValue(Double.toString(original)); }
        input.setFocused(true); input.selectAll();
    }
    private void preview(String text) {
        try {
            if (!text.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) { throw new IllegalArgumentException(); }
            double number = Double.parseDouble(text);
            if (!Double.isFinite(number) || number < setting.getMin() || number > setting.getMax()) { throw new IllegalArgumentException(); }
            setting.setValue(number); changed.run(); input.validation("");
        } catch (IllegalArgumentException invalid) { input.validation(setting.getMin() + " to " + setting.getMax()); }
    }

    public Slider(DoubleSetting setting, Runnable changed) {
        this(setting, changed, 0);
    }
    public Slider(DoubleSetting setting, Runnable changed, double step) {
        this.setting = setting; this.changed = changed; this.step = step;
        input.onKey(event -> {
            if (event.key() != 265 && event.key() != 264) { return false; }
            double increment = (step > 0 ? step : setting.keyboardIncrement()) * (event.hasShiftDown() ? 10 : 1);
            setting.setValue(Math.clamp(setting.getValue() + (event.key()==265?increment:-increment),setting.getMin(),setting.getMax()));
            input.setValue(Double.toString(setting.getValue())); input.validation(""); changed.run(); return true;
        });
        input.onChange(this::preview);
        input.onCancel(() -> { setting.setValue(original); changed.run(); });
        input.onCommit(() -> input.setValue(Double.toString(setting.getValue())));
        position = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.DRAGGING, normalized(), 0.1, MathUtil.Easing.OUT_QUART);
    }

    private double normalized() {
        return setting.position(setting.getValue());
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        position.animateTo(normalized());
        double progress = MathUtil.clamp01(position.update(deltaSeconds));
        if (cachedNumber != setting.getValue()) { cachedNumber = setting.getValue(); cachedValue = String.format(Locale.ROOT, step == 1 ? "%.0f" : "%.2f", cachedNumber); }
        String value = cachedValue;
        String label = Theme.displayName(setting.getName()) + ": " + value;
        double labelScale = Math.min(0.88, (width - 4) / Math.max(1, RenderUtil.textWidth(font, label, false)));
        RenderUtil.text(graphics, font, label,
                x + hovered * 0.7 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER), y + 1, labelScale, Theme.opacity(Theme.TEXT, opacity));
        input.setBounds(x, y, width, Math.max(12, height - 9));
        if (input.isFocused()) {
            RenderUtil.drawRoundedRect(graphics, x, y, width, Math.max(12, height - 9), 2, Theme.opacity(Theme.SURFACE, opacity));
            input.render(graphics, font, mouseX, mouseY, deltaSeconds, opacity);
        }
        double trackX = x + 5;
        double trackWidth = width - 10;
        double trackY = y + height - 6;
        RenderUtil.drawRoundedRect(graphics, trackX, trackY, trackWidth, 1.5, 0.75,
                Theme.opacity(Theme.MUTED, opacity * 0.45));
        if (progress > 0.001) {
            RenderUtil.drawRoundedRect(graphics, trackX, trackY, trackWidth * progress, 1.5, 0.75,
                    Theme.opacity(Theme.accent(), opacity * 0.8));
        }
        double thumb = trackX + trackWidth * progress;
        double size = 4 + hovered + (dragging ? 0.5 : 0);
        RenderUtil.drawGlow(graphics, thumb - size / 2, trackY - size / 2 + 0.75, size, size, size / 2,
                Theme.opacity(Theme.accent(), opacity * (0.6 + hovered * 0.4)));
        RenderUtil.drawRoundedRect(graphics, thumb - size / 2, trackY - size / 2 + 0.75, size, size, size / 2,
                Theme.opacity(Theme.TEXT, opacity));
    }

    private void setFromMouse(double mouseX) {
        double progress = MathUtil.clamp01((mouseX - x - 5) / Math.max(1, width - 10));
        double value = setting.valueAt(progress);
        if (step > 0) { value = setting.getMin() + Math.rint((value - setting.getMin()) / step) * step; }
        setting.setValue(Math.max(setting.getMin(), Math.min(setting.getMax(), value)));
        changed.run();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        if (mouseY < y + 12) { beginEditing(); return true; }
        dev.riserecode.ui.component.FocusedInput.commit();
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
