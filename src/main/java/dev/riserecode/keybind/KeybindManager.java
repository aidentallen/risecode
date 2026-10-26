package dev.riserecode.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.Client;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.KeyPressEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.clickgui.ClickGui;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;

public final class KeybindManager implements AutoCloseable {
    private final Minecraft minecraft;
    private final ModuleManager modules;
    private final Supplier<ClickGui> clickGuiSupplier;
    private final KeyMapping clickGuiKey;
    private final EventBus.Subscription subscription;

    public KeybindManager(EventBus eventBus, Minecraft minecraft, ModuleManager modules, Supplier<ClickGui> clickGuiSupplier) {
        this.minecraft = minecraft;
        this.modules = modules;
        this.clickGuiSupplier = clickGuiSupplier;
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(Client.MOD_ID, "controls"));
        clickGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.riserecode.clickgui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));
        subscription = eventBus.subscribe(KeyPressEvent.class, 100, false, this::onKey);
    }

    public KeyMapping getClickGuiKey() { return clickGuiKey; }

    private void onKey(KeyPressEvent event) {
        if (!event.isPress() || minecraft.font == null || minecraft.gui.overlay() != null) { return; }
        Screen screen = minecraft.gui.screen();
        if (clickGuiKey.matches(event.getInput())
                && (screen == null || screen instanceof ClickGui || screen instanceof TitleScreen)) {
            ClickGui clickGui = clickGuiSupplier.get();
            if (screen == clickGui) { clickGui.onClose(); }
            else { clickGui.open(screen); }
            event.cancel();
            return;
        }
        if (screen != null || minecraft.player == null) { return; }
        for (Module module : modules.getModules()) {
            if (module.getKeybind() != Module.UNBOUND && module.getKeybind() == event.getKey()) {
                modules.toggle(module);
            }
        }
    }

    @Override
    public void close() { subscription.close(); }
}
