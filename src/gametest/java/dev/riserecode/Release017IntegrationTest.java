package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.module.modules.NoFallModule;
import dev.riserecode.setting.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.menu.*;
import dev.riserecode.ui.theme.*;
import dev.riserecode.event.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Native pixel scenes, keyboard routing, page isolation, and authoritative Survival fall tests. */
public final class Release017IntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private Release017IntegrationTest(){}
    private static final class FeedbackProbe extends net.minecraft.client.gui.screens.Screen {
        net.minecraft.client.gui.components.Button action,disabled;
        int activations;
        FeedbackProbe(){super(net.minecraft.network.chat.Component.literal("Button feedback"));}
        @Override protected void init(){action=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Themed action"),b->activations++).bounds(width/2-80,height/2-12,160,24).build());disabled=addRenderableWidget(net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Disabled"),b->activations++).bounds(width/2-80,height/2+20,160,24).build());disabled.active=false;}
    }
    private static void number(String name,double value){((DoubleSetting)Theme.manager().getSetting(name)).setValue(value);}
    private static <E extends Enum<E>> void option(String name,E value){ThemeManager.assign(Theme.manager().getSetting(name),value);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Theme.manager().reset();mc.options.enableVsync().set(false);mc.options.framerateLimit().set(240);number("Font Scale",1.3);number("Glow Radius",16.4);number("Glow Intensity",.59);((ColorSetting)Theme.manager().getSetting("Primary Accent")).setValue(0xFFC000FF);((ColorSetting)Theme.manager().getSetting("Secondary Accent")).setValue(0xFFFF8A00);});
        var probe=c.computeOnClient(mc->new FeedbackProbe());c.setScreen(()->probe);c.waitTicks(25);
        double[] cursor=c.computeOnClient(mc->new double[]{(probe.action.getX()+80)*mc.getWindow().getGuiScale(),(probe.action.getY()+10)*mc.getWindow().getGuiScale()});
        c.getInput().setCursorPos(cursor[0],cursor[1]);c.waitTicks(8);
        c.runOnClient(mc->{check(MenuTheme.hover(probe.action)>.9,"native themed button has animated hover");check(MenuTheme.hover(probe.disabled)==0,"disabled buttons do not hover");long sounds=dev.riserecode.ui.sound.UiSounds.dispatches();probe.action.setFocused(true);probe.action.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN,0,0));check(probe.activations==1&&dev.riserecode.ui.sound.UiSounds.dispatches()==sounds+1,"keyboard activation dispatches one action and sound");});
        c.takeScreenshot("0171-button-subtle-hover");
        c.runOnClient(mc->option("Button Style",ThemeManager.ButtonStyle.ACCENT));c.waitTicks(8);c.takeScreenshot("0171-button-accent-hover");
        c.runOnClient(mc->option("Button Style",ThemeManager.ButtonStyle.SUBTLE));
        c.runOnClient(mc->{for(var sound:new ThemeManager.ClickSound[]{ThemeManager.ClickSound.TICK,ThemeManager.ClickSound.TAP,ThemeManager.ClickSound.MECHANICAL,ThemeManager.ClickSound.WOOD,ThemeManager.ClickSound.GLASS}){option("Click Sound",sound);check(dev.riserecode.ui.sound.UiSounds.play()!=net.minecraft.client.sounds.SoundEngine.PlayResult.NOT_STARTED,"sound asset plays: "+sound);}option("Click Sound",ThemeManager.ClickSound.SOFT_POP);});
        c.setScreen(TitleScreen::new);c.waitTicks(25);c.takeScreenshot("0171-title-subtle");
        c.runOnClient(mc->option("Button Style",ThemeManager.ButtonStyle.ACCENT));c.waitTicks(8);c.takeScreenshot("0171-title-accent");
        c.runOnClient(mc->option("Button Style",ThemeManager.ButtonStyle.SUBTLE));
        c.runOnClient(mc->check(mc.gui.screen().children().stream().anyMatch(PageGear.class::isInstance),"native title has recovery gear"));
        c.getInput().pressKey(InputConstants.KEY_RSHIFT);c.waitForScreen(ClickGui.class);c.waitTicks(20);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setFocused(true);gui.getSearchField().setValue("test search");});
        c.getInput().pressKey(InputConstants.KEY_RSHIFT);c.waitForScreen(TitleScreen.class);check(true,"Right Shift closes focused search");
        c.getInput().pressKey(InputConstants.KEY_RSHIFT);c.waitForScreen(ClickGui.class);c.waitTicks(20);
        c.runOnClient(mc->{
            var gui=Client.INSTANCE.getClickGui();
            check(ClickGui.VISIBLE_PAGES.equals(java.util.List.of(ClickGui.Page.SEARCH,ClickGui.Page.COMBAT,ClickGui.Page.MOVEMENT,ClickGui.Page.PLAYER,ClickGui.Page.RENDER,ClickGui.Page.WORLD,ClickGui.Page.UTILITY,ClickGui.Page.HUD,ClickGui.Page.APPEARANCE,ClickGui.Page.RESOURCES)),"original ten tabs restored");
            for(var category:dev.riserecode.module.Category.values()){
                gui.selectCategory(category);check(gui.getPage().name().equals(category.name()),"original category navigation: "+category);
                check(gui.getActivePanel().getModuleButtons().stream().allMatch(b->b.getModule().getCategory()==category&&b.getModule().classification()!=null),"category placement and classification: "+category);
            }
            var preferences=Client.INSTANCE.getConfigManager().getUiPreferences().deepCopy();
            for(String alias:new String[]{"GAMEPLAY","VISUALS","MOVEMENT"}){
                var migrated=preferences.deepCopy();migrated.addProperty("selectedTab",alias);Client.INSTANCE.getConfigManager().setUiPreferences(migrated);
                var loaded=new ClickGui(Client.INSTANCE.getModuleManager(),Client.INSTANCE.getHudRenderer(),()->{});
                check(loaded.getPage()==(alias.equals("GAMEPLAY")?ClickGui.Page.COMBAT:alias.equals("VISUALS")?ClickGui.Page.RENDER:ClickGui.Page.MOVEMENT),"saved selection migrates: "+alias);
            }
            Client.INSTANCE.getConfigManager().setUiPreferences(preferences);
            gui.selectPage(ClickGui.Page.COMBAT);
        });c.waitTicks(12);c.takeScreenshot("0171-original-tabs");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.HUD));c.waitTicks(8);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var header=gui.getHeaderLayout();check(header.gear().centerY()==gui.getEditHudButton().getBounds().centerY(),"gear and Edit HUD share their header center");check(header.label().x()+header.label().width()<header.action().x(),"HUD title leaves room for header actions");});c.takeScreenshot("alignment-hud-header");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.PROFILES));c.waitTicks(8);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.getSearchField().setValue("appearance-location");var save=gui.getActionButtons().stream().filter(a->a.label().equals("Save appearance profile")).findFirst().orElseThrow();var b=save.getBounds();check(save.mouseClicked(b.centerX(),b.centerY(),0)&&Theme.manager().getProfiles().contains("appearance-location"),"appearance profiles save from Profiles");});c.waitTicks(8);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var load=gui.getActionButtons().stream().filter(a->a.label().equals("Appearance: appearance-location")).findFirst().orElseThrow();var b=load.getBounds();check(load.mouseClicked(b.centerX(),b.centerY(),0),"appearance profiles load from Profiles");check(gui.getSearchField().getBounds().x()+gui.getSearchField().getBounds().width()<gui.getHeaderLayout().gear().x(),"profile input does not overlap gear");});c.takeScreenshot("alignment-profiles");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.UTILITY));c.waitTicks(8);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var baritone=gui.getModuleButton("Baritone");var field=(dev.riserecode.ui.component.setting.NumericField)baritone.getSettingControls().stream().filter(control->control.setting().getName().equals("Destination X")).findFirst().orElseThrow().component();field.beginEditing();"-29999999".codePoints().forEach(cp->dev.riserecode.ui.component.FocusedInput.character(new net.minecraft.client.input.CharacterEvent(cp)));dev.riserecode.ui.component.FocusedInput.commit();check(Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.BaritoneModule.class).orElseThrow().x.getValue()==-29999999,"coordinate value box accepts exact negative input");});
        c.runOnClient(mc->Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.APPEARANCE));c.waitTicks(10);c.takeScreenshot("0171-appearance-scroll");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();for(var control:Theme.manager().getControls()){editor.getControl(control.setting().getName());}check(true,"every theme setting remains accessible on the scrolling Appearance page");editor.reveal("Button Style");});c.waitTicks(12);c.takeScreenshot("0171-button-style-setting");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();var bounds=editor.getControl("Button Style").getBounds();check(editor.mouseClicked(bounds.centerX(),bounds.centerY(),0)&&Theme.manager().choice("Button Style",ThemeManager.ButtonStyle.class)==ThemeManager.ButtonStyle.ACCENT,"Appearance selector changes live button style");});c.waitTicks(8);c.takeScreenshot("0171-appearance-accent");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();var bounds=editor.getControl("Button Style").getBounds();check(editor.mouseClicked(bounds.centerX(),bounds.centerY(),1)&&Theme.manager().choice("Button Style",ThemeManager.ButtonStyle.class)==ThemeManager.ButtonStyle.SUBTLE,"Appearance selector restores subtle buttons");});
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.FRIENDS);check(gui.getSidebar().isExpanded(),"Utilities expands for Friends");});c.waitTicks(12);c.takeScreenshot("0171-utilities");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.APPEARANCE));
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();((dev.riserecode.ui.component.setting.ColorPicker)editor.getControl("Primary Accent")).setExpanded(true);editor.reveal("Primary Accent");});c.waitTicks(14);
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();var picker=(dev.riserecode.ui.component.setting.ColorPicker)editor.getControl("Primary Accent");var bounds=picker.getBounds();check(editor.mouseClicked(bounds.centerX(),bounds.y()+10,0)&&!picker.isExpanded(),"partially visible expanded color picker remains interactive");});
        c.runOnClient(mc->Client.INSTANCE.getClickGui().getThemeEditor().reveal("Opening Scale"));c.waitTicks(12);c.takeScreenshot("017-motion");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().getThemeEditor().reveal("Background"));c.waitTicks(12);c.takeScreenshot("017-background-browser");
        c.runOnClient(mc->{var parent=new OptionsScreen(new TitleScreen(),mc.options,false);var id=MenuTheme.pageId(parent);Theme.manager().page(id).theme.setValue(PageAppearance.ThemeMode.DISABLED);mc.gui.setScreen(parent);check(!MenuTheme.enabled(parent),"page theme disables native adapters");});c.waitTicks(12);c.takeScreenshot("017-options-native");
        c.runOnClient(mc->{var parent=mc.gui.screen();check(parent.children().stream().anyMatch(PageGear.class::isInstance),"disabled theme retains gear");mc.gui.setScreen(new PageAppearanceScreen(parent,MenuTheme.pageId(parent)));});c.waitTicks(15);c.takeScreenshot("017-page-gear");
        c.runOnClient(mc->{var parent=new OptionsScreen(new TitleScreen(),mc.options,false);Theme.manager().page(MenuTheme.pageId(parent)).theme.setValue(PageAppearance.ThemeMode.ENABLED);check(MenuTheme.enabled(parent),"page theme can be restored");mc.gui.setScreen(new TitleScreen());});
        for(var background:new ThemeManager.Background[]{ThemeManager.Background.ANIMATED_GRADIENT,ThemeManager.Background.AURORA,ThemeManager.Background.STARFIELD}){
            c.runOnClient(mc->{option("Background",background);option("Background Particles",ThemeManager.Particles.SNOW);});c.waitTicks(8);c.takeScreenshot("017-background-"+background.name().toLowerCase(java.util.Locale.ROOT));
        }
        c.runOnClient(mc->{option("Background Particles",ThemeManager.Particles.NONE);});
        if(Boolean.getBoolean("riserecode.ui017Only")){Client.INSTANCE.getLogger().info("Release 0.17 native UI checks passed ({} assertions).",checks);return;}
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            if(Boolean.getBoolean("riserecode.blur017Only")){benchmark(c,world);return;}
            fall(c,world,null);float baseline=world.getServer().computeOnServer(server->server.getPlayerList().getPlayers().getFirst().getHealth());check(baseline<20,"unprotected Survival fall damages server health");
            for(var mode:new NoFallModule.Mode[]{NoFallModule.Mode.PACKET,NoFallModule.Mode.WATER,NoFallModule.Mode.POWDER_SNOW,NoFallModule.Mode.COBWEB,NoFallModule.Mode.LADDER,NoFallModule.Mode.SLIME,NoFallModule.Mode.AUTO}){
                fall(c,world,mode);float hp=world.getServer().computeOnServer(server->server.getPlayerList().getPlayers().getFirst().getHealth());Client.INSTANCE.getLogger().info("NoFall {}: health={} status={}",mode,hp,c.computeOnClient(mc->noFall().status()));check(hp==20,"NoFall "+mode+" prevents authoritative fall damage (health="+hp+")");
                c.runOnClient(mc->check(mc.player.getInventory().getSelectedSlot()==0,"clutch restores slot for "+mode));
            }
            fall(c,world,NoFallModule.Mode.WATER,true,false);check(world.getServer().computeOnServer(server->server.getPlayerList().getPlayers().getFirst().getHealth())==20,"offhand water clutch protects health");
            fall(c,world,NoFallModule.Mode.WATER,false,true);check(world.getServer().computeOnServer(server->server.getPlayerList().getPlayers().getFirst().getHealth())<20,"missing item cannot report successful protection");
            c.runOnClient(mc->{dev.riserecode.config.SettingCodecs.apply(noFall().getMode(),new com.google.gson.JsonPrimitive("VANILLA"));check(noFall().getMode().getValue()==NoFallModule.Mode.AUTO,"legacy VANILLA mode migrates to Auto");noFall().setEnabled(false);});
            world.getServer().runCommand("tp @a 0.7 80 0.5 0 0");c.waitTicks(3);
            c.runOnClient(mc->{var n=noFall();n.getMode().setValue(NoFallModule.Mode.PACKET);n.setEnabled(true);mc.gui.setScreen(null);mc.player.setOnGround(false);mc.player.setDeltaMovement(0,-.5,0);mc.player.fallDistance=4;Client.INSTANCE.getEventBus().post(new UpdateEvent(mc,9000));
                var original=new ServerboundMovePlayerPacket.PosRot(7,80,-5,33,44,false,true);var event=Client.INSTANCE.getEventBus().post(new PacketEvent.Send(original));var rewritten=(ServerboundMovePlayerPacket)event.getPacket();check(rewritten.isOnGround()&&rewritten.getX(0)==7&&rewritten.getZ(0)==-5&&rewritten.getYRot(0)==33&&rewritten.horizontalCollision(),"packet mode preserves movement fields");check(!original.isOnGround(),"original packet stays immutable");
                for(var movement:new ServerboundMovePlayerPacket[]{new ServerboundMovePlayerPacket.Pos(4,5,6,false,true),new ServerboundMovePlayerPacket.Rot(37,51,false,true),new ServerboundMovePlayerPacket.StatusOnly(false,true)}){
                    var changed=(ServerboundMovePlayerPacket)Client.INSTANCE.getEventBus().post(new PacketEvent.Send(movement)).getPacket();check(changed.isOnGround()&&changed.type()==movement.type()&&changed.hasPosition()==movement.hasPosition()&&changed.hasRotation()==movement.hasRotation()&&changed.horizontalCollision(),"packet variant and collision flags preserved");
                }
                n.getMode().setValue(NoFallModule.Mode.AUTO);var normal=new ServerboundMovePlayerPacket.StatusOnly(false,false);check(Client.INSTANCE.getEventBus().post(new PacketEvent.Send(normal)).getPacket()==normal,"changing mode immediately clears packet policy");n.setEnabled(false);});
        }
        Client.INSTANCE.getLogger().info("Release 0.17 native checks passed ({} assertions).",checks);
    }
    private static void benchmark(ClientGameTestContext c,TestSingleplayerContext world){
        world.getServer().runCommand("gamemode spectator @a");world.getServer().runCommand("tp @a 0.5 70 -8.5 0 25");world.getServer().runCommand("fill -8 63 -8 8 63 8 minecraft:stone");
        c.runOnClient(mc->{mc.options.enableVsync().set(false);mc.options.framerateLimit().set(260);number("GUI Blur Radius",30);((BooleanSetting)Theme.manager().getSetting("World Blur")).setValue(false);Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COMBAT);});c.waitTicks(100);
        c.takeScreenshot("017-world-window-blur");
        c.runOnClient(mc->{number("World Blur Radius",12);((BooleanSetting)Theme.manager().getSetting("World Blur")).setValue(true);});c.waitTicks(20);c.takeScreenshot("017-world-full-blur");
        c.runOnClient(mc->{check(dev.riserecode.util.BlurUtil.getRenderedFrames()>10,"scene blur renders in-world");((BooleanSetting)Theme.manager().getSetting("World Blur")).setValue(false);});
        com.google.gson.JsonArray rows=new com.google.gson.JsonArray();
        for(int round=0;round<3;round++){
            c.waitTicks(20);double[] times=UpgradeBenchmark.measure(c,"DUAL_FILTER");com.google.gson.JsonObject row=new com.google.gson.JsonObject();row.addProperty("round",round);row.addProperty("medianMs",times[0]);row.addProperty("p95Ms",times[1]);rows.add(row);
        }
        try{var dir=java.nio.file.Path.of("../../verification/0.17.0");java.nio.file.Files.createDirectories(dir);java.nio.file.Files.writeString(dir.resolve("blur-"+System.getProperty("riserecode.benchmarkLabel","vanilla")+".json"),rows.toString());}catch(java.io.IOException error){throw new AssertionError(error);}
        Client.INSTANCE.getLogger().info("Release 0.17 blur benchmark completed");
    }
    private static NoFallModule noFall(){return Client.INSTANCE.getModuleManager().getByClass(NoFallModule.class).orElseThrow();}
    private static void fall(ClientGameTestContext c,TestSingleplayerContext world,NoFallModule.Mode mode){fall(c,world,mode,false,false);}
    private static void fall(ClientGameTestContext c,TestSingleplayerContext world,NoFallModule.Mode mode,boolean offhand,boolean missing){
        c.runOnClient(mc->{noFall().setEnabled(false);mc.gui.setScreen(null);});
        world.getServer().runCommand("gamemode creative @a");
        world.getServer().runCommand("tp @a 0.7 84 0.5 0 0");
        world.getServer().runCommand("fill -3 64 -3 3 95 3 minecraft:air");
        world.getServer().runCommand("fill -3 63 -3 3 63 3 minecraft:stone");
        world.getServer().runCommand("clear @a");world.getServer().runCommand("item replace entity @a hotbar.0 with minecraft:stick");
        String item=mode==null||mode==NoFallModule.Mode.PACKET?null:switch(mode){case WATER,AUTO->"water_bucket";case POWDER_SNOW->"powder_snow_bucket";case COBWEB->"cobweb";case LADDER->"ladder";case SLIME->"slime_block";default->null;};
        if(item!=null&&!missing){world.getServer().runCommand("item replace entity @a "+(offhand?"weapon.offhand":"hotbar.1")+" with minecraft:"+item+" 1");}
        if(mode==NoFallModule.Mode.LADDER){world.getServer().runCommand("fill 1 64 -1 1 85 1 minecraft:stone");}
        world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setHealth(20);p.getFoodData().setFoodLevel(6);p.getFoodData().setSaturation(0);p.fallDistance=0;p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);});
        c.waitTicks(4);
        c.runOnClient(mc->{mc.player.getInventory().setSelectedSlot(0);mc.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);if(mode!=null){noFall().getMode().setValue(mode);noFall().setEnabled(true);}});
        world.getServer().runCommand("gamemode survival @a");
        c.waitTicks(90);
        c.takeScreenshot("017-nofall-"+(mode==null?"baseline":mode.name().toLowerCase(java.util.Locale.ROOT)));
    }
}
