package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.ui.clickgui.ClickGui;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;

/** Shared custom-control keyboard navigation in the actual extracted screen, with no pointer clicks. */
public final class KeyboardReviewIntegrationTest {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static void key(ClientGameTestContext context, int key) {
        context.runOnClient(mc -> mc.gui.screen().keyPressed(new KeyEvent(key, 0, 0))); context.waitTicks(2);
    }
    private static void focus(ClientGameTestContext context, String prefix) {
        for (int i = 0; i < 100; i++) {
            key(context, InputConstants.KEY_TAB);
            if (context.computeOnClient(mc -> gui().focusedControlLabel().startsWith(prefix))) { return; }
        }
        throw new AssertionError("Keyboard cannot reach " + prefix + ": " + context.computeOnClient(mc -> gui().accessibleControlLabels()));
    }
    public static void run(ClientGameTestContext context) {
        context.waitFor(mc -> Client.INSTANCE.isInitialized() && Client.INSTANCE.resourcesReady() && mc.gui.overlay() == null, 1200);
        var module = Client.INSTANCE.getModuleManager().getByClass(FullBrightModule.class).orElseThrow();
        context.runOnClient(mc -> {
            Client.INSTANCE.getModuleManager().disableAll();
            module.getMode().setValue(FullBrightModule.Mode.GAMMA); module.getBrightness().setValue(.5);
            gui().open(null); gui().selectPage(ClickGui.Page.SEARCH); gui().getSearchField().setValue("FullBright");
        });
        context.waitFor(mc -> gui().isOpen(), 240); context.waitTicks(20);
        focus(context, "FullBright, module");
        key(context, InputConstants.KEY_SPACE);
        context.runOnClient(mc -> check(module.isEnabled(), "Space enables focused module"));
        key(context, InputConstants.KEY_RIGHT); context.waitTicks(20);
        context.runOnClient(mc -> check(gui().getModuleButton("FullBright").isExpanded(), "Right expands module settings"));
        focus(context, "Brightness, slider");
        key(context, InputConstants.KEY_LEFT);
        context.runOnClient(mc -> check(module.getBrightness().getValue() < .5, "Arrow adjusts slider without pointer"));
        key(context, InputConstants.KEY_RETURN);
        context.runOnClient(mc -> check(dev.riserecode.ui.component.FocusedInput.active(), "Enter starts slider text editing"));
        key(context, InputConstants.KEY_ESCAPE);
        context.runOnClient(mc -> check(!dev.riserecode.ui.component.FocusedInput.active() && mc.gui.screen() == gui(), "Escape ends editor before closing screen"));
        focus(context, "Mode, choice");
        var mode = module.getMode().getValue();
        key(context, InputConstants.KEY_RIGHT);
        context.runOnClient(mc -> check(module.getMode().getValue() != mode, "Arrow changes enum choice"));
        context.runOnClient(mc -> { module.setEnabled(false); mc.gui.setScreen(null); });
        Client.INSTANCE.getLogger().info("Keyboard review native checks passed ({} assertions)", checks);
    }
}
