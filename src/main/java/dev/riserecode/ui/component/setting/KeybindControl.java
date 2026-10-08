package dev.riserecode.ui.component.setting;

import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.module.Module;
import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared binding editor for every module; key capture is centralized before Screen shortcuts. */
public final class KeybindControl extends UIComponent {
    public static final double HEIGHT = 54;
    private final Module module;
    private final KeybindManager keys;
    private final ActionButton binding, reset, toggle, hold;
    private final List<ActionButton> buttons;
    public KeybindControl(Module module, KeybindManager keys) {
        this.module = module; this.keys = keys;
        binding = new ActionButton(() -> keys.isCapturing(module) ? "Press a key..." : KeybindManager.keyName(module.getKeybind()),
                () -> { if (keys.isCapturing(module)) { keys.cancelCapture(); } else { keys.beginCapture(module); } });
        reset = new ActionButton("Reset", () -> keys.reset(module));
        toggle = new ActionButton("Toggle", () -> { keys.cancelCapture(); keys.setMode(module, Module.BindMode.TOGGLE); });
        hold = new ActionButton("Hold", () -> { keys.cancelCapture(); keys.setMode(module, Module.BindMode.HOLD); });
        buttons = List.of(binding, reset, toggle, hold);
    }
    public ActionButton getBindingButton() { return binding; }
    public ActionButton getResetButton() { return reset; }
    public ActionButton getToggleButton() { return toggle; }
    public ActionButton getHoldButton() { return hold; }
    public void cancelCapture() { if (keys.isCapturing(module)) { keys.cancelCapture(); } }
    @Override public void render(GuiGraphicsExtractor g, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        binding.setBounds(x + 51, y + 3, Math.max(25, width - 104), 20);
        reset.setBounds(x + width - 48, y + 3, 48, 20);
        toggle.setBounds(x + width - 113, y + 28, 54, 20); hold.setBounds(x + width - 54, y + 28, 54, 20);
        RenderUtil.text(g, font, "Keybind", x, y + 8, 0.83, Theme.opacity(Theme.TEXT, opacity));
        RenderUtil.text(g, font, "Activation", x, y + 34, 0.83, Theme.opacity(Theme.SECONDARY, opacity));
        for (ActionButton button : buttons) { button.render(g, font, mouseX, mouseY, seconds, opacity); }
        ActionButton selected = module.getBindMode() == Module.BindMode.HOLD ? hold : toggle;
        var bounds = selected.getBounds();
        RenderUtil.drawRoundedBorder(g, bounds.x(), bounds.y(), bounds.width(), bounds.height(), 4, Theme.opacity(Theme.accent(), opacity * 0.8), 0.8);
        if (keys.isCapturing(module)) {
            var b = binding.getBounds(); RenderUtil.drawRoundedBorder(g, b.x(), b.y(), b.width(), b.height(), 4, Theme.opacity(Theme.accent(), opacity), 0.8);
            if (!keys.getCaptureNotice().isEmpty()) { RenderUtil.text(g, font, "GUI key reserved", x + 53, y + 24, 0.6, Theme.opacity(Theme.secondaryAccent(), opacity)); }
        }
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return contains(mouseX, mouseY) && buttons.stream().anyMatch(child -> child.mouseClicked(mouseX, mouseY, button));
    }
}
