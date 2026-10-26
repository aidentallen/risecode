package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.sound.UiSounds;
import dev.riserecode.ui.theme.ThemeManager;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Timed render sequences exercise the transition itself, not just its final frame. */
public final class UiRepairIntegrationTest {
    private static int checks;
    private static final AtomicInteger activations=new AtomicInteger();
    private UiRepairIntegrationTest() { }
    private static void check(boolean value,String message) { checks++;if(!value) { throw new AssertionError(message); } }
    private static void number(String name,double value) { ((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(name)).setValue(value); }
    private static void bool(String name,boolean value) { ((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting(name)).setValue(value); }
    @SuppressWarnings("unchecked") private static void background(ThemeManager.Background value) { ((EnumSetting<ThemeManager.Background>)Client.INSTANCE.getThemeManager().getSetting("Background")).setValue(value); }
    private static final class ProbeScreen extends Screen {
        private Button action;
        private Button disabled;
        private net.minecraft.client.gui.components.events.GuiEventListener sodiumButton;
        private ProbeScreen() { super(Component.literal("Transition probe")); }
        @Override protected void init() {
            action=addRenderableWidget(Button.builder(Component.literal("Soft bounce"),ignored->activations.incrementAndGet()).bounds(width/2-80,height/2,160,24).build());
            disabled=addRenderableWidget(Button.builder(Component.literal("Disabled"),ignored->activations.incrementAndGet()).bounds(width/2-80,height/2+32,160,24).build());disabled.active=false;
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium")) {
                try {
                    var dim=Class.forName("net.caffeinemc.mods.sodium.client.util.Dim2i");
                    var bounds=dim.getConstructor(int.class,int.class,int.class,int.class).newInstance(width/2-80,height/2+64,160,24);
                    Object button=Class.forName("net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget").getConstructor(dim,Component.class,Runnable.class,boolean.class,boolean.class)
                        .newInstance(bounds,Component.literal("Sodium bounce"),(Runnable)activations::incrementAndGet,true,true);
                    sodiumButton=addRenderableWidget((net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.narration.NarratableEntry)button);
                } catch(ReflectiveOperationException error) { throw new AssertionError("Installed Sodium button adapter",error); }
            }
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
            // Older/mod menus call these from foreground: none may add another backdrop/dimming pass.
            extractTransparentBackground(g);extractBlurredBackground(g);extractMenuBackground(g);
            g.text(font,"Foreground text §aΩ 中",width/2-80,height/2-24,0xFFFFFFFF);
            super.extractRenderState(g,mx,my,delta);
        }
    }
    public static void run(ClientGameTestContext c) {
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();mc.options.pauseOnLostFocus=false;mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);number("Opening Duration",1);number("Navigation Duration",1);number("Hover Duration",1);background(ThemeManager.Background.SPACE);});
        var probe=c.computeOnClient(mc->new ProbeScreen());c.setScreen(()->probe);c.waitTick();c.takeScreenshot("161-menu-early");
        c.runOnClient(mc->{check(MenuTheme.backdropPasses()==1,"Exactly one stationary backdrop despite repeated foreground helpers");check(MenuTheme.frameProgress(probe)>0&&MenuTheme.frameProgress(probe)<1,"Foreground fade is sampled while moving");check(MenuTheme.screenScale(probe)<1,"Only foreground has an entrance scale");});
        c.waitTicks(3);c.takeScreenshot("161-menu-middle");c.waitTicks(22);c.takeScreenshot("161-menu-settled");
        c.runOnClient(mc->check(MenuTheme.screenScale(probe)==1,"Foreground settles at normal scale"));
        c.runOnClient(mc->number("Press Timing",.8));
        long sounds=c.computeOnClient(mc->UiSounds.dispatches());
        c.runOnClient(mc->{probe.action.setFocused(true);probe.action.keyPressed(new KeyEvent(InputConstants.KEY_RETURN,0,0));});
        c.waitTick();
        c.runOnClient(mc->{check(activations.get()==1&&UiSounds.dispatches()==sounds+1,"Keyboard button activation dispatches one immediate action and sound");check(MenuTheme.pressScale(probe.action)<1,"Individual keyboard-activated button compresses");check(MenuTheme.pressScale(probe.disabled)==1,"Neighboring disabled button has independent feedback");});
        c.takeScreenshot("161-button-compress");c.waitTicks(4);c.takeScreenshot("161-button-rebound");
        c.waitTicks(16);c.runOnClient(mc->check(Math.abs(MenuTheme.pressScale(probe.action)-1)<.001,"Button rebound settles"));
        double[] cursor=c.computeOnClient(mc->new double[]{probe.action.getX()+80,probe.action.getY()+12});
        c.getInput().setCursorPos(cursor[0]*2,cursor[1]*2);c.waitTick();c.getInput().pressMouse(0);c.waitTick();
        c.runOnClient(mc->check(activations.get()==2&&UiSounds.dispatches()==sounds+2,"Real mouse button activation dispatches once"));
        c.runOnClient(mc->{probe.action.keyPressed(new KeyEvent(InputConstants.KEY_SPACE,0,0));check(activations.get()==3&&UiSounds.dispatches()==sounds+3,"Space activation dispatches once");});c.waitTick();
        c.runOnClient(mc->check(MenuTheme.pressScale(probe.action)<1,"Space activation shares native spring feedback"));c.takeScreenshot("161-space-feedback");c.waitTicks(20);
        if(probe.sodiumButton!=null) {
            c.runOnClient(mc->{mc.setLastInputType(net.minecraft.client.InputType.KEYBOARD_TAB);probe.sodiumButton.setFocused(true);probe.sodiumButton.keyPressed(new KeyEvent(InputConstants.KEY_RETURN,0,0));});c.waitTick();
            c.runOnClient(mc->check(activations.get()==4&&UiSounds.dispatches()==sounds+4,"Sodium keyboard activation dispatches one action and sound"));
            c.takeScreenshot("161-sodium-button-compress");c.waitTicks(20);
        }
        for(ThemeManager.Background kind:new ThemeManager.Background[]{ThemeManager.Background.SPACE,ThemeManager.Background.GRADIENT,ThemeManager.Background.SOLID}) {
            for(double opacity:new double[]{0,.5,1}) {
                c.runOnClient(mc->{background(kind);number("Background Opacity",opacity);mc.gui.setScreen(new ProbeScreen());});c.waitTick();
                c.takeScreenshot("161-backdrop-"+kind.name().toLowerCase(java.util.Locale.ROOT)+"-"+(int)(opacity*100));
                c.runOnClient(mc->check(MenuTheme.backdropPasses()==1,"Backdrop opacity never introduces another layer"));
            }
        }
        c.runOnClient(mc->{background(ThemeManager.Background.LAKE);number("Background Opacity",1);mc.gui.setScreen(new TitleScreen());});c.waitTick();c.takeScreenshot("161-title-moving");
        c.runOnClient(mc->mc.gui.setScreen(new OptionsScreen(new TitleScreen(),mc.options,false)));c.waitTick();c.takeScreenshot("161-options-moving");c.waitTicks(24);
        c.runOnClient(mc->mc.gui.setScreen(new VideoSettingsScreen(new TitleScreen(),mc,mc.options)));c.waitTick();c.takeScreenshot("161-video-moving");c.waitTicks(24);c.takeScreenshot("161-video-settled");
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium")) {
            c.runOnClient(mc->{
                try { mc.gui.setScreen((Screen)Class.forName("net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen").getMethod("createScreen",Screen.class).invoke(null,new TitleScreen())); }
                catch(ReflectiveOperationException error) { throw new AssertionError("Installed video settings",error); }
            });c.waitTick();c.takeScreenshot("161-sodium-video-moving");c.waitTicks(24);c.takeScreenshot("161-sodium-video-settled");
            c.runOnClient(mc->check(MenuTheme.backdropPasses()==1,"Sodium settings retains one full-screen backdrop"));
        }
        c.runOnClient(mc->{Client.INSTANCE.getClickGui().open(mc.gui.screen());Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COMBAT);});c.waitTicks(25);
        c.runOnClient(mc->Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.APPEARANCE));c.waitTick();c.takeScreenshot("161-tab-moving");
        c.runOnClient(mc->{var side=Client.INSTANCE.getClickGui().getSidebar();check(side.selectionOffset()>20.75&&side.selectionOffset()<8*20.75,"Sidebar selection travels between rows");});
        c.waitTicks(24);c.takeScreenshot("161-appearance-top");
        double[] hoverCursor=c.computeOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getSidebar().getItemBounds(4);return new double[]{b.centerX()*gui.getCanvasScale()*2,b.centerY()*gui.getCanvasScale()*2};});
        c.getInput().setCursorPos(hoverCursor[0],hoverCursor[1]);c.waitTicks(2);
        c.runOnClient(mc->{double hover=Client.INSTANCE.getClickGui().getSidebar().hoverAmount(4);check(hover>0&&hover<1,"Sidebar hover fades through intermediate values");});
        c.takeScreenshot("161-hover-moving");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().getThemeEditor().reveal("Sound File"));c.waitTicks(12);
        c.runOnClient(mc->{var preview=Client.INSTANCE.getClickGui().getThemeEditor().getAction("Preview Sound");var b=preview.getBounds();long before=UiSounds.dispatches();check(preview.mouseClicked(b.centerX(),b.centerY(),0)&&UiSounds.dispatches()==before+1,"Sound preview plays once without a second click sound");});
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();check(editor.getActions().stream().noneMatch(a->a.label().equals("Save")||a.label().equals("Load")),"Appearance contains no profile toolbar");editor.reveal("Background File");});c.waitTicks(12);c.takeScreenshot("161-appearance-import");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();var image=editor.getAction("Import Image").getBounds();var field=editor.getControl("Background File").getBounds();check(image.y()>field.y(),"Import Image is beside its background settings");number("Window Width",360);number("Window Height",240);number("Font Scale",1.3);});c.waitTicks(12);c.takeScreenshot("161-appearance-small");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.FRIENDS);check(gui.getSidebar().isExpanded(),"Saved utility destination retains automatic expansion");bool("Reduced Motion",true);gui.selectPage(ClickGui.Page.RENDER);});c.waitTicks(2);
        c.runOnClient(mc->check(Client.INSTANCE.getClickGui().getSidebar().selectionOffset()==4*20.75,"Reduced Motion makes selection movement immediate"));
        c.runOnClient(mc->{mc.gui.setScreen(new ProbeScreen());});c.waitTick();
        c.runOnClient(mc->{check(MenuTheme.screenScale(mc.gui.screen())==1&&MenuTheme.frameProgress(mc.gui.screen())==1,"Reduced Motion disables native entrance movement and fade");check(MenuTheme.backdropPasses()==1,"Reduced Motion retains one backdrop");Client.INSTANCE.getThemeManager().reset();});
        try(var world=c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            c.runOnClient(mc->{number("Opening Duration",1);mc.gui.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));});c.waitTick();c.takeScreenshot("161-pause-moving");c.waitTicks(24);c.takeScreenshot("161-pause-settled");
            c.runOnClient(mc->check(MenuTheme.backdropPasses()==1,"Pause menu has no residual vanilla overlay"));
        }
        Client.INSTANCE.getLogger().info("UI repair checks passed ({} assertions)",checks);
    }
}
