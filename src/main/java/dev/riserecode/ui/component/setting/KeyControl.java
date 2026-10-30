package dev.riserecode.ui.component.setting;

import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.setting.KeySetting;
import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

/** One hotkey row: click the key, press the new one. Backspace clears it, Escape cancels. */
public final class KeyControl extends UIComponent {
    public static final double HEIGHT = 26;
    private static KeySetting capturing;
    private final KeySetting setting;
    private final ActionButton binding, reset;
    public KeyControl(KeySetting setting, Runnable changed) {
        this.setting = setting;
        binding = new ActionButton(() -> capturing == setting ? "Press a key..." : setting.isUnbound() ? "NONE" : KeybindManager.keyName(setting.getValue()),
                () -> capturing = capturing == setting ? null : setting);
        reset = new ActionButton("Reset", () -> { capturing = null; setting.reset(); changed.run(); });
    }
    /** Called by the menu before its own shortcuts; returns true when the key was consumed by a capture. */
    public static boolean capture(KeyEvent event, Runnable changed) {
        if (capturing == null) { return false; }
        var target = capturing;
        capturing = null;
        if (event.key() != 256) { target.setValue(event.key() == 259 || event.key() == 261 ? -1 : event.key()); changed.run(); }
        return true;
    }
    public static void cancelCapture() { capturing = null; }
    @Override public void render(GuiGraphicsExtractor g, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        binding.setBounds(x + width - 150, y + 3, 98, 20);
        reset.setBounds(x + width - 48, y + 3, 48, 20);
        RenderUtil.text(g, font, setting.getName().replace(" Key", ""), x, y + 8, 0.83, Theme.opacity(Theme.TEXT, opacity));
        binding.render(g, font, mouseX, mouseY, seconds, opacity);
        reset.render(g, font, mouseX, mouseY, seconds, opacity);
        if (capturing == setting) {
            var b = binding.getBounds();
            RenderUtil.drawRoundedBorder(g, b.x(), b.y(), b.width(), b.height(), 4, Theme.opacity(Theme.accent(), opacity), 0.8);
        }
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return contains(mouseX, mouseY) && (binding.mouseClicked(mouseX, mouseY, button) || reset.mouseClicked(mouseX, mouseY, button));
    }
}
