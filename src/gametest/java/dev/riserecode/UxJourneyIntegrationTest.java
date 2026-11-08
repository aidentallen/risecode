package dev.riserecode;

import dev.riserecode.ui.clickgui.ClickGui;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;

/** Native journey regression: visible disclosure, search, narrow toolbar, keyboard and return navigation. */
public final class UxJourneyIntegrationTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition){throw new AssertionError(message);}}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();Client.INSTANCE.getClickGui().open(mc.gui.screen());Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COMBAT);});
        c.waitTicks(20);c.takeScreenshot("ux-combat");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();check(gui.accessibleControlLabels().stream().anyMatch(s->s.startsWith("Client settings")),"Settings footer reachable by keyboard");check(gui.accessibleControlLabels().stream().anyMatch(s->s.startsWith("Combat")),"Sidebar page reachable by keyboard");var b=gui.getActivePanel().getVisibleButtons().getFirst();boolean enabled=b.getModule().isEnabled();var box=b.getBounds();gui.getActivePanel().mouseClicked(box.x()+box.width()-14,box.y()+12,0);check(b.isExpanded(),"Visible plus opens settings");check(enabled==b.getModule().isEnabled(),"Inspect settings without toggling module");});
        c.waitTicks(15);c.takeScreenshot("ux-module-expanded");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getActivePanel().getVisibleButtons().getFirst();boolean enabled=b.getModule().isEnabled();var box=b.getBounds();gui.getActivePanel().mouseClicked(box.x()+box.width()-14,box.y()+30,0);check(b.getModule().isEnabled()!=enabled,"On Off text toggles state");Client.INSTANCE.getModuleManager().setEnabled(b.getModule(),enabled);});
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setValue("this module does not exist 18273");});
        c.waitTicks(5);c.runOnClient(mc->check(Client.INSTANCE.getClickGui().getActivePanel().getVisibleButtons().isEmpty(),"Search empty state"));c.takeScreenshot("ux-empty-search");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.APPEARANCE);gui.getSearchField().setValue("Reduced Motion");});c.waitTicks(15);c.takeScreenshot("ux-appearance-search");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();check(gui.getThemeEditor().getMaxScroll()<100,"Scoped settings search limits results");gui.keyPressed(new KeyEvent(70,0,2));check(gui.getPage()==ClickGui.Page.APPEARANCE,"Ctrl F stays scoped to appearance");gui.getSearchField().setValue("");gui.selectPage(ClickGui.Page.HUD);Client.INSTANCE.openHudEditor();mc.gui.screen().onClose();check(mc.gui.screen()==gui&&gui.getPage()==ClickGui.Page.HUD,"HUD editor returns to caller");gui.selectPage(ClickGui.Page.RESOURCES);((dev.riserecode.setting.DoubleSetting)Client.INSTANCE.getThemeManager().getSetting("Window Width")).setValue(360.0);((dev.riserecode.setting.DoubleSetting)Client.INSTANCE.getThemeManager().getSetting("Content Padding")).setValue(20.0);});
        c.waitTicks(20);c.takeScreenshot("ux-resources-narrow");
        c.runOnClient(mc->{var search=Client.INSTANCE.getClickGui().getResourcesEditor().search().getBounds();check(search.width()>150,"Narrow resource toolbar gives search its own row");Client.INSTANCE.getThemeManager().reset();guiDraftTest(mc);mc.gui.setScreen(null);});
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.open(null);gui.selectPage(ClickGui.Page.APPEARANCE);gui.getSearchField().setValue("Easing Curve");});c.waitTicks(15);
        var easing=c.computeOnClient(mc->Client.INSTANCE.getThemeManager().getSetting("Easing Curve").getValue());
        c.runOnClient(mc->{var control=Client.INSTANCE.getClickGui().getThemeEditor().getControl("Easing Curve");var box=control.getBounds();control.mouseClicked(box.centerX(),box.centerY(),0);check(mc.gui.screen() instanceof dev.riserecode.ui.menu.ChoiceScreen,"Enum control opens inspectable choices");});
        c.waitTicks(5);c.takeScreenshot("ux-choice-list");c.runOnClient(mc->{mc.gui.screen().onClose();check(mc.gui.screen()==Client.INSTANCE.getClickGui(),"Choice cancel returns to caller");check(easing.equals(Client.INSTANCE.getThemeManager().getSetting("Easing Curve").getValue()),"Choice cancel preserves value");mc.gui.setScreen(new dev.riserecode.map.WaypointScreen(Client.INSTANCE.getClickGui(),Client.INSTANCE.getMaps()));});
        c.waitTicks(5);c.runOnClient(mc->{var screen=mc.gui.screen();screen.keyPressed(new KeyEvent(258,0,1));screen.keyPressed(new KeyEvent(32,0,0));check(mc.gui.screen()==Client.INSTANCE.getClickGui(),"Waypoint manager Done works with keyboard only");mc.gui.setScreen(new dev.riserecode.map.WorldMapScreen(Client.INSTANCE.getClickGui(),Client.INSTANCE.getMaps()));});
        c.waitTicks(15);c.runOnClient(mc->{var screen=mc.gui.screen();screen.keyPressed(new KeyEvent(258,0,1));screen.keyPressed(new KeyEvent(32,0,0));check(mc.gui.screen()==Client.INSTANCE.getClickGui(),"World map Done works with keyboard only");Client.INSTANCE.getClickGui().getSearchField().setValue("zzzz no such appearance setting");});
        c.waitTicks(5);c.takeScreenshot("ux-appearance-no-results");
        c.runOnClient(mc->{dev.riserecode.ui.theme.Theme.setGerman(true);((dev.riserecode.setting.DoubleSetting)Client.INSTANCE.getThemeManager().getSetting("Font Scale")).setValue(2.0);Client.INSTANCE.getClickGui().getSearchField().setValue("Reduced Motion");});
        c.waitTicks(10);c.takeScreenshot("ux-german-font2");c.runOnClient(mc->{dev.riserecode.ui.theme.Theme.setGerman(false);Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});
        Client.INSTANCE.getLogger().info("UX journey checks passed: {}",checks);
    }
    private static void guiDraftTest(net.minecraft.client.Minecraft mc){
        var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.COSMETICS);
        var editor=gui.getCosmeticsEditor();var visible=editor.toolbar().stream().filter(b->b.label().startsWith("Visible:")).findFirst().orElseThrow();visible.setBounds(0,0,120,20);var bounds=visible.getBounds();visible.mouseClicked(bounds.centerX(),bounds.centerY(),0);
        check(editor.isDirty(),"Cosmetic draft differs before navigation");gui.selectPage(ClickGui.Page.SEARCH);
        check(mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen,"Leaving dirty draft asks for confirmation");
        check(editor.isDirty(),"Opening confirmation preserves draft");
        mc.gui.screen().onClose();check(mc.gui.screen()==gui&&editor.isDirty(),"Cancel confirmation keeps editing and draft");
        editor.discard();gui.selectPage(ClickGui.Page.SEARCH);
    }
}
