package dev.riserecode;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Temporal geometry checks; intentionally does not claim frame pacing or GPU performance. */
public final class UxMotionIntegrationTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition){throw new AssertionError(message);}}
    public static void run(ClientGameTestContext c){
        c.runOnClient(mc->{var themes=Client.INSTANCE.getThemeManager();themes.reset();((DoubleSetting)themes.getSetting("Opening Duration")).setValue(1.0);Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COMBAT);});
        c.waitTick();
        double early=c.computeOnClient(mc->{var gui=Client.INSTANCE.getClickGui();double shown=gui.getDisplayedWindowBounds().width();check(shown<gui.getWindowBounds().width(),"Opening starts with a bounded foreground scale");return shown;});c.takeScreenshot("ux3-motion-opening-early");
        c.waitTicks(4);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();check(gui.getDisplayedWindowBounds().width()>early,"Opening advances over time");double before=gui.getDisplayedWindowBounds().width();gui.toggleVisibility();check(Math.abs(gui.getDisplayedWindowBounds().width()-before)<.001,"Closing reversal retains rendered pose");gui.toggleVisibility();check(Math.abs(gui.getDisplayedWindowBounds().width()-before)<.001,"Reopening interrupted exit retains rendered pose");});c.takeScreenshot("ux3-motion-reversed");
        c.waitTicks(24);c.runOnClient(mc->check(Client.INSTANCE.getClickGui().isOpen(),"Interrupted opening settles and remains usable"));c.takeScreenshot("ux3-motion-settled");
        c.runOnClient(mc->{((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.PLAYER);});c.waitTicks(2);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();check(gui.getDisplayedWindowBounds().width()==gui.getWindowBounds().width(),"Reduced Motion removes decorative window displacement");check(Math.abs(gui.getSidebar().getSelectionBounds().y()-gui.getSidebar().getItemBounds(gui.getSidebar().getSelected()).y())<.001,"Reduced Motion moves navigation selection immediately");Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});
        Client.INSTANCE.getLogger().info("UX temporal motion checks passed: {}",checks);
    }
}
