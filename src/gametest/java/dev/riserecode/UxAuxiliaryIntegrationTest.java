package dev.riserecode;

import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.*;
import dev.riserecode.ui.component.setting.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;

/** Auxiliary editor journeys and edit recovery, distinct from frame-time or narration proof. */
public final class UxAuxiliaryIntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static void number(String name,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(name)).setValue(value);}
    static void focus(ClientGameTestContext c,String prefix){
        for(int i=0;i<350;i++){
            c.getInput().pressKey(258);c.waitTicks(2);
            if(c.computeOnClient(mc->{var gui=mc.gui.screen();return gui instanceof ClickGui click?click.focusedControlLabel().startsWith(prefix):KeyboardNavigation.auxiliary(gui)!=null&&KeyboardNavigation.auxiliary(gui).focusedLabel().startsWith(prefix);})){return;}
        }
        throw new AssertionError("Keyboard cannot reach "+prefix);
    }
    private static void invalid(Slider slider){slider.beginEditing();slider.input().charTyped(new CharacterEvent('b'));FocusedInput.commit();}
    private static void cLabelProbe(dev.riserecode.ui.hud.HudEditorScreen hud){
        var slider=hud.getControls().stream().filter(control->control instanceof Slider value&&value.name().equals("Position X")).map(Slider.class::cast).findFirst().orElseThrow();
        double before=slider.value();var b=slider.getBounds();hud.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(b.x()+5,b.y()+SettingMetrics.labelHeight()-.5,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);check(slider.input().isFocused()&&slider.value()==before,"Entire font2 label enters editing without a track change");hud.charTyped(new CharacterEvent('b'));check(!slider.input().validation().isEmpty(),"HUD routes typed characters to its numeric editor");FocusedInput.cancel();
    }
    public static void run(ClientGameTestContext c){
        c.runOnClient(mc->{var value=new DoubleSetting("Recovery speed","Degrees per second",2,0,10);var slider=new Slider(value,()->{});slider.setBounds(0,0,200,SettingMetrics.height(slider,0));invalid(slider);check(!slider.input().validation().isEmpty(),"Invalid slider fixture starts with error");slider.mouseClicked(180,slider.getBounds().height()-5,0);slider.mouseReleased(180,slider.getBounds().height()-5,0);double replacement=value.getValue();check(slider.input().validation().isEmpty()&&Double.parseDouble(slider.input().getValue())==replacement,"Track replacement synchronizes model draft and error");slider.input().cancel();check(value.getValue()==replacement,"Cancel after track replacement keeps the replacement baseline");invalid(slider);slider.adjust(1);check(slider.input().validation().isEmpty()&&Double.parseDouble(slider.input().getValue())==value.getValue(),"Arrow adjustment resolves invalid draft coherently");check(slider.input().accessibleName().contains("Recovery speed"),"Numeric text identity names the setting");Client.INSTANCE.getThemeManager().reset();Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.SEARCH);});
        c.waitTicks(8);
        boolean enabled=c.computeOnClient(mc->{var button=Client.INSTANCE.getClickGui().getModuleButton("KillAura");button.setExpanded(false);return button.getModule().isEnabled();});
        c.runOnClient(mc->Client.INSTANCE.getClickGui().getSearchField().setValue("kill aura rotation speed"));c.waitTicks(25);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var button=gui.getModuleButton("KillAura");check(button.isExpanded()&&button.searchMatchName().equals("RotationSpeed"),"Combined module and setting terms identify Rotation Speed");check(button.getModule().isEnabled()==enabled,"Search inspection preserves gameplay state");var bounds=button.settledSearchMatch();var panel=gui.getActivePanel().getBounds();check(bounds!=null&&bounds.y()>=panel.y()-.01&&bounds.y()+bounds.height()<=panel.y()+panel.height()+.01,"Combined search reveals the actual matching control");});c.takeScreenshot("ux4-combined-setting-search");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().getSearchField().setValue(""));c.waitTicks(5);c.runOnClient(mc->{check(!Client.INSTANCE.getClickGui().getModuleButton("KillAura").isExpanded(),"Clearing search restores prior disclosure state");Client.INSTANCE.openHudEditor();});c.waitTicks(10);
        c.getInput().pressKey(258);c.waitTicks(2);c.getInput().pressKey(257);c.waitTicks(2);
        c.runOnClient(mc->{var hud=(dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen();check(hud.getSelected()!=null,"HUD selected by actual keyboard input");hud.nudge(new KeyEvent(262,0,3));hud.nudge(new KeyEvent(264,0,3));var x=hud.getControls().stream().filter(control->control instanceof Slider slider&&slider.name().equals("Position X")).map(Slider.class::cast).findFirst().orElseThrow();var y=hud.getControls().stream().filter(control->control instanceof Slider slider&&slider.name().equals("Position Y")).map(Slider.class::cast).findFirst().orElseThrow();check(x.value()==hud.getSelected().layout().x()&&y.value()==hud.getSelected().layout().y(),"Both HUD position models follow coarse nudges");double previous=x.value();x.adjust(1);check(Math.abs(hud.getSelected().layout().x()-previous-.001)<1e-8,"Position slider continues from the nudged coordinate");number("Font Scale",2);hud.getSettingsSection().setExpanded(false);number("Font Scale",2);});
        c.waitTicks(20);c.takeScreenshot("ux4-hud-collapsed-font2");
        c.runOnClient(mc->{check(KeyboardNavigation.auxiliary(mc.gui.screen()).labels().stream().noneMatch(label->label.startsWith("Position X")),"Collapsed HUD context exposes no body controls");((dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen()).getSettingsSection().setExpanded(true);});c.waitTicks(25);
        c.runOnClient(mc->cLabelProbe((dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen()));
        focus(c,"BackgroundColor, color,");c.waitTicks(2);c.takeScreenshot("ux4-hud-deep-style-font2");
        c.runOnClient(mc->{var hud=(dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen();var header=hud.getSettingsSection().getBounds();check(hud.mouseScrolled(header.x()+header.width()-2,header.y()+header.height()+5,0,-1),"HUD wheel scrolling works across the dynamic context width");for(var control:hud.getControls()){check(control.getBounds().height()+.01>=SettingMetrics.height(control,28),"HUD controls use shared font-aware geometry");}hud.onClose();var glass=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.GlassModule.class).orElseThrow();mc.gui.setScreen(new GlassEditorScreen(glass,Client.INSTANCE.getClickGui()));});c.waitTicks(25);
        focus(c,"Shared shimmer speed, slider,");c.waitTicks(2);c.takeScreenshot("ux4-glass-deep-style-font2");
        c.runOnClient(mc->{var glass=(GlassEditorScreen)mc.gui.screen();for(var control:glass.getControls()){if(control.getBounds().width()>0){check(control.getBounds().height()+.01>=SettingMetrics.height(control,23),"Glass rows reserve shared geometry");}}glass.onClose();Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COSMETICS);});c.waitTicks(20);
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();var def=Client.INSTANCE.getCosmetics().library().definitions().stream().filter(d->Client.INSTANCE.getCosmetics().library().available(d)).findFirst().orElseThrow();editor.select(def.id());var edit=editor.buttons().stream().filter(b->b.label().equals("Edit")).findFirst().orElseThrow();var bounds=edit.getBounds();edit.mouseClicked(bounds.centerX(),bounds.centerY(),0);editor.getItemSection().setExpanded(true);});c.waitTicks(25);
        focus(c,"Offset Z, slider,");c.waitTicks(2);c.takeScreenshot("ux4-cosmetic-deep-style-font2");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();check(editor.isDirty(),"Cosmetic item is an unapplied draft");var b=editor.poseButton().getBounds();editor.poseButton().mouseClicked(b.centerX(),b.centerY(),0);check(mc.gui.screen() instanceof dev.riserecode.ui.menu.ChoiceScreen,"Pose offers an inspectable list");check(editor.isDirty(),"Opening cosmetic choices retains parent draft");});c.waitTicks(10);c.takeScreenshot("ux4-cosmetic-pose-choices");
        c.runOnClient(mc->{mc.gui.screen().onClose();check(Client.INSTANCE.getClickGui().getCosmeticsEditor().isDirty(),"Canceling cosmetic choices retains draft");Client.INSTANCE.getClickGui().getCosmeticsEditor().discard();Client.INSTANCE.getThemeManager().reset();var blocks=new dev.riserecode.setting.BlockListSetting("Blocks","",java.util.List.of("minecraft:stone","missing:test"));mc.gui.setScreen(new BlockPickerScreen(Client.INSTANCE.getClickGui(),blocks,()->{}));});c.waitTicks(5);
        c.runOnClient(mc->{var screen=mc.gui.screen();var selected=screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals("Selected")).map(net.minecraft.client.gui.components.Button.class::cast).findFirst().orElseThrow();selected.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(selected.getX()+selected.getWidth()/2.0,selected.getY()+selected.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);});
        c.waitTicks(5);c.takeScreenshot("ux4-blocks-selected-only");
        c.runOnClient(mc->{mc.gui.screen().onClose();Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});
        Client.INSTANCE.getLogger().info("UX auxiliary editor checks passed: {}",checks);
    }
}
