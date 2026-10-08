package dev.riserecode;

import dev.riserecode.module.Module;
import dev.riserecode.module.modules.AmbienceModule;
import dev.riserecode.module.modules.BaritoneModule;
import dev.riserecode.module.modules.hud.WatermarkModule;
import dev.riserecode.module.modules.hud.ArrayListModule;
import dev.riserecode.render.AmbienceRenderer;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.StringSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.FocusedInput;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.hud.HudEditorScreen;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.theme.BrandingLogo;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.CharacterEvent;

/** Disposable real-input and GPU checks, run with vanilla and the installed optional stack. */
public final class UpgradeIntegrationTest {
    private static int checks;
    private UpgradeIntegrationTest() { }
    private static void check(boolean value,String label) { checks++; if(!value){throw new AssertionError(label);} }
    private static ClickGui gui() { return Client.INSTANCE.getClickGui(); }
    private static void click(ClientGameTestContext c,UIComponent.Bounds b,int button,boolean canvas) {
        double[] p=c.computeOnClient(mc->new double[]{(b.x()+b.width()*0.5)*(canvas?gui().getCanvasScale():1)*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth(),
            (b.y()+Math.min(5,b.height()/2))*(canvas?gui().getCanvasScale():1)*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight()});
        c.getInput().setCursorPos(p[0],p[1]);c.waitTick();c.getInput().pressMouse(button);c.waitTick();
    }
    private static void paste(ClientGameTestContext c,String text) {
        c.runOnClient(mc->{mc.keyboardHandler.setClipboard(text);var keyboard=(dev.riserecode.testmixin.KeyboardInputInvoker)mc.keyboardHandler;
            for(int key:new int[]{65,86}) { keyboard.riserecode$press(mc.getWindow().handle(),1,new net.minecraft.client.input.KeyEvent(key,0,2));keyboard.riserecode$press(mc.getWindow().handle(),0,new net.minecraft.client.input.KeyEvent(key,0,2)); }});c.waitTick();
    }
    private static void edit(ClientGameTestContext c,Slider slider,boolean canvas,String value) { click(c,slider.getBounds(),0,canvas);check(c.computeOnClient(mc->slider.input().isFocused()),"Numeric field focused");paste(c,value); }
    public static void run(ClientGameTestContext c) {
        var client=Client.INSTANCE;var themes=client.getThemeManager();var modules=client.getModuleManager();
        c.runOnClient(mc->{themes.reset();mc.options.enableVsync().set(false);mc.options.bobView().set(false);});
        UpgradeMenuTest.run(c);
        c.setScreen(TitleScreen::new);c.waitTicks(12);c.takeScreenshot("upgrade-title");
        check(c.computeOnClient(mc->mc.gui.screen().children().stream().anyMatch(child->child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().contains("Interface"))),"Title interface button");
        check(c.computeOnClient(mc->MenuTheme.activeMenu()),"Themed title recognized");
        c.runOnClient(mc->gui().open(mc.gui.screen()));c.waitFor(mc->gui().isOpen(),240);
        c.runOnClient(mc->{gui().selectPage(ClickGui.Page.APPEARANCE);gui().getThemeEditor().reveal("Font Scale");});c.waitTicks(15);
        var scale=c.computeOnClient(mc->(Slider)gui().getThemeEditor().getControl("Font Scale"));
        edit(c,scale,true,"1.25");check(themes.number("Font Scale")==1.25,"Appearance input previews: value="+themes.number("Font Scale")+" text="+scale.input().getValue()+" validation="+scale.input().validation());
        c.getInput().pressKey(344);check(c.computeOnClient(mc->mc.gui.screen()==gui() && scale.input().isFocused()),"Right Shift isolated from focused value");
        paste(c,"-");check(themes.number("Font Scale")==1.25 && !scale.input().validation().isEmpty(),"Incomplete input retains valid preview");
        c.getInput().pressKey(256);check(themes.number("Font Scale")==1 && c.computeOnClient(mc->mc.gui.screen()==gui()),"Escape cancels without closing");
        edit(c,scale,true,"1.1");c.getInput().pressKey(257);check(themes.number("Font Scale")==1.1 && !FocusedInput.active(),"Enter commits native clipboard input");
        c.runOnClient(mc->{((DoubleSetting)themes.getSetting("Font Scale")).setValue(1.0);themes.changed();gui().selectPage(ClickGui.Page.SEARCH);gui().getSearchField().setValue("Crosshair");gui().getSearchField().setFocused(false);});c.waitTicks(12);
        var card=c.computeOnClient(mc->gui().getModuleButton("Crosshair"));click(c,card.getBounds(),1,true);c.waitTicks(15);
        var moduleScale=(Slider)card.getSettingControls().stream().filter(control->control.setting().getName().equals("Scale")).findFirst().orElseThrow().component();
        edit(c,moduleScale,true,"2.25");c.getInput().pressKey(257);check(modules.getByName("Crosshair").orElseThrow().getSettings().stream().anyMatch(s->s.getName().equals("Scale") && s.getValue().equals(2.25)),"Module numeric input");
        c.runOnClient(mc->{gui().selectPage(ClickGui.Page.APPEARANCE);gui().getThemeEditor().reveal("Primary Accent");});c.waitTicks(12);
        var picker=c.computeOnClient(mc->(ColorPicker)gui().getThemeEditor().getControl("Primary Accent"));click(c,picker.getBounds(),1,true);c.waitTicks(15);
        c.runOnClient(mc->picker.channel(1).beginEditing());paste(c,"128");c.getInput().pressKey(257);check((themes.color("Primary Accent")>>16&255)==128,"Typed color channel previews and commits");
        c.runOnClient(mc->picker.speed().beginEditing());paste(c,"0.75");c.getInput().pressKey(257);check(themes.colorSetting("Primary Accent").getSpeed()==0.75,"Typed gradient speed");
        c.runOnClient(mc->picker.spacing().beginEditing());paste(c,"180");c.getInput().pressKey(256);check(themes.colorSetting("Primary Accent").getSpacing()==100,"Gradient cancellation restores prior value");
        c.runOnClient(mc->{themes.reset();gui().getThemeEditor().reveal("Appearance Preset");});c.waitTicks(8);
        c.runOnClient(mc->{gui().selectPage(ClickGui.Page.APPEARANCE);themes.applyPreset(ThemeManager.Preset.AURORA);});c.waitTicks(10);c.takeScreenshot("upgrade-appearance-presets");
        c.runOnClient(mc->{check(themes.undoPreset(),"Preset Undo in game");((StringSetting)themes.getSetting("Display Name")).setValue("test brand");((StringSetting)themes.getSetting("Watermark Text")).setValue("independent mark");});
        c.runOnClient(mc->{try{Path file=Path.of("config","test-logo.png");try(var image=new com.mojang.blaze3d.platform.NativeImage(16,16,false)){for(int y=0;y<16;y++){for(int x=0;x<16;x++){image.setPixel(x,y,((x+y)%2==0)?0xFF59D5CB:0xFFB487E9);}}image.writeToFile(file);}((StringSetting)themes.getSetting("Logo File")).setValue(file.toAbsolutePath().toString());themes.importLogo();}catch(java.io.IOException e){throw new AssertionError(e);}gui().getThemeEditor().reveal("Logo File");});
        c.waitTicks(20);c.takeScreenshot("upgrade-branding-logo");long loads=BrandingLogo.loads();c.waitTicks(10);check(BrandingLogo.loads()==loads && loads>0,"Logo texture cached");
        c.runOnClient(mc->{check(themes.saveProfile("UpgradeBrand"),"Appearance profile saves");themes.reset();check(themes.loadProfile("UpgradeBrand") && client.getName().equals("test brand") && themes.text("Watermark Text").equals("independent mark"),"Independent branding round trip");});
        c.runOnClient(mc->{FocusedInput.commit();mc.gui.setScreen(null);themes.reset();});
        try(var world=c.worldBuilder().create()) {
            world.getServer().runCommand("gamemode spectator @a");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");
            world.getServer().runCommand("forceload add 80 80 120 120");
            world.getServer().runOnServer(server->{for(int x=5;x<=7;x++){for(int z=5;z<=7;z++){server.overworld().getChunk(x,z);}}});c.waitTicks(12);
            world.getServer().runCommand("fill 85 70 85 115 70 115 minecraft:stone");world.getServer().runCommand("fill 100 67 99 111 70 110 minecraft:stone");world.getServer().runCommand("fill 101 68 100 110 70 109 minecraft:water");
            world.getServer().runCommand("setblock 106 73 104 minecraft:water");world.getServer().runCommand("tp @a 98.5 72 98.5 -35 8");
            c.waitTicks(5);c.runOnClient(mc->{mc.player.setPos(98.5,72,98.5);mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);mc.player.setYRot(-35);mc.player.setXRot(8);});
            try { c.waitFor(mc->mc.player.getX()>98 && mc.player.getY()>71 && mc.level.getBlockState(new net.minecraft.core.BlockPos(104,70,104)).getFluidState().is(net.minecraft.tags.FluidTags.WATER),400); }
            catch(AssertionError failure) { c.runOnClient(mc->client.getLogger().error("Fixture timed out: pos={} water={} spectator={} flying={}",mc.player.position(),mc.level.getBlockState(new net.minecraft.core.BlockPos(104,70,104)),mc.player.isSpectator(),mc.player.getAbilities().flying));throw failure; }
            world.getConnection().waitForChunksRender();c.waitTicks(40);
            c.runOnClient(mc->client.getLogger().info("Water fixture player={} camera={} loadedWater={}",mc.player.position(),mc.gameRenderer.mainCamera().position(),mc.level.getBlockState(new net.minecraft.core.BlockPos(104,70,104))));
            var ambience=modules.getByClass(AmbienceModule.class).orElseThrow();var baritone=modules.getByClass(BaritoneModule.class).orElseThrow();
            check(!ambience.isEnabled(),"Ambience defaults disabled");check(ambience.mode.getValue()==AmbienceModule.SkyMode.AURORA && ambience.quality.getValue()==AmbienceModule.Quality.BALANCED && ambience.theme.getValue(),"Ambience defaults");
            c.runOnClient(mc->{modules.setEnabled(ambience,true);mc.player.setXRot(8);});
            for(var mode:AmbienceModule.SkyMode.values()) { c.runOnClient(mc->ambience.mode.setValue(mode));c.waitTicks(8);c.takeScreenshot("upgrade-sky-"+mode.name().toLowerCase(java.util.Locale.ROOT)); }
            check(AmbienceRenderer.frames()>20,"Procedural sky executes");check(AmbienceRenderer.waterDraws()>20,"Water uses native terrain pipeline");
            c.runOnClient(mc->{ambience.sky.setValue(false);});long sky=AmbienceRenderer.frames();c.waitTicks(6);check(AmbienceRenderer.frames()==sky && c.computeOnClient(mc->AmbienceRenderer.waterActive()),"Independent sky and water switches");
            c.runOnClient(mc->{ambience.sky.setValue(true);ambience.water.setValue(false);});long water=AmbienceRenderer.waterDraws();c.waitTicks(6);check(AmbienceRenderer.waterDraws()==water && c.computeOnClient(mc->AmbienceRenderer.skyActive()),"Independent water switch");
            c.runOnClient(mc->ambience.water.setValue(true));world.getServer().runCommand("tp @a 104.5 68.2 104.5 0 0");c.waitTicks(8);c.takeScreenshot("upgrade-water-underwater");
            c.runOnClient(mc->modules.setEnabled(ambience,false));sky=AmbienceRenderer.frames();water=AmbienceRenderer.waterDraws();c.waitTicks(8);check(sky==AmbienceRenderer.frames() && water==AmbienceRenderer.waterDraws(),"Disable restores native sky and fluids");
            var watermark=modules.getByClass(WatermarkModule.class).orElseThrow();var array=modules.getByClass(ArrayListModule.class).orElseThrow();
            c.runOnClient(mc->{modules.setEnabled(watermark,true);modules.setEnabled(array,true);client.getHudRenderer().setEnabled(true);modules.getByName("Crosshair").orElseThrow().setEnabled(true);});c.waitTicks(15);
            check(client.getHudRenderer().getEntries().stream().noneMatch(e->e.module()==watermark||e.module()==array),"HUD containers excluded from array");
            c.runOnClient(mc->{modules.setEnabled(watermark,false);check(client.getProfileManager().saveProfile("Upgrade"),"Module profile saves");modules.setEnabled(watermark,true);check(client.getProfileManager().loadProfile("Upgrade"),"Module profile loads");check(!watermark.isEnabled() && array.isEnabled(),"Independent HUD states round trip");client.openHudEditor();});c.waitTicks(15);
            var box=client.getHudRenderer().getBoxes().stream().filter(b->b.element().id().equals("watermark")).findFirst().orElseThrow();click(c,new UIComponent.Bounds(box.x(),box.y(),box.width(),box.height()),1,false);c.waitTicks(8);
            var editor=c.computeOnClient(mc->(HudEditorScreen)mc.gui.screen());var hudScale=(Slider)editor.getControls().getFirst();edit(c,hudScale,false,"1.35");c.getInput().pressKey(257);check(box.element().layout().scale.getValue()==1.35,"HUD scale typed");
            var hudX=(Slider)editor.getControls().get(1);edit(c,hudX,false,"0.125");c.getInput().pressKey(257);check(box.element().layout().x()==0.125,"HUD normalized position typed");
            c.runOnClient(mc->mc.gui.setScreen(null));
            c.runOnClient(mc->{modules.setEnabled(baritone,true);});check(baritone.access().available()==net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("baritone"),"Optional dependency availability");
            check(c.computeOnClient(mc->baritone.status().contains("Idle")) || !baritone.access().available(),"Enabling never starts saved task");
            if(baritone.access().available()) {
                world.getServer().runCommand("gamemode survival @a");world.getServer().runCommand("tp @a 96.5 71 96.5 0 0");world.getServer().runCommand("give @a minecraft:diamond_pickaxe");c.waitTicks(20);
                c.runOnClient(mc->{baritone.x.setValue(92.0);baritone.y.setValue(71.0);baritone.z.setValue(96.0);baritone.action("Start");});c.waitTicks(5);
                c.runOnClient(mc->client.getCommandManager().handleChat(".b pause"));check(c.computeOnClient(mc->baritone.status().equals("Paused")),"Pause through .b forwarding");c.runOnClient(mc->baritone.action("Resume"));
                c.waitFor(mc->mc.player.position().distanceToSqr(92.5,71,96.5)<2,400);check(true,"Navigation reaches destination");
                world.getServer().runCommand("setblock 91 71 96 minecraft:diamond_ore");c.waitTicks(6);c.runOnClient(mc->{baritone.task.setValue(BaritoneModule.Task.MINE);baritone.targets.setValue("minecraft:diamond_ore");baritone.action("Start");});
                c.waitFor(mc->mc.level.getBlockState(new net.minecraft.core.BlockPos(91,71,96)).isAir(),400);check(true,"Mining removes target");
                c.runOnClient(mc->{baritone.action("Stop");var follower=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(java.util.UUID.fromString("12345678-1234-1234-1234-123456789abc"),"TestFollower"));follower.setId(999998);follower.setPos(mc.player.getX()+4,71,mc.player.getZ());mc.level.addEntity(follower);baritone.task.setValue(BaritoneModule.Task.FOLLOW);baritone.player.setValue("TestFollower");baritone.action("Start");});c.waitTicks(5);
                check(c.computeOnClient(mc->baritone.status().toLowerCase(java.util.Locale.ROOT).contains("follow")),"Follow task starts through API");
                c.setScreen(()->new net.minecraft.client.gui.screens.ChatScreen(".b resu",false));c.waitTicks(3);c.getInput().pressKey(258);c.waitTick();
                check(c.computeOnClient(mc->((dev.riserecode.testmixin.ChatInputAccessor)mc.gui.screen()).riserecode$input().getValue().equals(".b resume")),"Native chat Tab completes forwarded commands");c.takeScreenshot("upgrade-baritone-completion");
                c.runOnClient(mc->{mc.gui.setScreen(null);client.getCommandManager().handleChat(".baritone cancel");});check(c.computeOnClient(mc->baritone.status().contains("Idle")),".baritone forwarding stops task");
                c.runOnClient(mc->baritone.action("Start"));c.waitTicks(3);
                c.runOnClient(mc->{check(!baritone.access().complete("go").isEmpty(),"Public API completion");check(client.getProfileManager().saveProfile("BaritoneTask"),"Task controls saved");check(client.getProfileManager().loadProfile("BaritoneTask"),"Task controls loaded");});
                check(c.computeOnClient(mc->baritone.status().contains("Idle")),"Profile never resumes saved task");
                c.runOnClient(mc->{baritone.action("Start");baritone.action("Pause");modules.setEnabled(baritone,false);});check(c.computeOnClient(mc->baritone.status().contains("Idle")),"Disable stops paused task");
                c.runOnClient(mc->{modules.setEnabled(baritone,true);baritone.task.setValue(BaritoneModule.Task.DESTINATION);baritone.x.setValue(88.0);baritone.action("Start");});
                c.waitFor(mc->mc.player.position().distanceToSqr(88.5,71,96.5)<2,400);c.runOnClient(mc->baritone.action("Stop"));check(c.computeOnClient(mc->baritone.status().contains("Idle")),"Restart after paused disable releases movement");
            } else { c.runOnClient(mc->baritone.action("Start"));check(c.computeOnClient(mc->baritone.status().contains("Unavailable")),"Missing dependency feedback"); }
            c.runOnClient(mc->{modules.setEnabled(ambience,true);});
            if(Boolean.getBoolean("riserecode.withShaderPack")) {
                c.runOnClient(mc->shaderPack(true));c.waitTicks(10);
                check(AmbienceRenderer.shaderPackActive()&&!c.computeOnClient(mc->AmbienceRenderer.skyActive())&&!c.computeOnClient(mc->AmbienceRenderer.waterActive()),"Iris shader pack owns sky and water");
                check(ambience.isEnabled() && ambience.water.getValue(),"Iris handoff preserves enabled settings");c.takeScreenshot("upgrade-iris-handoff");
                c.runOnClient(mc->shaderPack(false));c.waitTicks(10);check(c.computeOnClient(mc->AmbienceRenderer.skyActive())&&c.computeOnClient(mc->AmbienceRenderer.waterActive()),"Ambience resumes when Iris shaders disabled");
            }
            world.getServer().runCommand("gamemode spectator @a");world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0 80 0");
            c.waitFor(mc->mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER),400);c.waitTicks(8);
            long beforeDimensionSky=AmbienceRenderer.frames();c.waitTicks(6);check(AmbienceRenderer.frames()==beforeDimensionSky,"Native dimension without sky retained");
            world.getServer().runCommand("execute in minecraft:overworld run tp @a 98.5 72 98.5 -35 8");
            c.waitFor(mc->mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD),400);c.waitTicks(8);check(c.computeOnClient(mc->AmbienceRenderer.skyActive()),"Ambience resumes across dimensions");
            var reload=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->reload.isDone(),600);c.waitTicks(15);check(c.computeOnClient(mc->AmbienceRenderer.skyActive()),"Resource reload resumes enabled effect");c.takeScreenshot("upgrade-after-resource-reload");
        }
        c.waitTicks(10);check(!c.computeOnClient(mc->AmbienceRenderer.skyActive())&&!c.computeOnClient(mc->AmbienceRenderer.waterActive()),"Disconnect removes rendering overrides");
        c.runOnClient(mc->{modules.disableAll();themes.reset();FocusedInput.commit();});
        client.getLogger().info("Upgrade GPU/input checks passed ({} assertions)",checks);
    }
    private static void shaderPack(boolean enabled) {
        try {
            Class<?> iris=Class.forName("net.irisshaders.iris.Iris");Object config=iris.getMethod("getIrisConfig").invoke(null);
            config.getClass().getMethod("setShaderPackName",String.class).invoke(config,"ComplementaryUnbound_r5.9.3.zip");
            config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);
        } catch(ReflectiveOperationException failure) { throw new AssertionError("Installed Iris pack handoff",failure); }
    }
}
