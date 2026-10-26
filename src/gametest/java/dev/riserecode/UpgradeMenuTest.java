package dev.riserecode;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.ui.menu.MenuTheme;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

/** Native screens, navigation and throttling; no replacement input widgets. */
final class UpgradeMenuTest {
    private static void check(boolean result,String label) { if(!result) { throw new AssertionError(label); } }
    static void run(ClientGameTestContext c) {
        var themes=Client.INSTANCE.getThemeManager();
        PolishIntegrationTest.menus(c);
        c.runOnClient(mc->{var tracker=mc.getFramerateLimitTracker();tracker.onInputReceived();tracker.setFramerateLimit(144);
            mc.options.enableVsync().set(true);check(tracker.getFramerateLimit()==Integer.MAX_VALUE,"Themed title removes software cap");
            ((BooleanSetting)themes.getSetting("Uncapped Themed Menus")).setValue(false);check(tracker.getFramerateLimit()==60,"Menu limit restored when disabled");
            ((BooleanSetting)themes.getSetting("Uncapped Themed Menus")).setValue(true);check(mc.options.enableVsync().get(),"Uncapping preserves VSync");
            ((dev.riserecode.testmixin.FrameLimitAccessor)tracker).riserecode$latestInput(net.minecraft.util.Util.getMillis()-70_000);
            mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.AFK);check(tracker.getFramerateLimit()==30,"AFK throttle preserved");tracker.onInputReceived();
            mc.options.enableVsync().set(false);tracker.setFramerateLimit(260);
        });
        c.runOnClient(mc->mc.gui.setScreen(new net.minecraft.client.gui.screens.DisconnectedScreen(new TitleScreen(),Component.literal("Disconnected"),Component.literal("Disposable connection test"))));
        c.waitTicks(6);c.takeScreenshot("upgrade-disconnected");check(c.computeOnClient(mc->MenuTheme.enabled()),"Disconnected themed");
        c.runOnClient(mc->mc.gui.screen().children().stream().filter(child->child instanceof net.minecraft.client.gui.components.Button).map(child->(net.minecraft.client.gui.components.Button)child).findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(257,0,0)));
        check(c.computeOnClient(mc->mc.gui.screen() instanceof TitleScreen),"Native disconnect back action");
        c.runOnClient(mc->{try {var constructor=net.minecraft.client.gui.screens.ConnectScreen.class.getDeclaredConstructor(Screen.class,Component.class);constructor.setAccessible(true);mc.gui.setScreen(constructor.newInstance(new TitleScreen(),Component.literal("Connecting")));}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
        c.waitTicks(6);c.takeScreenshot("upgrade-connecting");check(c.computeOnClient(mc->MenuTheme.enabled()),"Connecting themed");
        c.runOnClient(mc->mc.gui.screen().children().stream().filter(child->child instanceof net.minecraft.client.gui.components.Button).map(child->(net.minecraft.client.gui.components.Button)child).findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(257,0,0)));
        check(c.computeOnClient(mc->mc.gui.screen() instanceof TitleScreen),"Native connect cancel action");
        c.setScreen(()->new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(new TitleScreen()));c.waitTicks(8);c.takeScreenshot("upgrade-world-selection");
        check(c.computeOnClient(mc->MenuTheme.enabled() && MenuTheme.family(mc.gui.screen()).equals("Loading")),"World selection uses Loading preference");c.getInput().pressKey(256);c.waitTick();
        check(c.computeOnClient(mc->mc.gui.screen() instanceof TitleScreen),"Native world-selection back action");
        for(String name:new String[]{"net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen","net.irisshaders.iris.gui.screen.ShaderPackScreen","com.terraformersmc.modmenu.gui.ModsScreen"}) {
            String mod=name.contains("sodium")?"sodium":name.contains("iris")?"iris":"modmenu";if(!FabricLoader.getInstance().isModLoaded(mod)){continue;}
            c.runOnClient(mc->{try {Class<?> type=Class.forName(name);Screen parent=new TitleScreen();Screen screen=(Screen)(mod.equals("sodium")?type.getMethod("createScreen",Screen.class).invoke(null,parent):type.getConstructor(Screen.class).newInstance(parent));mc.gui.setScreen(screen);}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
            c.waitTicks(10);c.takeScreenshot("upgrade-options-"+mod);check(c.computeOnClient(mc->MenuTheme.enabled()),"Optional menu adapter "+mod);
            c.getInput().pressKey(256);c.waitTicks(2);check(c.computeOnClient(mc->mc.gui.screen() instanceof TitleScreen),"Native optional menu back action "+mod);
        }
        themes.reset();Client.INSTANCE.getLogger().info("Upgrade native-menu and FPS checks passed");
    }
}
