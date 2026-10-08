package dev.riserecode.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.Client;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.KeyPressEvent;
import dev.riserecode.event.UpdateEvent;
import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.ui.clickgui.ClickGui;
import java.util.function.Supplier;
import java.util.Locale;
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
    private final EventBus.Subscription tickSubscription;
    private final KeybindController controller;
    private Module capturing;
    private String captureNotice = "";

    public KeybindManager(EventBus eventBus, Minecraft minecraft, ModuleManager modules, Supplier<ClickGui> clickGuiSupplier) {
        this.minecraft = minecraft;
        this.modules = modules;
        this.clickGuiSupplier = clickGuiSupplier;
        controller = new KeybindController(modules);
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(Client.MOD_ID, "controls"));
        clickGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.riserecode.clickgui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));
        subscription = eventBus.subscribe(KeyPressEvent.class, 100, true, this::onKey);
        tickSubscription = eventBus.subscribe(UpdateEvent.class, 1000, false, ignored -> {
            controller.update(inputAllowed(), key -> InputConstants.isKeyDown(minecraft.getWindow(), key));
            if (capturing != null && !(minecraft.gui.screen() instanceof ClickGui)) { cancelCapture(); }
        });
    }

    public KeyMapping getClickGuiKey() { return clickGuiKey; }

    public void bind(Module module, String name) {
        bind(module, parseKey(name));
    }
    public void bind(Module module, int key) {
        if (key != Module.UNBOUND && (key < 32 || key > 348)) { throw new IllegalArgumentException("Choose a keyboard key"); }
        if (key != Module.UNBOUND && key == KeyMappingHelper.getBoundKeyOf(clickGuiKey).getValue()) {
            throw new IllegalArgumentException("That key opens the ClickGUI; choose another key");
        }
        modules.setKeybind(module, key);
    }
    public void setMode(Module module, Module.BindMode mode) { modules.setBindMode(module, mode); }
    public void reset(Module module) { cancelCapture(); bind(module, Module.UNBOUND); }
    public void beginCapture(Module module) { capturing = module; captureNotice = ""; }
    public void cancelCapture() { capturing = null; captureNotice = ""; }
    public boolean isCapturing(Module module) { return capturing == module; }
    public boolean isCapturing() { return capturing != null; }
    public String getCaptureNotice() { return captureNotice; }
    public boolean persistentEnabled(Module module) { return controller.persistentEnabled(module); }

    public static int parseKey(String name) {
        String normalized = name.strip().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.equals("none") || normalized.equals("unbound")) { return Module.UNBOUND; }
        normalized = switch (normalized) {
            case "rshift", "rightshift", "right_shift" -> "right.shift";
            case "lshift", "leftshift", "left_shift", "shift" -> "left.shift";
            case "rctrl", "rightctrl", "right_control" -> "right.control";
            case "lctrl", "leftctrl", "left_control", "ctrl", "control" -> "left.control";
            case "ralt", "rightalt", "right_alt" -> "right.alt";
            case "lalt", "leftalt", "left_alt", "alt" -> "left.alt";
            case "esc" -> "escape";
            case "return" -> "enter";
            default -> normalized.replace('_', '.');
        };
        try {
            InputConstants.Key key = InputConstants.getKey(normalized.startsWith("key.keyboard.")
                    ? normalized : "key.keyboard." + normalized);
            if (key.getValue() < 32 || key.getValue() > 348) { throw new IllegalArgumentException("Unknown key"); }
            return key.getValue();
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown keyboard key: " + name + ". Try R, F6, RIGHT_SHIFT, or NONE");
        }
    }

    public static String keyName(int key) {
        return key == Module.UNBOUND ? "NONE" : InputConstants.Type.KEYSYM.getOrCreate(key).getName()
                .replace("key.keyboard.", "").replace('.', '_').toUpperCase(Locale.ROOT);
    }

    private void onKey(KeyPressEvent event) {
        if (event.isRelease()) { controller.releaseKey(event.getKey()); }
        if (capturing != null && minecraft.gui.screen() instanceof ClickGui) {
            event.cancel();
            if (!event.isPress()) { return; }
            if (event.getInput().isEscape()) { cancelCapture(); return; }
            if (event.getKey() == InputConstants.KEY_BACKSPACE || event.getKey() == InputConstants.KEY_DELETE) { reset(capturing); return; }
            try { bind(capturing, event.getKey()); cancelCapture(); }
            catch (IllegalArgumentException invalid) { captureNotice = invalid.getMessage(); }
            return;
        }
        if (event.isCancelled() || !event.isPress() || minecraft.font == null || minecraft.gui.overlay() != null) { return; }
        Screen screen = minecraft.gui.screen();
        if (clickGuiKey.matches(event.getInput())
                && (screen == null || screen instanceof ClickGui || screen instanceof TitleScreen)) {
            ClickGui clickGui = clickGuiSupplier.get();
            if (screen == clickGui) { clickGui.toggleVisibility(); }
            else { clickGui.open(screen); }
            event.cancel();
            return;
        }
        controller.press(event.getKey(), inputAllowed());
    }
    private boolean inputAllowed() {
        return minecraft.player != null && minecraft.player.isAlive() && minecraft.level != null
                && minecraft.gui.screen() == null && minecraft.gui.overlay() == null && minecraft.isWindowActive();
    }

    @Override
    public void close() { cancelCapture(); controller.close(); subscription.close(); tickSubscription.close(); }
}
