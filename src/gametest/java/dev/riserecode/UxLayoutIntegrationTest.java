package dev.riserecode;

import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.component.FocusedInput;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.NumericField;
import dev.riserecode.ui.component.setting.EnumSelector;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;

/** Large-type geometry and complete input journeys, separate from performance measurements. */
public final class UxLayoutIntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static void number(String name,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(name)).setValue(value);}
    public static void run(ClientGameTestContext c){
        c.runOnClient(mc->{var slider=new Slider(new DoubleSetting("Test","",1,0,10),()->{});slider.beginEditing();for(char ch:"bad".toCharArray()){slider.input().charTyped(new net.minecraft.client.input.CharacterEvent(ch));}FocusedInput.commit();check(slider.input().getValue().equals("bad")&&!slider.input().validation().isEmpty(),"Invalid slider draft and error survive blur");slider.input().cancel();check(slider.input().validation().isEmpty(),"Slider Escape clears stale error");var numeric=new NumericField(new DoubleSetting("X","",1,-10,10),()->{});numeric.beginEditing();for(char ch:"bad".toCharArray()){numeric.field().charTyped(new net.minecraft.client.input.CharacterEvent(ch));}FocusedInput.commit();check(numeric.field().getValue().equals("bad")&&!numeric.field().validation().isEmpty(),"Invalid coordinate draft survives blur");numeric.field().cancel();check(numeric.field().validation().isEmpty(),"Coordinate cancel restores matching validation state");Client.INSTANCE.getThemeManager().reset();Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COMBAT);});
        c.waitTicks(15);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var first=gui.getActivePanel().getVisibleButtons().getFirst();var b=first.getBounds();boolean enabled=first.getModule().isEnabled(),expanded=first.isExpanded();gui.getActivePanel().mouseClicked(b.x()+20,b.y()+12,0);check(first.isExpanded()!=expanded&&first.getModule().isEnabled()==enabled,"Card body inspects without enabling module");if(!first.isExpanded()){gui.getActivePanel().mouseClicked(b.centerX(),b.y()+12,1);}number("Font Scale",2);});
        c.waitTicks(25);c.takeScreenshot("ux3-module-font2");
        c.runOnClient(mc->{var first=Client.INSTANCE.getClickGui().getActivePanel().getVisibleButtons().getFirst();var side=Client.INSTANCE.getClickGui().getSidebar();check(Math.abs(side.getSelectionBounds().y()-side.getItemBounds(side.getSelected()).y())<.1,"Selection pill aligns with font-aware navigation row");double end=0;for(var control:first.getSettingControls()){if(!control.setting().isVisible()){continue;}var b=control.component().getBounds();check(b.y()>=end-.01,"Visible setting rows do not overlap at font2");end=b.y()+b.height();if(control.component() instanceof EnumSelector<?>){check(b.height()>=40.6-.01,"Enum row reserves both font2 lines plus spacing");}}number("Window Width",360);number("Window Height",240);number("Content Padding",20);dev.riserecode.ui.theme.Theme.setGerman(true);});
        c.waitTicks(20);c.takeScreenshot("ux3-module-360x240-font2-german");
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();dev.riserecode.ui.theme.Theme.setGerman(false);});c.waitTicks(25);
        String target=c.computeOnClient(mc->Client.INSTANCE.getClickGui().getActivePanel().getVisibleButtons().getFirst().getSettingControls().reversed().stream().filter(control->control.setting().isVisible()&&control.component() instanceof Slider).findFirst().orElseThrow().setting().getName());
        boolean reached=false;
        for(int i=0;i<250;i++){
            c.getInput().pressKey(258);c.waitTicks(2);
            if(c.computeOnClient(mc->Client.INSTANCE.getClickGui().focusedControlLabel().startsWith(target+","))){reached=true;break;}
        }
        check(reached,"Tab reaches a deep module setting without manual scrolling: "+target);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();check(gui.getActivePanel().getScrollOffset()>0,"Logical focus automatically scrolls module viewport");});c.takeScreenshot("ux3-keyboard-deep-module");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.CLIENT_SOUNDS);gui.getSearchField().setValue("volume");gui.keyPressed(new KeyEvent(70,0,2));check(gui.getPage()==ClickGui.Page.CLIENT_SOUNDS,"Ctrl F stays in client settings scope");});
        c.waitTicks(15);c.takeScreenshot("ux3-sounds-search");
        c.runOnClient(mc->{var choice=new dev.riserecode.ui.menu.ChoiceScreen<>(Client.INSTANCE.getClickGui(),"Choice size fixture",java.util.List.of(dev.riserecode.util.MathUtil.Easing.values()),dev.riserecode.util.MathUtil.Easing.LINEAR,value->{});mc.gui.setScreen(choice);choice.init(320,180);check(!dev.riserecode.ui.menu.MenuTheme.gearSupported(choice),"Transient choice has no nested appearance gear");for(var child:choice.children()){if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.visible){check(widget.getY()>=0&&widget.getY()+widget.getHeight()<=180,"Choice controls fit 320x180 screen fixture");}}});
        c.waitTick();c.takeScreenshot("ux3-choice-320x180-fixture");
        c.runOnClient(mc->{var point=new dev.riserecode.map.WaypointStore.Waypoint(java.util.UUID.randomUUID(),"Draft",0,64,0,"minecraft:overworld","",0xFF64D8C4,true,false,false);var editor=new dev.riserecode.map.WaypointEditScreen(Client.INSTANCE.getClickGui(),Client.INSTANCE.getMaps(),point);mc.gui.setScreen(editor);editor.init(320,180);for(var child:editor.children()){if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget&&widget.visible){check(widget.getY()>=0&&widget.getY()+widget.getHeight()<=180,"Waypoint controls fit 320x180 screen fixture");}}});
        c.waitTick();c.takeScreenshot("ux3-waypoint-320x180-fixture");
        c.runOnClient(mc->{mc.gui.setScreen(new dev.riserecode.ui.component.setting.BlockPickerScreen(Client.INSTANCE.getClickGui(),new dev.riserecode.setting.BlockListSetting("Blocks","",java.util.List.of()),()->{}));});c.waitTicks(4);
        c.runOnClient(mc->mc.gui.screen().keyPressed(new KeyEvent(267,0,0)));c.waitTicks(2);c.takeScreenshot("ux3-blocks-next-page");
        c.runOnClient(mc->{mc.gui.screen().onClose();Client.INSTANCE.openHudEditor();});c.waitTicks(8);
        c.getInput().pressKey(258);c.waitTicks(2);c.getInput().pressKey(257);c.waitTicks(2);
        double oldX=c.computeOnClient(mc->{var hud=(dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen();check(hud.getSelected()!=null,"HUD element selected with Tab and Enter");return hud.getSelected().layout().x();});
        c.runOnClient(mc->{var hud=(dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen();check(hud.nudge(new KeyEvent(262,0,2)),"HUD keyboard nudge handled");check(hud.getSelected().layout().x()>oldX,"HUD keyboard nudge changes position");});c.takeScreenshot("ux3-hud-keyboard-edit");
        c.runOnClient(mc->{mc.gui.screen().onClose();Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});
        Client.INSTANCE.getLogger().info("UX layout and keyboard checks passed: {}",checks);
    }
}
