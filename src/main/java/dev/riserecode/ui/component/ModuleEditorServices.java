package dev.riserecode.ui.component;

import dev.riserecode.keybind.KeybindManager;
import dev.riserecode.ui.theme.ThemeManager;

/** Explicit services passed from the UI composition root to every module card. */
public record ModuleEditorServices(ThemeManager themes, KeybindManager keys, ModuleEditorRegistry editors) {
    public static ModuleEditorServices live() {
        var client=dev.riserecode.Client.INSTANCE;
        return new ModuleEditorServices(client.getThemeManager(),client.getKeybindManager(),ModuleEditorRegistry.defaults(net.minecraft.client.Minecraft.getInstance()));
    }
}
