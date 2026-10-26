package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.event.BlockOutlineEvent;
import dev.riserecode.module.modules.BlockOverlayModule;
import dev.riserecode.module.modules.hud.CoordinatesModule;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.ButtonMetrics;
import dev.riserecode.ui.component.FocusedInput;
import dev.riserecode.ui.component.SettingsSection;
import dev.riserecode.ui.component.setting.StringControl;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.ui.sound.UiSounds;
import dev.riserecode.ui.theme.ModuleAppearance;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;

/** Real input, per-module rendering, native menu geometry and actual block shapes. */
public final class Release019IntegrationTest {
    private static int checks;
    private static final AtomicInteger clicks=new AtomicInteger();
    private Release019IntegrationTest(){}
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static ThemeManager themes(){return Client.INSTANCE.getThemeManager();}
    private static void number(String name,double value){((DoubleSetting)themes().getSetting(name)).setValue(value);themes().changed();}
    private static void bool(String name,boolean value){((BooleanSetting)themes().getSetting(name)).setValue(value);themes().changed();}
    private static void moduleNumber(ModuleAppearance a,String name,double value){((DoubleSetting)a.get(name)).setValue(value);}
    private static BlockOverlayModule overlay(){return Client.INSTANCE.getModuleManager().getByClass(BlockOverlayModule.class).orElseThrow();}
    private static void blockSetting(String name,Object value){dev.riserecode.config.SettingCodecs.apply(overlay().getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow(),new com.google.gson.Gson().toJsonTree(value));}
    private static void cursor(ClientGameTestContext c,double x,double y){c.getInput().setCursorPos(x*2,y*2);}
    private static final class Probe extends Screen {
        Button action;
        private Probe(){super(Component.literal("Compact action probe"));}
        @Override protected void init(){action=addRenderableWidget(Button.builder(Component.literal("Test action"),button->clicks.incrementAndGet()).bounds(width/2-100,height/2,200,20).build());}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
            super.extractRenderState(g,mx,my,partial);
            if(themes().existingModule("test.image.a")!=null){
                ModuleAppearance.with("test.image.a",()->dev.riserecode.ui.theme.ModuleBackground.draw(g,20,30,width/2.0-30,130,1));
                ModuleAppearance.with("test.image.b",()->dev.riserecode.ui.theme.ModuleBackground.draw(g,width/2.0+10,30,width/2.0-30,130,1));
            }
        }
    }
    private static final class ToolsProbe extends Screen {
        final dev.riserecode.ui.component.ResourcesEditor editor;
        private ToolsProbe(){super(Component.literal("Resource tools"));editor=new dev.riserecode.ui.component.ResourcesEditor(Client.INSTANCE.getResourcesManager(),ignored->{});}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){editor.setBounds(30,30,width-60,height-60);editor.render(g,font,mx,my,.05,1);}
    }
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);themes().reset();var ui=Client.INSTANCE.getConfigManager().getUiPreferences();ui.remove("sections");Client.INSTANCE.getConfigManager().setUiPreferences(ui);});
        c.setScreen(TitleScreen::new);c.waitTicks(12);c.takeScreenshot("019-title-compact");
        c.runOnClient(mc->{long total=mc.gui.screen().children().stream().filter(AbstractButton.class::isInstance).map(AbstractButton.class::cast).filter(b->!b.getMessage().getString().isBlank()).peek(b->check(b.getHeight()==ButtonMetrics.HEIGHT,"native title buttons use tab height")).count();check(total>=4,"title menu retains all buttons");});
        c.runOnClient(mc->mc.gui.setScreen(new OptionsScreen(new TitleScreen(),mc.options,false)));c.waitTicks(12);c.takeScreenshot("019-options-compact");
        var probe=c.computeOnClient(mc->new Probe());c.setScreen(()->probe);c.waitTicks(10);
        double[] p=c.computeOnClient(mc->new double[]{probe.action.getX()+probe.action.getWidth()/2.0,probe.action.getY()+8});cursor(c,p[0],p[1]);
        long sounds=c.computeOnClient(mc->UiSounds.dispatches());c.getInput().holdMouse(0);c.waitTicks(12);
        c.runOnClient(mc->{check(clicks.get()==1&&UiSounds.dispatches()==sounds+1,"held native action dispatches exactly once");check(MenuTheme.pressScale(probe.action)<.99,"native button stays down while held");});c.takeScreenshot("019-native-held");cursor(c,1,1);c.getInput().releaseMouse(0);c.waitTicks(16);
        c.runOnClient(mc->{check(Math.abs(MenuTheme.pressScale(probe.action)-1)<.001,"release outside restores native button");probe.action.setFocused(true);probe.action.keyPressed(new KeyEvent(InputConstants.KEY_RETURN,0,0));check(clicks.get()==2,"keyboard activation stays single dispatch");});
        if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium")){
            c.runOnClient(mc->{try{mc.gui.setScreen((Screen)Class.forName("net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen").getMethod("createScreen",Screen.class).invoke(null,new TitleScreen()));}catch(ReflectiveOperationException error){throw new AssertionError(error);}});c.waitTicks(14);c.takeScreenshot("019-sodium-compact");
            c.runOnClient(mc->{long count=mc.gui.screen().children().stream().filter(net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget.class::isInstance).map(net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget.class::cast).peek(b->check(b.getDimensions().height()==17,"Sodium action hitbox uses compact height")).count();check(count>0,"Sodium native actions remain available");});
        }
        c.runOnClient(mc->{Client.INSTANCE.getClickGui().open(mc.gui.screen());Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COMBAT);});c.waitTicks(20);
        double[] tab=c.computeOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getSidebar().getItemBounds(4);return new double[]{b.centerX()*gui.getCanvasScale(),b.centerY()*gui.getCanvasScale()};});cursor(c,tab[0],tab[1]);c.getInput().holdMouse(0);c.waitTicks(14);
        c.runOnClient(mc->{check(Client.INSTANCE.getClickGui().getPage()==ClickGui.Page.RENDER,"tab changes on press");check(Client.INSTANCE.getClickGui().getSidebar().pressScale(4)<.99,"tab stays down beyond the old timer");});c.takeScreenshot("019-tab-held");cursor(c,1,1);c.getInput().releaseMouse(0);c.waitTicks(16);
        c.runOnClient(mc->{check(Math.abs(Client.INSTANCE.getClickGui().getSidebar().pressScale(4)-1)<.001,"tab releases outside hitbox");check(Client.INSTANCE.getClickGui().getResizeHintOpacity()<.01,"resize hint hidden away from corner");});
        double[] corner=c.computeOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getWindowBounds();return new double[]{(b.x()+b.width()-6)*gui.getCanvasScale(),(b.y()+b.height()-6)*gui.getCanvasScale()};});cursor(c,corner[0],corner[1]);c.waitTicks(6);c.runOnClient(mc->check(Client.INSTANCE.getClickGui().getResizeHintOpacity()>.9,"resize hint appears near corner"));c.takeScreenshot("019-resize-hint");cursor(c,1,1);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.APPEARANCE);check(!gui.getThemeEditor().section("Typography").isExpanded(),"appearance sections initially collapsed");});c.waitTicks(10);c.takeScreenshot("019-collapsed-appearance");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().getThemeEditor().reveal("Font Scale"));c.waitTicks(18);c.runOnClient(mc->check(Client.INSTANCE.getClickGui().getThemeEditor().section("Typography").isExpanded(),"reveal opens the containing section"));c.takeScreenshot("019-expanded-typography");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();editor.reveal("Logo File");});c.waitTicks(18);
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getThemeEditor();var control=(StringControl)editor.getControl("Logo File");control.field().setFocused(true);check(FocusedInput.active(),"text owner is active before collapse");editor.section("Branding").setExpanded(false);check(!FocusedInput.active(),"collapsing commits and releases focused text");var reloaded=new SettingsSection("appearance:Typography","Typography",()->{});check(reloaded.isExpanded(),"section open state survives reconstruction");
            var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setValue("BlockOverlay");});c.waitTicks(12);
        c.runOnClient(mc->{var button=Client.INSTANCE.getClickGui().getModuleButton("BlockOverlay");var b=button.getBounds();button.mouseClicked(b.centerX(),b.y()+10,1);button.appearance().section().setExpanded(true);});c.waitTicks(18);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var panel=gui.getActivePanel();var appearance=gui.getModuleButton("BlockOverlay").appearance();double delta=appearance.getBounds().y()-panel.getBounds().y()-8;panel.mouseScrolled(panel.getBounds().centerX(),panel.getBounds().centerY(),-delta/24);});c.waitTicks(16);c.takeScreenshot("019-module-global");
        c.runOnClient(mc->{var local=Client.INSTANCE.getClickGui().getModuleButton("BlockOverlay").appearance().model();check(local.global.getValue()&&local.controls().stream().filter(t->t.setting()!=local.global).noneMatch(t->t.setting().isVisible()),"global mode hides local appearance controls");local.global.setValue(false);ThemeManager.assign(local.get("Primary Accent"),0xFFBB44CC);Client.INSTANCE.getClickGui().getModuleButton("BlockOverlay").appearance().editor().section("Colors & Glow").setExpanded(true);});c.waitTicks(20);c.takeScreenshot("019-module-custom");
        c.runOnClient(mc->{var local=themes().module(overlay());check(ModuleAppearance.call(overlay().capabilityId(),()->themes().number("Font Scale"))==1&&themes().number("Font Scale")==1,"world outline does not override fonts");local.global.setValue(true);local.global.setValue(false);check(((dev.riserecode.setting.ColorSetting)local.get("Primary Accent")).getValue()==0xFFBB44CC,"custom values survive inheritance toggle");});
        c.runOnClient(mc->{
            for(String pair:new String[]{"a:scenic","b:space"}){
                var parts=pair.split(":");var local=themes().module("test.image."+parts[0]);local.bind(new dev.riserecode.module.modules.hud.FpsModule(new dev.riserecode.event.EventBus(),mc));local.global.setValue(false);
                try(var stream=Release019IntegrationTest.class.getResourceAsStream("/assets/riserecode/textures/gui/"+parts[1]+".png")){
                    var source=themes().getDirectory().resolve("test-background-"+parts[0]+".png");java.nio.file.Files.copy(java.util.Objects.requireNonNull(stream),source,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    ThemeManager.assign(local.get("Background File"),dev.riserecode.ui.theme.BrandingLogo.importPng(themes().getDirectory(),source));ThemeManager.assign(local.get("Background"),ThemeManager.Background.LOCAL_IMAGE);moduleNumber(local,"GUI Blur Radius",parts[0].equals("a")?0:18);
                }catch(java.io.IOException error){throw new AssertionError(error);}
            }
            var first=ModuleAppearance.call("test.image.a",()->dev.riserecode.ui.menu.BackgroundAssets.asset(ThemeManager.Background.LOCAL_IMAGE));var second=ModuleAppearance.call("test.image.b",()->dev.riserecode.ui.menu.BackgroundAssets.asset(ThemeManager.Background.LOCAL_IMAGE));
            check(!first.equals(second),"imported module backgrounds have independent textures");check(first.equals(ModuleAppearance.call("test.image.a",()->dev.riserecode.ui.menu.BackgroundAssets.asset(ThemeManager.Background.LOCAL_IMAGE))),"drawing a second imported image preserves the first view");mc.gui.setScreen(new Probe());
        });c.waitTicks(12);c.takeScreenshot("019-independent-backgrounds");
        c.runOnClient(mc->{Client.INSTANCE.getClickGui().open(mc.gui.screen());Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.PROFILES);});c.waitTicks(12);
        var toolsProbe=c.computeOnClient(mc->new ToolsProbe());c.setScreen(()->toolsProbe);c.waitTicks(5);
        c.runOnClient(mc->{var button=toolsProbe.editor.getToolsButton();var b=button.getBounds();toolsProbe.editor.mouseClicked(b.centerX(),b.centerY(),0);dev.riserecode.ui.animation.ButtonFeedback.releaseAll();});c.waitTicks(8);
        c.runOnClient(mc->{check(!toolsProbe.editor.getToolsSection().isExpanded(),"Resources tools start collapsed");toolsProbe.editor.getToolsSection().setExpanded(true);});c.waitTicks(10);c.takeScreenshot("019-resource-tools-expanded");
        c.runOnClient(mc->{toolsProbe.editor.getToolsSection().setExpanded(false);check(!toolsProbe.editor.getToolsSection().isExpanded(),"Resources tools collapse independently");Client.INSTANCE.getClickGui().open(mc.gui.screen());Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COSMETICS);var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();String id=Client.INSTANCE.getCosmetics().library().definitions().stream().filter(d->d.category()==dev.riserecode.cosmetics.CosmeticCategory.CAPE).findFirst().orElseThrow().id();editor.select(id);});c.waitTicks(8);
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();var edit=editor.buttons().get(2).getBounds();editor.mouseClicked(edit.centerX(),edit.centerY(),0);editor.mouseReleased(edit.centerX(),edit.centerY(),0);dev.riserecode.ui.animation.ButtonFeedback.releaseAll();});c.waitTicks(6);
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();check(!editor.getItemSection().isExpanded(),"cosmetic settings start collapsed");editor.getItemSection().setExpanded(true);});c.waitTicks(10);c.takeScreenshot("019-cosmetic-settings-expanded");
        c.runOnClient(mc->{var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();editor.getItemSection().setExpanded(false);check(!editor.getItemSection().isExpanded(),"cosmetic settings collapse independently");Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.PROFILES);});
        c.runOnClient(mc->{number("Window Width",360);number("Window Height",240);number("Font Scale",1.5);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.PROFILES);});c.waitTicks(20);c.takeScreenshot("019-small-large-font");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();for(var b:gui.getActionButtons()){check(b.getBounds().height()==17,"utility action height stays uniform");check(b.getBounds().x()+b.getBounds().width()<=gui.getWindowBounds().x()+gui.getWindowBounds().width(),"wrapped action stays inside window");}themes().reset();mc.gui.setScreen(null);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -4 64 -2 4 64 6 stone");world.getServer().runCommand("tp @a 0.5 65 4.5 180 0");world.getServer().runCommand("setblock 0 66 0 stone");world.getServer().runCommand("setblock 2 66 0 stone");c.waitTicks(10);
            c.runOnClient(mc->{mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();overlay().setEnabled(true);blockSetting("Transition Duration",1.0);});world.getServer().runCommand("tp @a 0.5 65 4.5 180 0");c.waitTicks(3);c.runOnClient(mc->{mc.player.setDeltaMovement(0,0,0);check(mc.player.position().distanceTo(new net.minecraft.world.phys.Vec3(.5,65,4.5))<1,"block fixture starts within targeting reach");});c.getInput().lookAt(new BlockPos(0,66,0));c.waitFor(mc->mc.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(new BlockPos(0,66,0)),160);c.waitTicks(22);
            c.runOnClient(mc->{check(Client.INSTANCE.getWorldRenderer().getLines().size()==12,"cube emits twelve full edges");check(Client.INSTANCE.getWorldRenderer().getQuads().size()==6,"fill retains six native faces");check(Client.INSTANCE.getEventBus().post(new BlockOutlineEvent()).isCancelled(),"custom highlight suppresses duplicate vanilla edges");});c.takeScreenshot("019-block-full");
            c.getInput().lookAt(new BlockPos(2,66,0));
            c.waitFor(mc->{double min=Client.INSTANCE.getWorldRenderer().getLines().stream().mapToDouble(l->Math.min(l.from().x,l.to().x)).min().orElse(-10);return min>.001&&min<1.99;},160);
            c.runOnClient(mc->{double min=Client.INSTANCE.getWorldRenderer().getLines().stream().mapToDouble(l->Math.min(l.from().x,l.to().x)).min().orElseThrow();check(min>-.002&&min<1.99,"rendered highlight travels between actual blocks (minX="+min+")");});c.takeScreenshot("019-block-moving");c.waitTicks(24);
            c.runOnClient(mc->{blockSetting("Outline Style","CORNERS");blockSetting("Edge Gap",60.0);});c.waitTicks(2);c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().size()==24,"corner style splits every edge"));c.takeScreenshot("019-block-corners");
            c.runOnClient(mc->{blockSetting("See-through Outline",true);blockSetting("Pop Animation",true);blockSetting("Block Pop Duration",1.0);});c.getInput().lookAt(new BlockPos(0,66,0));
            c.waitFor(mc->Client.INSTANCE.getWorldRenderer().getLines().stream().anyMatch(line->!line.visibleOnly()),160);c.takeScreenshot("019-block-pop");c.waitTicks(24);
            c.runOnClient(mc->{check(Client.INSTANCE.getWorldRenderer().getLines().stream().allMatch(line->line.visibleOnly())&&Client.INSTANCE.getWorldRenderer().getQuads().stream().allMatch(face->face.visibleOnly()),"settled highlight restores normal depth testing");blockSetting("Smooth Switching",false);});
            c.getInput().lookAt(new BlockPos(2,66,0));c.waitFor(mc->Client.INSTANCE.getWorldRenderer().getQuads().stream().anyMatch(face->!face.visibleOnly()),160);c.takeScreenshot("019-block-pop-inside");
            c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().stream().anyMatch(line->!line.visibleOnly()),"outline and fill stay visible while growing inside an opaque block"));c.waitTicks(24);
            c.runOnClient(mc->{blockSetting("Smooth Switching",true);blockSetting("Pop Animation",false);});c.getInput().lookAt(new BlockPos(0,66,0));c.waitTicks(24);
            world.getServer().runCommand("setblock 0 66 0 oak_stairs[facing=north]");c.waitTicks(25);c.takeScreenshot("019-block-stairs");c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().size()>24,"stairs preserve their compound shape"));
            c.runOnClient(mc->{blockSetting("Edge Gap",100.0);});c.waitTicks(2);c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().isEmpty()&&!Client.INSTANCE.getWorldRenderer().getQuads().isEmpty(),"100 percent gap removes only edge segments"));
            c.runOnClient(mc->{blockSetting("Edge Gap",60.0);bool("Reduced Motion",true);});c.getInput().lookAt(new BlockPos(2,66,0));c.waitTicks(2);
            c.runOnClient(mc->{double min=Client.INSTANCE.getWorldRenderer().getLines().stream().mapToDouble(l->Math.min(l.from().x,l.to().x)).min().orElseThrow();check(Math.abs(min-1.998)<.001,"reduced motion renders the target immediately");overlay().setEnabled(false);check(!Client.INSTANCE.getEventBus().post(new BlockOutlineEvent()).isCancelled(),"disabling overlay restores vanilla outline");bool("Reduced Motion",false);});
            var coordinate=c.computeOnClient(mc->Client.INSTANCE.getModuleManager().getByClass(CoordinatesModule.class).orElseThrow());c.runOnClient(mc->coordinate.setEnabled(true));c.waitTicks(10);double original=c.computeOnClient(mc->Client.INSTANCE.getHudRenderer().getBoxes().stream().filter(b->b.element().module()==coordinate).findFirst().orElseThrow().height());
            c.runOnClient(mc->{var local=themes().module(coordinate);local.global.setValue(false);moduleNumber(local,"Font Scale",1.8);ThemeManager.assign(local.get("Background"),ThemeManager.Background.SCENIC);});c.waitTicks(8);c.runOnClient(mc->check(Client.INSTANCE.getHudRenderer().getBoxes().stream().filter(b->b.element().module()==coordinate).findFirst().orElseThrow().height()>original,"HUD editor bounds follow local font metrics"));c.takeScreenshot("019-module-hud");
            c.runOnClient(mc->{var array=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.hud.ArrayListModule.class).orElseThrow();var local=themes().module(array);local.global.setValue(false);ThemeManager.assign(local.get("HUD Visible"),false);});c.waitTicks(10);
            c.runOnClient(mc->check(Client.INSTANCE.getHudRenderer().getBoxes().stream().anyMatch(b->b.element().module()==coordinate),"one module's HUD visibility cannot hide other modules"));
            c.runOnClient(mc->mc.gui.setScreen(new dev.riserecode.ui.hud.HudEditorScreen(Client.INSTANCE.getHudRenderer())));c.waitTicks(6);c.takeScreenshot("019-hud-editor");
            c.runOnClient(mc->{var screen=(dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen();var box=Client.INSTANCE.getHudRenderer().getBoxes().stream().filter(b->b.element().module()==coordinate).findFirst().orElseThrow();screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(box.x()+2,box.y()+2,new net.minecraft.client.input.MouseButtonInfo(1,0)),false);});c.waitTicks(6);c.takeScreenshot("019-hud-settings-collapsed");
            c.runOnClient(mc->{var screen=(dev.riserecode.ui.hud.HudEditorScreen)mc.gui.screen();check(!screen.getSettingsSection().isExpanded(),"HUD settings start collapsed");screen.getSettingsSection().setExpanded(true);});c.waitTicks(10);c.takeScreenshot("019-hud-settings-expanded");
            c.runOnClient(mc->{themes().reset();coordinate.setEnabled(false);mc.gui.setScreen(null);});
        }
        Client.INSTANCE.getLogger().info("Release 0.19 native checks passed ({} assertions).",checks);
    }
}
