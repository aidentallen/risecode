package dev.riserecode.ui.component;

import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.setting.ColorSwatch;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Owns presentation and input adapters, never the module's feature logic. */
public final class ModuleButton extends UIComponent {
    public static final double HEADER_HEIGHT = 34;
    public record SettingControl(Setting<?> setting, UIComponent component, double height) { }

    private final Module module;
    private final ModuleManager manager;
    private final List<SettingControl> controls = new ArrayList<>();
    private final Animation enabled;
    private final Animation expansion = new Animation(0, 0.22, MathUtil.Easing.OUT_QUART);
    private boolean expanded;
    private UIComponent captured;

    public ModuleButton(Module module, ModuleManager manager, Runnable changed) {
        this.module = module;
        this.manager = manager;
        enabled = new Animation(module.isEnabled() ? 1 : 0, 0.18, MathUtil.Easing.IN_OUT_CUBIC);
        for (Setting<?> setting : module.getSettings()) {
            UIComponent component;
            double controlHeight;
            if (setting instanceof BooleanSetting bool) {
                component = new ToggleSwitch(bool, changed); controlHeight = 28;
            } else if (setting instanceof DoubleSetting number) {
                component = new Slider(number, changed); controlHeight = 36;
            } else if (setting instanceof EnumSetting<?> option) {
                component = enumControl(option, changed); controlHeight = 42;
            } else if (setting instanceof ColorSetting color) {
                component = new ColorSwatch(color, changed); controlHeight = 28;
            } else {
                throw new IllegalArgumentException("No UI adapter for " + setting.getClass().getName());
            }
            controls.add(new SettingControl(setting, component, controlHeight));
        }
    }

    private static <E extends Enum<E>> UIComponent enumControl(EnumSetting<E> setting, Runnable changed) {
        return new EnumSelector<>(setting, changed);
    }

    public Module getModule() { return module; }
    public boolean isExpanded() { return expanded; }
    public List<SettingControl> getSettingControls() { return List.copyOf(controls); }

    public void update(double deltaSeconds) {
        expansion.animateTo(expanded ? 1 : 0);
        expansion.update(deltaSeconds);
    }

    private double settingsHeight() {
        double result = 8;
        for (SettingControl control : controls) {
            if (control.setting().isVisible()) { result += control.height() + 6; }
        }
        return result;
    }

    public double getPreferredHeight() {
        return HEADER_HEIGHT + settingsHeight() * expansion.getValue();
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        hover.animateTo(mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT ? 1 : 0);
        double hovered = hover.update(deltaSeconds);
        enabled.animateTo(module.isEnabled() ? 1 : 0);
        double active = enabled.update(deltaSeconds);
        int background = Theme.lerp(Theme.SURFACE, Theme.HOVER, hovered);
        background = Theme.lerp(background, 0xD02A3653, active * 0.65);
        RenderUtil.drawRoundedRect(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(background, opacity));
        RenderUtil.drawRoundedBorder(graphics, x, y, width, height, Theme.BUTTON_RADIUS,
                Theme.opacity(Theme.BORDER, opacity));
        RenderUtil.drawText(graphics, font, RenderUtil.ellipsize(font, module.getName(), width - 48),
                x + 10 + hovered * 2, y + 11, Theme.opacity(Theme.lerp(Theme.TEXT, Theme.BLUE, active), opacity),
                false, false);
        RenderUtil.drawRoundedRect(graphics, x + width - 32, y + 14, 6, 6, 3,
                Theme.opacity(Theme.lerp(Theme.MUTED, Theme.BLUE, active), opacity));
        if (!controls.isEmpty()) {
            RenderUtil.drawText(graphics, font, expanded ? "−" : "+", x + width - 19, y + 10,
                    Theme.opacity(Theme.SECONDARY, opacity), false, false);
        }
        double bodyHeight = height - HEADER_HEIGHT;
        if (bodyHeight <= 0.5) { return; }
        RenderUtil.scissorPush(graphics, x + 2, y + HEADER_HEIGHT, width - 4, Math.max(0, bodyHeight - 2));
        try {
            double controlY = y + HEADER_HEIGHT + 4;
            for (SettingControl control : controls) {
                if (!control.setting().isVisible()) { continue; }
                control.component().setBounds(x + 10, controlY, width - 20, control.height());
                control.component().render(graphics, font, mouseX, mouseY, deltaSeconds,
                        opacity * expansion.getValue());
                controlY += control.height() + 6;
            }
        } finally {
            RenderUtil.scissorPop(graphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) { return false; }
        if (mouseY < y + HEADER_HEIGHT) {
            if ((button == 1 || (button == 0 && mouseX >= x + width - 22)) && !controls.isEmpty()) {
                expanded = !expanded;
                if (!expanded && captured != null) {
                    captured.mouseReleased(mouseX, mouseY, 0); captured = null;
                }
            } else if (button == 0) {
                manager.toggle(module);
            }
            return true;
        }
        if (!expanded) { return true; }
        for (SettingControl control : controls) {
            if (control.setting().isVisible() && control.component().mouseClicked(mouseX, mouseY, button)) {
                captured = control.component(); return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        return captured != null && captured.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(mouseX, mouseY, button);
        captured = null;
        return consumed;
    }
}
