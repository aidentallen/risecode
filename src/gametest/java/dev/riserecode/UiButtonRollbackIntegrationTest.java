package dev.riserecode;

import dev.riserecode.map.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.*;
import dev.riserecode.ui.component.setting.*;
import dev.riserecode.ui.menu.PageGear;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;

/** Restored pre-review button surface, exercised through native mouse/key input. */
public final class UiButtonRollbackIntegrationTest {
    private static int checks;
    private enum Sample { FIRST, SECOND, THIRD }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void key(ClientGameTestContext c,int code){c.getInput().pressKey(code);c.waitTicks(3);}
    private static void click(ClientGameTestContext c,UIComponent.Bounds b,int button){
        double[] xy=c.computeOnClient(mc->{double scale=Client.INSTANCE.getClickGui().getCanvasScale()*mc.getWindow().getGuiScale();return new double[]{b.centerX()*scale,b.centerY()*scale};});
        c.getInput().setCursorPos(xy[0],xy[1]);c.waitTick();c.getInput().pressMouse(button);c.waitTicks(4);
    }
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.getInput().resizeWindow(1280,960);var gui=Client.INSTANCE.getClickGui();
        c.runOnClient(mc->{mc.options.guiScale().set(2);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(60);mc.options.pauseOnLostFocus=false;Client.INSTANCE.getThemeManager().reset();((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);gui.open(null);gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setValue("FullBright");gui.getSearchField().setFocused(false);});c.waitTicks(5);
        var module=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.FullBrightModule.class).orElseThrow();c.runOnClient(mc->{module.setEnabled(false);gui.getModuleButton("FullBright").setExpanded(false);});c.waitTicks(3);
        var card=gui.getModuleButton("FullBright");var b=card.getBounds();var header=new UIComponent.Bounds(b.x()+4,b.y()+3,b.width()-8,30);
        click(c,header,0);c.runOnClient(mc->check(module.isEnabled(),"Original left-click enables module"));click(c,header,0);c.runOnClient(mc->check(!module.isEnabled(),"Second left-click disables module"));click(c,header,1);c.runOnClient(mc->{check(card.isExpanded()&&!module.isEnabled(),"Original right-click opens settings without toggling");check(gui.accessibleControlLabels().stream().noneMatch(s->s.startsWith("Pages,")||s.startsWith("Client messages,")||s.startsWith("FullBright, help")||s.startsWith("FullBright, toggle")),"Added root and split module button targets are absent");});c.takeScreenshot("buttons-restored-module");
        c.runOnClient(mc->{gui.selectPage(ClickGui.Page.COSMETICS);gui.getCosmeticsEditor().discard();});c.waitTicks(5);c.runOnClient(mc->{var e=gui.getCosmeticsEditor();check(e.toolbar().size()==4&&e.buttons().size()==4,"Original four cosmetic toolbar and four footer actions remain");check(gui.accessibleControlLabels().stream().noneMatch(s->s.startsWith("Import a cape,")||s.startsWith("Preview Solid,")||s.startsWith("Show catalog,")),"Added cosmetic modal/empty-state buttons are absent");});c.takeScreenshot("buttons-restored-cosmetics");
        var choice=new EnumSetting<>("Sample","",Sample.FIRST);var picker=new EnumSelector<>(choice,()->{});
        c.runOnClient(mc->mc.gui.setScreen(new Screen(Component.literal("Original cycling control")){
            @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float dt){super.extractRenderState(g,x,y,dt);picker.setBounds(20,30,220,45);picker.render(g,font,x,y,dt,1);}
            @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent e,boolean twice){return picker.mouseClicked(e.x(),e.y(),e.button());}
        }));c.waitTicks(3);var same=c.computeOnClient(mc->mc.gui.screen());c.getInput().setCursorPos(120,90);c.getInput().pressMouse(0);c.waitTicks(3);c.runOnClient(mc->{check(choice.getValue()==Sample.SECOND,"Left click cycles to next option");check(mc.gui.screen()==same,"Cycling opens no added choice-menu buttons");});
        var maps=Client.INSTANCE.getMaps();var parent=c.computeOnClient(mc->new TitleScreen());var point=new WaypointStore.Waypoint(UUID.randomUUID(),"Draft",1,64,2,"minecraft:overworld","",0xFF64D8C4,true,false,false);
        c.runOnClient(mc->mc.gui.setScreen(new WaypointEditScreen(parent,maps,point)));c.waitTicks(4);c.runOnClient(mc->{var widgets=mc.gui.screen().children();var buttons=widgets.stream().filter(Button.class::isInstance).map(Button.class::cast).filter(v->!(v instanceof PageGear)).toList();check(buttons.size()==2&&buttons.stream().map(v->v.getMessage().getString()).toList().containsAll(List.of("Save","Cancel")),"Waypoint form restores only Save and Cancel");check(widgets.stream().filter(EditBox.class::isInstance).count()==7,"All original editable waypoint fields remain");});c.takeScreenshot("buttons-restored-waypoint");key(c,256);c.runOnClient(mc->check(mc.gui.screen()==parent,"Waypoint Escape returns without committing"));
        c.runOnClient(mc->mc.gui.setScreen(new BlockPickerScreen(parent,new BlockListSetting("Blocks","",List.of()),()->{})));c.waitTicks(3);c.runOnClient(mc->check(mc.gui.screen().children().stream().noneMatch(w->w instanceof Button&&!(w instanceof PageGear)),"Block picker has no added pagination or filter buttons"));key(c,256);c.runOnClient(mc->check(mc.gui.screen()==parent,"Original block picker Escape returns"));
        c.runOnClient(mc->{gui.open(parent);Client.INSTANCE.openHudEditor();});c.waitTicks(4);c.runOnClient(mc->{var nav=KeyboardNavigation.auxiliary(mc.gui.screen());check(nav.labels().stream().noneMatch(s->s.startsWith("Done, button")||s.startsWith("Help, button")),"HUD added Done/Help actions are absent");});key(c,256);
        c.runOnClient(mc->{gui.open(parent);gui.selectPage(ClickGui.Page.RESOURCES);gui.getResourcesEditor().getToolsButton().activate();});c.waitTicks(4);c.runOnClient(mc->check(gui.accessibleControlLabels().stream().noneMatch(v->v.startsWith("Vanilla Tweaks, button")||v.contains(". Tools: Cache all or retry")),"Resources has original maintenance actions without added provider/status buttons"));
        c.runOnClient(mc->{Client.INSTANCE.getProfileManager().saveProfile("rollback");Client.INSTANCE.getThemeManager().saveProfile("rollback");Client.INSTANCE.getThemeManager().saveProfile("rollback");gui.selectPage(ClickGui.Page.PROFILES);});c.waitTicks(4);c.runOnClient(mc->check(gui.accessibleControlLabels().stream().noneMatch(v->v.startsWith("Restore previous")||v.startsWith("Load restored")),"Profiles has original save/load/delete actions without added recovery buttons"));
        c.runOnClient(mc->{Client.INSTANCE.getFriendManager().add("Alex");gui.selectPage(ClickGui.Page.FRIENDS);});c.waitTicks(4);c.runOnClient(mc->{check(gui.accessibleControlLabels().stream().anyMatch(v->v.startsWith("Alex  ×")),"Original combined friend/remove action is restored");check(gui.accessibleControlLabels().stream().noneMatch(v->v.startsWith("Remove Alex")),"Separate added friend Remove button is absent");});
        UxAuxiliaryIntegrationTest.focus(c,"Alex  ×");key(c,257);c.runOnClient(mc->check(mc.gui.screen()==gui,"Friend removal opens no added confirmation dialog"));
        c.runOnClient(mc->mc.gui.setScreen(new dev.riserecode.ui.menu.PageAppearanceScreen(gui,dev.riserecode.ui.menu.MenuTheme.pageId(gui))));c.waitTicks(4);var page=c.computeOnClient(mc->mc.gui.screen());c.runOnClient(mc->check(KeyboardNavigation.auxiliary(page).labels().stream().noneMatch(v->v.startsWith("Page status,")),"Added page-status action is absent"));UxAuxiliaryIntegrationTest.focus(c,"Reset this page");key(c,257);c.runOnClient(mc->check(mc.gui.screen()==page,"Original page reset opens no added confirmation buttons"));key(c,256);
        c.runOnClient(mc->mc.gui.setScreen(new GlassEditorScreen(Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.GlassModule.class).orElseThrow(),gui)));c.waitTicks(5);c.runOnClient(mc->check(KeyboardNavigation.auxiliary(mc.gui.screen()).labels().stream().noneMatch(v->v.startsWith("Reset Rotation, button")||v.startsWith("Reset Tilt, button")||v.startsWith("Reset Zoom, button")||v.startsWith("Preview, button")),"Glass keeps original controls without added preview/reset buttons"));c.takeScreenshot("buttons-restored-glass");key(c,256);
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});Client.INSTANCE.getLogger().info("Original UI button rollback checks passed: {}",checks);
    }
}
