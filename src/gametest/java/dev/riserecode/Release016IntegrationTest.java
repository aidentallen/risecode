package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.MouseScrollEvent;
import dev.riserecode.map.*;
import dev.riserecode.module.Module;
import dev.riserecode.module.modules.*;
import dev.riserecode.module.modules.hud.MinimapModule;
import dev.riserecode.setting.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.font.StyledText;
import dev.riserecode.ui.sound.UiSounds;
import dev.riserecode.ui.theme.ThemeManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.*;
import net.minecraft.world.level.block.Blocks;

public final class Release016IntegrationTest {
    private static int checks;
    private Release016IntegrationTest(){ }
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static void number(String key,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(key)).setValue(value);Client.INSTANCE.getThemeManager().changed();}
    private static void bool(String key,boolean value){((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting(key)).setValue(value);Client.INSTANCE.getThemeManager().changed();}
    @SuppressWarnings("unchecked") private static <E extends Enum<E>> void choice(String key,E value){((EnumSetting<E>)Client.INSTANCE.getThemeManager().getSetting(key)).setValue(value);Client.INSTANCE.getThemeManager().changed();}
    public static void run(ClientGameTestContext c){
        Client.INSTANCE.getLogger().info("0.16 stage: startup");
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();Client.INSTANCE.getModuleManager().disableAll();mc.options.pauseOnLostFocus=false;mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);});
        Client.INSTANCE.getLogger().info("0.16 stage: native title");
        c.setScreen(TitleScreen::new);c.waitTicks(8);
        c.runOnClient(mc->{
            var text=StyledText.of(Component.literal("§aVideo §7disabled Ω 中").withStyle(s->s.withClickEvent(new ClickEvent.CopyToClipboard("video"))));
            check(text.runs().stream().noneMatch(run->run.text().contains("§")),"Formatting markers never become glyphs");
            check(text.runs().getFirst().style().getColor()!=null&&text.runs().getLast().style().getClickEvent()!=null,"Legacy colors preserve component click metadata");
            check(text.width(mc.font)==text.regions(mc.font).stream().mapToDouble(StyledText.Region::width).sum(),"Styled measurement matches clickable regions");
            check(text.wrap(mc.font,50).stream().allMatch(line->line.width(mc.font)<=50),"Styled wrapping respects measured width");
            var nativeText=dev.riserecode.ui.theme.Theme.nativeText(Component.literal("Native Ω 中 ♥"));
            check(StyledText.of(nativeText).width(mc.font)==mc.font.width(nativeText),"Explicit native font metrics and Unicode fallback agree");
            AtomicInteger actions=new AtomicInteger();ActionButton button=new ActionButton("Once",actions::incrementAndGet);button.setBounds(0,0,80,20);long sounds=UiSounds.dispatches();
            check(button.mouseClicked(5,5,0)&&actions.get()==1&&UiSounds.dispatches()==sounds+1,"Custom button dispatches one immediate action and sound");button.mouseReleased(5,5,0);check(actions.get()==1,"Release does not repeat action");
            Button nativeButton=Button.builder(Component.literal("Native"),ignored->actions.incrementAndGet()).bounds(0,0,80,20).build();nativeButton.setFocused(true);sounds=UiSounds.dispatches();nativeButton.keyPressed(new KeyEvent(257,0,0));
            check(actions.get()==2&&UiSounds.dispatches()==sounds+1,"Native keyboard activation dispatches once");
            Client.INSTANCE.getLogger().info("0.16 stage: opening client window");
            Client.INSTANCE.getClickGui().open(mc.gui.screen());
        });c.waitFor(mc->Client.INSTANCE.getClickGui().isOpen(),240);
        Client.INSTANCE.getLogger().info("0.16 stage: resizing client window");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.FRIENDS);check(gui.getSidebar().isExpanded(),"Utility destination expands sidebar automatically");number("Window Width",600);number("Window Height",400);});c.waitTicks(12);
        c.runOnClient(mc->{
            var gui=Client.INSTANCE.getClickGui();check(gui.getWindowBounds().width()==600&&gui.getWindowBounds().height()==400,"Independent window dimensions displayed");check(gui.getSearchField().getBounds().width()>450,"Content and clipping follow resized bounds");
            var shown=gui.getDisplayedWindowBounds();double mx=(shown.x()+shown.width()-3)*gui.getCanvasScale(),my=(shown.y()+shown.height()-3)*gui.getCanvasScale();var info=new net.minecraft.client.input.MouseButtonInfo(0,0);
            gui.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mx,my,info),false);gui.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(mx+12,my+8,info),12,8);gui.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(mx+12,my+8,info));
            check(Client.INSTANCE.getThemeManager().number("Window Width")>600&&Client.INSTANCE.getThemeManager().number("Window Height")>400,"Displayed resize handle changes both dimensions");number("Window Width",600);number("Window Height",400);
            bool("Client Animation Override",true);number("Client Pop Duration",2);number("Client Pop Scale",2);gui.open(new TitleScreen());
        });c.waitFor(mc->{var gui=Client.INSTANCE.getClickGui();return gui.getDisplayedWindowBounds().width()/gui.getWindowBounds().width()>.945;},240);
        c.runOnClient(mc->{
            var gui=Client.INSTANCE.getClickGui();var logical=gui.getWindowBounds();var shown=gui.getDisplayedWindowBounds();var arrow=gui.getSidebar().getArrowBounds();double scale=shown.width()/logical.width();
            double mx=(shown.x()+(arrow.centerX()-logical.x())*scale)*gui.getCanvasScale(),my=(shown.y()+(arrow.centerY()-logical.y())*scale)*gui.getCanvasScale();boolean expanded=gui.getSidebar().isExpanded();long sounds=UiSounds.dispatches();
            gui.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mx,my,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
            check(gui.getSidebar().isExpanded()!=expanded&&UiSounds.dispatches()==sounds+1,"Scaled entrance hit detection dispatches the arrow once");bool("Client Animation Override",false);number("Window Height",240);
        });c.waitTicks(12);
        c.runOnClient(mc->{var sidebar=Client.INSTANCE.getClickGui().getSidebar();var bounds=sidebar.getBounds();sidebar.mouseScrolled(bounds.centerX(),bounds.centerY(),-20);});c.waitTicks(8);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var sidebar=gui.getSidebar();var arrow=sidebar.getArrowBounds();var bounds=sidebar.getBounds();check(arrow.y()>=bounds.y()+40&&arrow.y()+arrow.height()<=bounds.y()+bounds.height(),"Short sidebar scroll reveals utility arrow");gui.keyPressed(new KeyEvent(262,0,0));check(sidebar.isExpanded(),"Keyboard navigation expands inline utilities");number("Window Height",400);gui.selectPage(ClickGui.Page.APPEARANCE);gui.getThemeEditor().reveal("Background");});c.waitTicks(12);c.takeScreenshot("016-background-browser");
        c.runOnClient(mc->{choice("Background",ThemeManager.Background.SPACE);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.RESOURCES);});c.waitTicks(12);c.takeScreenshot("016-resources-compact");
        c.runOnClient(mc->{bool("Reduced Motion",true);check(dev.riserecode.ui.animation.Animation.scale(ThemeManager.Channel.OPENING)==0,"Reduced Motion disables scale");check(new dev.riserecode.ui.animation.ButtonFeedback().scale()==1,"Reduced Motion disables spring");Client.INSTANCE.getThemeManager().resetLayout();check(Client.INSTANCE.getThemeManager().number("Window Width")==428&&Client.INSTANCE.getThemeManager().number("Window Height")==320,"Reset Layout restores dimensions");Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(new net.minecraft.client.gui.screens.options.VideoSettingsScreen(new TitleScreen(),mc,mc.options));});c.waitTicks(10);c.takeScreenshot("016-video-settings");
        c.runOnClient(mc->{
            try{
                var assets=java.nio.file.Files.createTempDirectory("risecode-ui-import-");var image=assets.resolve("background.png");var sound=assets.resolve("click.ogg");
                try(var in=Release016IntegrationTest.class.getResourceAsStream("/assets/riserecode/textures/gui/space.png")){java.nio.file.Files.copy(in,image);}
                try(var in=Release016IntegrationTest.class.getResourceAsStream("/assets/riserecode/sounds/ui/soft_pop.ogg")){java.nio.file.Files.copy(in,sound);}
                var themes=Client.INSTANCE.getThemeManager();((StringSetting)themes.getSetting("Background File")).setValue(image.toString());themes.importBackground();
                check(themes.text("Background File").matches("[a-f0-9]{64}\\.png")&&themes.choice("Background",ThemeManager.Background.class)==ThemeManager.Background.LOCAL_IMAGE,"Imported background is validated and content addressed");
                ((StringSetting)themes.getSetting("Sound File")).setValue(sound.toString());UiSounds.importOgg(themes);
            }catch(java.io.IOException error){throw new AssertionError("Local asset import",error);}
        });
        c.waitFor(mc->mc.gui.overlay()==null,1200);c.waitTicks(4);
        c.runOnClient(mc->{check(mc.getResourceManager().getResource(net.minecraft.resources.Identifier.fromNamespaceAndPath("riserecode","sounds/ui/local_ogg.ogg")).isPresent(),"Imported OGG published through local resource pack");check(UiSounds.play()!=net.minecraft.client.sounds.SoundEngine.PlayResult.NOT_STARTED,"Imported sound preview starts");});
        c.takeScreenshot("016-imported-background");
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("tp @a 0.5 70 0.5 0 0");
            c.waitFor(mc->mc.player!=null&&Client.INSTANCE.getMaps().ready(),600);c.runOnClient(mc->mc.gui.setScreen(null));
            var zoom=Client.INSTANCE.getModuleManager().getByClass(ZoomModule.class).orElseThrow();
            c.runOnClient(mc->{zoom.setEnabled(true);zoom.getMagnification().setValue(3.0);});c.waitTicks(8);check(Client.INSTANCE.getCameraEffects().magnification()==1,"Enabling Zoom only arms C");
            c.getInput().holdKey(InputConstants.KEY_C);c.waitTicks(8);check(zoom.isZoomActive()&&Client.INSTANCE.getCameraEffects().magnification()>2.9,"Hold activates zoom");
            c.runOnClient(mc->{var event=Client.INSTANCE.getEventBus().post(new MouseScrollEvent(0,2));check(event.isCancelled()&&zoom.activeMagnification()>3&&zoom.getMagnification().getValue()==3,"Wheel magnification is temporary");});
            c.getInput().releaseKey(InputConstants.KEY_C);c.waitTicks(8);check(zoom.isEnabled()&&!zoom.isZoomActive()&&Client.INSTANCE.getCameraEffects().magnification()==1,"Release leaves Zoom armed");
            c.getInput().holdKey(InputConstants.KEY_C);c.waitTicks(4);check(zoom.activeMagnification()==3,"Each activation starts at saved magnification");c.getInput().releaseKey(InputConstants.KEY_C);
            c.runOnClient(mc->Client.INSTANCE.getModuleManager().setBindMode(zoom,Module.BindMode.TOGGLE));c.getInput().pressKey(InputConstants.KEY_C);c.waitTicks(4);check(zoom.isZoomActive(),"Toggle activates");c.getInput().pressKey(InputConstants.KEY_C);c.waitTicks(4);check(!zoom.isZoomActive()&&zoom.isEnabled(),"Toggle releases without disarming");
            c.getInput().pressKey(InputConstants.KEY_C);c.waitTicks(3);c.runOnClient(mc->Client.INSTANCE.getModuleManager().setKeybind(zoom,InputConstants.KEY_V));c.waitTicks(3);check(!zoom.isZoomActive(),"Rebinding releases active zoom");
            c.getInput().pressKey(InputConstants.KEY_V);c.waitTicks(3);c.runOnClient(mc->mc.gui.setScreen(new PauseScreen(true)));c.waitTicks(8);check(!zoom.isZoomActive(),"Opening a screen releases active zoom");c.takeScreenshot("016-pause-menu");
            PauseScreen pause=c.computeOnClient(mc->(PauseScreen)mc.gui.screen());c.runOnClient(mc->{var button=(Button)pause.children().stream().filter(child->child instanceof Button b&&b.getMessage().getString().equals("risecode GUI")).findFirst().orElseThrow();button.onPress(new KeyEvent(257,0,0));});c.waitFor(mc->Client.INSTANCE.getClickGui().isOpen(),240);c.runOnClient(mc->Client.INSTANCE.getClickGui().onClose());c.waitFor(mc->mc.gui.screen()==pause,240);check(true,"Pause GUI button returns to original pause menu");
            c.runOnClient(mc->mc.gui.setScreen(null));
            world.getServer().runCommand("fill -8 54 -8 8 54 8 stone");world.getServer().runCommand("fill -8 55 -8 8 61 8 air");world.getServer().runCommand("fill -8 62 -8 8 64 8 stone");world.getServer().runCommand("fill -8 70 -8 8 70 8 stone");world.getServer().runCommand("tp @a 0.5 55 0.5");c.waitTicks(70);
            c.waitFor(mc->Client.INSTANCE.getMaps().layer(MapLayerPolicy.Mode.AUTO)==MapLayerPolicy.Layer.CAVE,600);
            c.waitFor(mc->{var maps=Client.INSTANCE.getMaps();var key=maps.key(maps.dimension(),0,0,0,MapLayerPolicy.Mode.CAVE,Integer.MIN_VALUE);var tile=maps.tile(key);return tile!=null&&tile.height(0)==54;},600);
            check(true,"Cave capture displays floor rather than roof");
            c.waitFor(mc->{var maps=Client.INSTANCE.getMaps();var tile=maps.tile(maps.key(maps.dimension(),0,0,0,MapLayerPolicy.Mode.CAVE,65));return tile!=null&&tile.height(0)==64;},600);
            check(true,"Stacked manual cave layer keeps a separate floor");
            c.runOnClient(mc->{var maps=Client.INSTANCE.getMaps();var key=maps.key(maps.dimension(),0,0,0,MapLayerPolicy.Mode.CAVE,Integer.MIN_VALUE);var tile=maps.tile(key);check(tile.detailed()&&tile.bytes()>16_000,"Cave tile retains texture detail");check(maps.cpuBytes()<=MapService.CPU_LIMIT&&maps.textures().bytes()<=MapTextures.GPU_LIMIT,"Map residency bounded by bytes");mc.gui.setScreen(new WorldMapScreen(null,maps));});c.waitTicks(16);c.takeScreenshot("016-map-cave-fullscreen");
            c.runOnClient(mc->{var maps=Client.INSTANCE.getMaps();var settings=maps.preferences();settings.addProperty("panel",true);maps.preferences(settings);mc.gui.setScreen(new WorldMapScreen(null,maps));});c.waitTicks(10);c.takeScreenshot("016-map-panel");
            c.runOnClient(mc->{mc.gui.setScreen(null);var mini=Client.INSTANCE.getModuleManager().getByClass(MinimapModule.class).orElseThrow();mini.shape.setValue(MinimapModule.Shape.CIRCLE);mini.setEnabled(true);});c.waitTicks(10);c.takeScreenshot("016-minimap-circle");
            c.runOnClient(mc->{var maps=Client.INSTANCE.getMaps();maps.recording(false);int count=maps.waypoints().all().size();mc.player.setHealth(0);Client.INSTANCE.getEventBus().post(new dev.riserecode.event.UpdateEvent(mc,12345));check(maps.waypoints().all().size()==count+1,"Deaths record with recording and overlays disabled");mc.player.setHealth(20);maps.recording(true);Client.INSTANCE.getModuleManager().disableAll();});
            ColorOutlineIntegrationTest.verify(c,world);
        }
        Client.INSTANCE.getLogger().info("Release 0.16 native checks passed ({} assertions)",checks);
    }
}
