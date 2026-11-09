package dev.riserecode;

import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Actual native window resizing, deliberately separate from Screen.init geometry fixtures. */
public final class UxViewportIntegrationTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition){throw new AssertionError(message);}}
    private static void number(String name,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(name)).setValue(value);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        int[] original=c.computeOnClient(mc->new int[]{mc.getWindow().getScreenWidth(),mc.getWindow().getScreenHeight(),mc.options.guiScale().get()});
        c.getInput().resizeWindow(960,720);
        c.runOnClient(mc->{mc.options.guiScale().set(3);Client.INSTANCE.getThemeManager().reset();number("Window Width",360);number("Window Height",240);number("Content Padding",20);number("Font Scale",2);Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.APPEARANCE);Client.INSTANCE.getClickGui().getSearchField().setValue("Reduced Motion");});c.waitTicks(20);
        c.runOnClient(mc->{check(mc.getWindow().getScreenWidth()==960&&mc.getWindow().getScreenHeight()==720,"Actual native window is 960x720");check(mc.gui.screen().width==320&&mc.gui.screen().height==240,"GUI scale 3 produces actual 320x240 viewport");var gui=Client.INSTANCE.getClickGui();var b=gui.getDisplayedWindowBounds();check(b.width()*gui.getCanvasScale()<=mc.gui.screen().width&&b.height()*gui.getCanvasScale()<=mc.gui.screen().height,"Large-type client fits actual minimum viewport");});c.takeScreenshot("ux4-native-960x720-scale3-font2");
        c.runOnClient(mc->mc.gui.setScreen(new dev.riserecode.ui.menu.ChoiceScreen<>(Client.INSTANCE.getClickGui(),"Actual small-window choices",java.util.List.of(dev.riserecode.util.MathUtil.Easing.values()),dev.riserecode.util.MathUtil.Easing.LINEAR,value->{})));c.waitTicks(15);
        c.runOnClient(mc->{for(var child:mc.gui.screen().children()){if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.visible){check(widget.getY()+widget.getHeight()<=240,"Choices remain reachable in actual small native viewport");}}});c.takeScreenshot("ux4-native-small-choice-font2");
        c.runOnClient(mc->{mc.gui.screen().onClose();Client.INSTANCE.openHudEditor();});c.waitTicks(8);
        c.getInput().pressKey(258);c.waitTicks(2);c.getInput().pressKey(257);c.waitTicks(2);
        c.runOnClient(mc->((dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen()).getSettingsSection().setExpanded(true));c.waitTicks(20);
        UxAuxiliaryIntegrationTest.focus(c,"Position Y, slider,");
        c.runOnClient(mc->dev.riserecode.ui.component.KeyboardNavigation.auxiliary(mc.gui.screen()).key(new net.minecraft.client.input.KeyEvent(258,0,1)));c.waitTicks(2);
        c.runOnClient(mc->{var hud=(dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen();check(dev.riserecode.ui.component.KeyboardNavigation.auxiliary(hud).focusedLabel().startsWith("Position X"),"Backward traversal reaches previous coordinate editor");var row=hud.getControls().stream().filter(control->control instanceof dev.riserecode.ui.component.setting.Slider slider&&slider.name().equals("Position X")).findFirst().orElseThrow().getBounds();var view=hud.getContextViewportBounds();check(row.y()>=view.y()-.01&&row.y()+row.height()<=view.y()+view.height()+.01,"Backward text focus reveals its setting label and whole row in actual small viewport");});c.takeScreenshot("ux4-native-small-hud-backward-focus");
        c.runOnClient(mc->mc.gui.screen().onClose());c.getInput().resizeWindow(original[0],original[1]);
        c.runOnClient(mc->{mc.options.guiScale().set(original[2]);Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});c.waitTicks(8);
        Client.INSTANCE.getLogger().info("UX actual native viewport checks passed: {}",checks);
    }
}
