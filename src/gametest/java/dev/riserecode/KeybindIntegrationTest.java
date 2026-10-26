package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.FreeLookModule;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ModuleButton;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.KeybindControl;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/** Real key capture and independent hold/toggle lifecycles through the Minecraft input bridge. */
public final class KeybindIntegrationTest {
    private static int checks;
    private KeybindIntegrationTest() { }
    public static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        var client = Client.INSTANCE; var modules = client.getModuleManager(); var keys = client.getKeybindManager();
        var bright = modules.getByClass(FullBrightModule.class).orElseThrow();
        var look = modules.getByClass(FreeLookModule.class).orElseThrow();
        context.runOnClient(mc -> { mc.gui.setScreen(null); modules.disableAll(); mc.options.setCameraType(CameraType.FIRST_PERSON); keys.reset(bright); keys.setMode(bright, Module.BindMode.TOGGLE); });
        world.getServer().runCommand("gamemode creative @a"); context.waitTicks(8);
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> gui().isOpen(), 240);
        context.runOnClient(mc -> {
            for (Category category : Category.values()) {
                gui().selectCategory(category);
                check(gui().getActivePanel().getModuleButtons().stream().allMatch(button -> button.getKeybindControl() != null), "bind editor present in " + category);
            }
            gui().selectPage(ClickGui.Page.SEARCH); gui().getSearchField().setValue("FullBright");
        }); context.waitTicks(8);
        ModuleButton button = context.computeOnClient(mc -> gui().getModuleButton("FullBright"));
        if (!context.computeOnClient(mc -> button.isExpanded())) { click(context, button::getBounds, 1, true); }
        context.waitTicks(12);
        KeybindControl control = button.getKeybindControl();
        reveal(context, control); click(context, () -> control.getBindingButton().getBounds());
        check(keys.isCapturing(bright), "click begins exclusive keyboard capture");
        context.takeScreenshot("keybind-listening");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitTicks(3);
        check(keys.isCapturing(bright) && context.computeOnClient(mc -> mc.gui.screen() == gui()), "GUI key rejected without closing bind editor");
        context.getInput().pressKey(InputConstants.KEY_ESCAPE); context.waitTicks(3);
        check(!keys.isCapturing() && context.computeOnClient(mc -> mc.gui.screen() == gui()), "Escape cancels binding without closing GUI");
        click(context, () -> control.getBindingButton().getBounds()); context.getInput().pressKey(InputConstants.KEY_F12); context.waitTicks(3);
        check(bright.getKeybind() == InputConstants.KEY_F12 && !keys.isCapturing() && !bright.isEnabled(), "captured key assigns without toggling module in GUI");
        click(context, () -> control.getHoldButton().getBounds()); check(bright.getBindMode() == Module.BindMode.HOLD, "Hold button changes activation mode immediately");
        context.takeScreenshot("keybind-hold-mode");
        context.runOnClient(mc -> {
            check(client.getProfileManager().saveProfile("KeybindTest"), "binding profile saves");
            keys.bind(bright, "F7"); keys.setMode(bright, Module.BindMode.TOGGLE);
            check(client.getProfileManager().loadProfile("KeybindTest"), "binding profile loads while GUI is open");
        }); context.waitTicks(5);
        check(bright.getKeybind() == InputConstants.KEY_F12 && bright.getBindMode() == Module.BindMode.HOLD, "profile restores key and Hold mode together");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> mc.gui.screen() == null, 240);
        context.getInput().holdKey(InputConstants.KEY_F12); context.waitTicks(4);
        check(bright.isEnabled() && !keys.persistentEnabled(bright), "native key hold activates temporary module");
        context.runOnClient(mc -> check(!client.getConfigManager().profileSnapshot().getAsJsonObject("modules").getAsJsonObject("FullBright").get("enabled").getAsBoolean(), "saving during hold excludes temporary enabled state"));
        context.getInput().releaseKey(InputConstants.KEY_F12); context.waitTicks(3); check(!bright.isEnabled(), "release deactivates module and restores lifecycle state");
        context.getInput().holdKey(InputConstants.KEY_F12); context.waitTicks(3); context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> gui().isOpen(), 240);
        check(!bright.isEnabled(), "opening GUI releases held modules before editing"); context.getInput().releaseKey(InputConstants.KEY_F12);
        reveal(context, control); click(context, () -> control.getToggleButton().getBounds()); check(bright.getBindMode() == Module.BindMode.TOGGLE, "Toggle button changes activation mode");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> mc.gui.screen() == null, 240);
        context.getInput().pressKey(InputConstants.KEY_F12); context.waitTicks(3); check(bright.isEnabled(), "toggle press survives key release");
        context.getInput().pressKey(InputConstants.KEY_F12); context.waitTicks(3); check(!bright.isEnabled(), "next press toggles module off");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> gui().isOpen(), 240);
        reveal(context, control); click(context, () -> control.getResetButton().getBounds()); check(bright.getKeybind() == Module.UNBOUND, "Reset clears keybind");
        context.takeScreenshot("keybind-reset");
        click(context, () -> control.getBindingButton().getBounds()); context.getInput().pressKey(InputConstants.KEY_F7); context.waitTicks(2);
        click(context, () -> control.getBindingButton().getBounds()); context.getInput().pressKey(InputConstants.KEY_DELETE); context.waitTicks(2);
        check(bright.getKeybind() == Module.UNBOUND && !keys.isCapturing(), "Delete is a second clear-binding shortcut");
        context.getInput().pressKey(InputConstants.KEY_RSHIFT); context.waitFor(mc -> mc.gui.screen() == null, 240);
        context.getInput().pressKey(InputConstants.KEY_F12); context.getInput().pressKey(InputConstants.KEY_F7); context.waitTicks(3);
        check(!bright.isEnabled(), "cleared key no longer activates module");
        context.runOnClient(mc -> {
            client.getCommandManager().handleChat(".bind FullBright F12 hold");
            check(bright.getKeybind() == InputConstants.KEY_F12 && bright.getBindMode() == Module.BindMode.HOLD, "bind command assigns optional activation mode");
            client.getCommandManager().handleChat(".bind FullBright NONE");
            keys.bind(look, "LEFT_ALT"); keys.setMode(look, Module.BindMode.HOLD); look.setEnabled(false);
        });
        context.getInput().holdKey(InputConstants.KEY_LALT); context.waitTicks(4);
        check(look.isEnabled() && look.isHeld() && context.computeOnClient(mc -> mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK), "FreeLook works from disabled state with shared Hold binding");
        context.getInput().releaseKey(InputConstants.KEY_LALT); context.waitTicks(4);
        check(!look.isEnabled() && !look.isHeld() && context.computeOnClient(mc -> mc.options.getCameraType() == CameraType.FIRST_PERSON), "FreeLook release restores camera and module state");
        context.runOnClient(mc -> { keys.bind(look, "F8"); keys.setMode(look, Module.BindMode.TOGGLE); });
        context.getInput().pressKey(InputConstants.KEY_F8); context.waitTicks(4); check(look.isEnabled() && look.isHeld(), "FreeLook supports toggle activation too");
        context.getInput().pressKey(InputConstants.KEY_F8); context.waitTicks(4); check(!look.isEnabled() && !look.isHeld(), "FreeLook toggle off restores camera");
        context.runOnClient(mc -> { keys.reset(look); look.setEnabled(true); });
        // Unbound hold mode must not fall back to the removed hidden Left Alt setting.
        context.runOnClient(mc -> keys.setMode(look, Module.BindMode.HOLD));
        context.getInput().holdKey(InputConstants.KEY_LALT); context.waitTicks(4); check(!look.isHeld(), "reset FreeLook has no hidden fallback key"); context.getInput().releaseKey(InputConstants.KEY_LALT);
        context.runOnClient(mc -> { modules.disableAll(); keys.bind(look, "LEFT_ALT"); keys.setMode(look, Module.BindMode.HOLD); keys.reset(bright); keys.setMode(bright, Module.BindMode.TOGGLE); });
        client.getLogger().info("Module keybind native OpenGL checks passed ({} assertions)", checks);
    }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static void reveal(ClientGameTestContext context, KeybindControl control) {
        context.runOnClient(mc -> {
            var panel = gui().getActivePanel(); var b = control.getBounds();
            panel.mouseScrolled(panel.getBounds().centerX(), panel.getBounds().centerY(), (panel.getBounds().centerY() - b.centerY()) / 24);
        }); context.waitTicks(12);
    }
    private static void click(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds) { click(context, bounds, 0, false); }
    private static void click(ClientGameTestContext context, Supplier<UIComponent.Bounds> bounds, int button, boolean header) {
        double[] p = context.computeOnClient(mc -> { var b = bounds.get(); return new double[] {b.centerX() * gui().getCanvasScale() * mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth(),
                (header ? b.y() + 12 : b.centerY()) * gui().getCanvasScale() * mc.getWindow().getScreenHeight() / mc.getWindow().getGuiScaledHeight()}; });
        context.getInput().setCursorPos(p[0], p[1]); context.waitTick(); context.getInput().pressMouse(button); context.waitTicks(3);
    }
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
}
