package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.mixin.ContainerScreenAccessor;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.menu.*;
import dev.riserecode.ui.theme.*;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Exercises the screenshot regression, native container lifetime and real footer input. */
public final class Release020IntegrationTest {
    private static int checks;
    private Release020IntegrationTest(){}
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static ThemeManager theme(){return Client.INSTANCE.getThemeManager();}
    private static void value(String name,Object value){var setting=theme().getSetting(name);if(setting instanceof dev.riserecode.setting.ColorSetting color&&value instanceof Number number){color.setValue(number.intValue());}else{ThemeManager.assign(setting,value);}theme().changed();}
    private static void cursor(ClientGameTestContext c,double x,double y){c.getInput().setCursorPos(x*2,y*2);}
    private static final class ParticleProbe extends Screen {
        private ParticleProbe(){super(Component.literal("Particle independence"));}
        @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float delta){g.fill(0,0,width,height,0xFF101010);}
    }
    private static int[] pixels(String name){
        try(var files=java.nio.file.Files.list(java.nio.file.Path.of("screenshots"))){
            var path=files.filter(p->p.getFileName().toString().contains("020-"+name+".png")).findFirst().orElseThrow();var image=javax.imageio.ImageIO.read(path.toFile());return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
        }catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static void shot(ClientGameTestContext c,String name){c.waitTicks(5);c.takeScreenshot("020-"+name);}
    private static void fixture(ClientGameTestContext c,String name,Function<Minecraft,Screen> factory){
        c.runOnClient(mc->mc.gui.setScreen(factory.apply(mc)));shot(c,name);
        c.runOnClient(mc->{check(mc.gui.screen() instanceof AbstractContainerScreen<?>,name+" is a live container");check(MenuTheme.enabled(),name+" uses risecode");check(mc.gui.screen().children().stream().anyMatch(PageGear.class::isInstance),name+" has style recovery gear");});
    }
    private static final class ButtonPopProbe extends Screen {
        private Button button; private double foregroundScale;
        private ButtonPopProbe(){super(Component.literal("Stationary screen"));}
        @Override protected void init(){button=addRenderableWidget(Button.builder(Component.literal("Button entrance"),b->{}).bounds(0,0,140,20).build());}
        @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float delta){foregroundScale=g.pose().m00();super.extractRenderState(g,mx,my,delta);}
    }
    private static void polishMenus(ClientGameTestContext c){
        c.runOnClient(mc->{theme().reset();value("Reduced Motion",false);value("Dialogs Animation Override",true);value("Dialogs Pop Duration",10.0);value("Dialogs Pop Scale",10.0);value("Glow Radius",80.0);mc.gui.setScreen(new ButtonPopProbe());});
        c.waitTicks(2);c.runOnClient(mc->{var screen=(ButtonPopProbe)mc.gui.screen();check(Math.abs(screen.foregroundScale-1)<.001&&MenuTheme.screenScale(screen)==1,"menu foreground stays stationary during entrance");check(MenuTheme.widgetScale(screen.button)<.99,"individual button has its own entrance pose");check(screen.button.isMouseOver(screen.button.getX()+screen.button.getWidth()/2.0,screen.button.getY()+screen.button.getHeight()/2.0),"animated button center remains clickable");});shot(c,"button-local-entrance-edge-glow");
        c.runOnClient(mc->{value("Reduced Motion",true);value("Particle Density",1536.0);value("Background Particles",ThemeManager.Particles.SNOW);value("GUI Blur Radius",128.0);mc.gui.setScreen(new TitleScreen());});shot(c,"extended-particle-blur-limits");
        c.runOnClient(mc->{theme().reset();value("Reduced Motion",true);Client.INSTANCE.getClickGui().open(mc.gui.screen());Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.APPEARANCE);});c.waitTicks(3);
        c.runOnClient(mc->{var section=Client.INSTANCE.getClickGui().getThemeEditor().section("Typography");check(section.getBounds().height()==dev.riserecode.ui.component.ModuleButton.HEADER_HEIGHT,"appearance headers match module cards");section.setExpanded(true);});shot(c,"appearance-module-cards");
        c.runOnClient(mc->{mc.gui.setScreen(null);theme().reset();});
    }
    private static void blockValue(String name,Object value){var module=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.BlockOverlayModule.class).orElseThrow();ThemeManager.assign(module.getSettings().stream().filter(s->s.getName().equals(name)).findFirst().orElseThrow(),value);}
    private static void polishOutline(ClientGameTestContext c,java.util.function.Consumer<String> command){
        c.runOnClient(mc->{mc.gui.setScreen(null);value("Reduced Motion",true);});command.accept("gamemode creative @a");command.accept("tp @a 0.5 65 4.5 180 0");command.accept("setblock 0 66 0 stone");command.accept("setblock 2 66 0 stone");c.waitTicks(5);
        c.runOnClient(mc->{mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();mc.player.setDeltaMovement(0,0,0);var module=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.BlockOverlayModule.class).orElseThrow();module.setEnabled(true);blockValue("OutlineWidth",32.0);blockValue("Smooth Switching",false);blockValue("See-through Outline",false);});
        c.getInput().lookAt(new net.minecraft.core.BlockPos(0,66,0));c.waitFor(mc->!Client.INSTANCE.getWorldRenderer().getLines().isEmpty(),160);
        c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().stream().allMatch(l->l.visibleOnly()&&l.width()==32),"maximum-width outline respects depth when disabled"));
        c.runOnClient(mc->blockValue("See-through Outline",true));c.waitTicks(2);c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().stream().allMatch(l->!l.visibleOnly()),"full-size outline remains see-through"));shot(c,"outline-through-all-edges");
        c.runOnClient(mc->{value("Reduced Motion",false);blockValue("Pop Animation",true);blockValue("Block Pop Duration",3.0);});c.getInput().lookAt(new net.minecraft.core.BlockPos(2,66,0));c.waitFor(mc->Client.INSTANCE.getWorldRenderer().getQuads().stream().anyMatch(q->!q.visibleOnly()),160);
        c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().stream().anyMatch(l->!l.visibleOnly()),"inside-block pop outline is see-through"));shot(c,"outline-see-through-pop");
        c.runOnClient(mc->{blockValue("See-through Outline",false);});c.waitTicks(2);c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().stream().allMatch(dev.riserecode.event.Render3DEvent.Line::visibleOnly),"disabling see-through restores depth during pop"));
        c.runOnClient(mc->{value("Reduced Motion",true);blockValue("See-through Outline",true);blockValue("Outline Style",dev.riserecode.module.modules.BlockOverlayModule.OutlineStyle.CORNERS);blockValue("Edge Gap",60.0);});c.waitTicks(2);
        c.runOnClient(mc->check(Client.INSTANCE.getWorldRenderer().getLines().size()==24,"corner-only depthless highlight retains all corner segments"));shot(c,"outline-corners-maximum-width");
        c.runOnClient(mc->{var module=Client.INSTANCE.getModuleManager().getByClass(dev.riserecode.module.modules.BlockOverlayModule.class).orElseThrow();module.setEnabled(false);module.getSettings().forEach(dev.riserecode.setting.Setting::reset);theme().reset();});
    }
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        polishMenus(c);
        c.runOnClient(mc->{mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.options.renderDistance().set(2);theme().reset();value("Reduced Motion",true);value("Background",ThemeManager.Background.NONE);value("Background Particles",ThemeManager.Particles.SNOW);value("Particle Opacity",.65);});
        c.setScreen(ParticleProbe::new);shot(c,"particles-before-background-edits");
        c.runOnClient(mc->{value("Background Opacity",0.0);value("Background Tint",0xFFFF0000);value("GUI Blur Radius",70.0);value("Background Motion Speed",4.0);value("Background Darkness",.8);});shot(c,"particles-after-background-edits");
        check(java.util.Arrays.equals(pixels("particles-before-background-edits"),pixels("particles-after-background-edits")),"rendered particles are identical after background opacity/tint/blur/motion/darkness changes");
        c.runOnClient(mc->{value("Background Opacity",1.0);value("Background Tint",0xFFFFFFFF);value("GUI Blur Radius",12.0);value("Background Motion Speed",1.0);value("Background Darkness",.42);});
        c.setScreen(TitleScreen::new);shot(c,"none-particles-title");
        c.runOnClient(mc->{Client.INSTANCE.getClickGui().open(mc.gui.screen());Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.COMBAT);});c.waitTicks(12);
        c.runOnClient(mc->{value("Reduced Motion",false);value("Client Animation Override",true);value("Client Pop Duration",10.0);value("Client Pop Scale",4.0);});
        double[] footer=c.computeOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getSettingsPopup().getBounds();return new double[]{b.centerX()*gui.getCanvasScale(),b.centerY()*gui.getCanvasScale()};});cursor(c,footer[0],footer[1]);c.getInput().holdMouse(0);c.getInput().releaseMouse(0);shot(c,"settings-popup");
        boolean suggestionsBefore=c.computeOnClient(mc->Client.INSTANCE.getClientPreferences().suggestions.getValue());
        double[] suggestionRow=c.computeOnClient(mc->{
            var gui=Client.INSTANCE.getClickGui();var popup=gui.getSettingsPopup();var logical=popup.popupBounds();var shown=popup.displayedPopupBounds();var row=popup.rowBounds(3);
            check(shown.width()<logical.width(),"popup scales while normal motion is enabled");
            check(shown.x()<logical.x()&&shown.y()+shown.height()>logical.y()+logical.height(),"popup enters from the bottom left");
            double scale=shown.width()/logical.width();return new double[]{(shown.x()+(row.centerX()-logical.x())*scale)*gui.getCanvasScale(),(shown.y()+(row.centerY()-logical.y())*scale)*gui.getCanvasScale()};
        });
        cursor(c,suggestionRow[0],suggestionRow[1]);c.getInput().holdMouse(0);c.getInput().releaseMouse(0);
        c.runOnClient(mc->{check(Client.INSTANCE.getClientPreferences().suggestions.getValue()!=suggestionsBefore,"animated popup rows use displayed mouse coordinates");Client.INSTANCE.getClientPreferences().suggestions.setValue(suggestionsBefore);value("Reduced Motion",true);value("Client Animation Override",false);theme().getSetting("Client Pop Duration").reset();theme().getSetting("Client Pop Scale").reset();});
        c.waitTicks(2);
        c.runOnClient(mc->{var popup=Client.INSTANCE.getClickGui().getSettingsPopup();check(popup.displayedPopupBounds().equals(popup.popupBounds()),"Reduced Motion removes popup scale and directional movement");});shot(c,"settings-popup-reduced-motion");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var popup=gui.getSettingsPopup();check(popup.isOpen(),"real footer click opens popup");var b=popup.popupBounds();check(b.x()>=0&&b.y()>=0&&(b.x()+b.width())*gui.getCanvasScale()<=mc.getWindow().getGuiScaledWidth(),"popup stays inside screen");check(!ClickGui.NAVIGATION_PAGES.contains(ClickGui.Page.PROFILES)&&!ClickGui.NAVIGATION_PAGES.contains(ClickGui.Page.FRIENDS),"friends and profiles moved from Utilities");popup.activate(0,false);});shot(c,"profiles");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();check(gui.getPage()==ClickGui.Page.PROFILES&&!gui.getActionButtons().isEmpty(),"profiles page still works");gui.getSettingsPopup().activate(1,false);});shot(c,"friends");
        c.runOnClient(mc->{check(Client.INSTANCE.getClickGui().getPage()==ClickGui.Page.FRIENDS,"friends page still works");Client.INSTANCE.getClickGui().getSettingsPopup().activate(2,true);});shot(c,"sounds");
        c.runOnClient(mc->{check(mc.gui.screen() instanceof ClickGui gui&&gui.getPage()==ClickGui.Page.CLIENT_SOUNDS,"sounds settings stay inside client");var prefs=Client.INSTANCE.getClientPreferences();long before=dev.riserecode.ui.sound.UiSounds.dispatches();prefs.sounds.setValue(false);dev.riserecode.ui.sound.UiSounds.play();check(dev.riserecode.ui.sound.UiSounds.dispatches()==before,"master mute prevents dispatch");prefs.sounds.setValue(true);Client.INSTANCE.getClickGui().getSettingsPopup().activate(4,true);});shot(c,"discord-setup");
        c.runOnClient(mc->{check(Client.INSTANCE.getDiscordActivity().status().equals("Off"),"Discord defaults off");var gui=Client.INSTANCE.getClickGui();gui.getSettingsPopup().activate(6,false);});shot(c,"scale");
        c.runOnClient(mc->{check(mc.gui.screen() instanceof ClickGui gui&&gui.getPage()==ClickGui.Page.GUI_SCALE,"scale settings stay inside client");var keys=Client.INSTANCE.getKeybindManager();keys.beginMenuCapture();});c.getInput().pressKey(InputConstants.KEY_F8);c.waitTicks(3);
        c.runOnClient(mc->{check(Client.INSTANCE.getKeybindManager().getClickGuiKey().matches(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_F8,0,0)),"menu key capture updates actual mapping");var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setFocused(true);gui.getSearchField().setValue("search active");});c.getInput().pressKey(InputConstants.KEY_F8);c.waitFor(mc->!(mc.gui.screen() instanceof ClickGui),240);
        c.runOnClient(mc->{check(!(mc.gui.screen() instanceof ClickGui),"menu closes while search is focused");Client.INSTANCE.getKeybindManager().setMenuKey(InputConstants.KEY_RSHIFT);mc.gui.setScreen(null);});
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @a");world.getServer().runCommand("give @a diamond 16");world.getServer().runCommand("give @a oak_log 16");world.getServer().runCommand("give @a iron_pickaxe");world.getServer().runCommand("recipe give @a *");c.waitTicks(10);
            c.runOnClient(mc->{mc.gui.setScreen(new InventoryScreen(mc.player));});shot(c,"inventory-risecode");
            c.runOnClient(mc->{check(MenuTheme.enabled(),"inventory defaults to risecode");check(ContainerTheme.surfaces()>0,"native panel extraction reached themed surfaces");var screen=(InventoryScreen)mc.gui.screen();var toggle=screen.children().stream().filter(net.minecraft.client.gui.components.ImageButton.class::isInstance).map(net.minecraft.client.gui.components.ImageButton.class::cast).findFirst().orElseThrow();toggle.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN,0,0));});shot(c,"inventory-recipes-risecode");
            c.runOnClient(mc->{var screen=(InventoryScreen)mc.gui.screen();mc.player.inventoryMenu.setCarried(new ItemStack(Items.GOLD_INGOT,4));PageAppearanceScreen.open(screen);check(mc.player.inventoryMenu.getCarried().getCount()==4,"opening gear preserves carried stack");});shot(c,"container-gear");
            c.runOnClient(mc->{check(mc.gui.screen() instanceof PageAppearanceScreen,"container editor opens");theme().page(InventoryScreen.class.getName()).style.setValue(PageAppearance.ScreenStyle.MINECRAFT);mc.gui.screen().onClose();check(mc.player.inventoryMenu.getCarried().getCount()==4,"returning from gear preserves carried stack");mc.player.inventoryMenu.setCarried(ItemStack.EMPTY);check(!MenuTheme.enabled(),"per-screen Minecraft style overrides risecode");});shot(c,"inventory-minecraft");
            c.runOnClient(mc->{theme().page(InventoryScreen.class.getName()).style.setValue(PageAppearance.ScreenStyle.RISECODE);value("Background Opacity",0.0);value("GUI Blur Radius",70.0);value("Background Darkness",.8);});shot(c,"particles-with-zero-background");
            c.runOnClient(mc->{value("Background",ThemeManager.Background.SPACE);value("Background Opacity",.65);});shot(c,"inventory-image-particles");
            c.runOnClient(mc->{value("Background",ThemeManager.Background.NONE);value("Background Opacity",1.0);value("GUI Blur Radius",12.0);value("Background Darkness",.42);});
            double[] diamond=c.computeOnClient(mc->{var screen=(InventoryScreen)mc.gui.screen();var b=(ContainerScreenAccessor)screen;var slot=screen.getMenu().slots.stream().filter(t->t.getItem().is(Items.DIAMOND)).findFirst().orElseThrow();return new double[]{b.risecode$left()+slot.x+8,b.risecode$top()+slot.y+8};});
            cursor(c,diamond[0],diamond[1]);c.getInput().holdMouse(0);c.getInput().releaseMouse(0);c.waitTicks(3);
            c.runOnClient(mc->check(mc.player.inventoryMenu.getCarried().is(Items.DIAMOND),"themed inventory still picks up items through native input"));
            double[] destination=c.computeOnClient(mc->{var screen=(InventoryScreen)mc.gui.screen();var b=(ContainerScreenAccessor)screen;var slot=screen.getMenu().slots.get(9);return new double[]{b.risecode$left()+slot.x+8,b.risecode$top()+slot.y+8};});
            cursor(c,destination[0],destination[1]);c.getInput().holdMouse(0);c.getInput().releaseMouse(0);c.waitTicks(3);c.runOnClient(mc->check(mc.player.inventoryMenu.getCarried().isEmpty(),"themed inventory places carried items into another slot"));
            c.runOnClient(mc->{var event=new net.minecraft.client.input.MouseButtonEvent(destination[0],destination[1],new net.minecraft.client.input.MouseButtonInfo(0,1));mc.gui.screen().mouseClicked(event,false);mc.gui.screen().mouseReleased(event);});c.waitTicks(3);
            c.runOnClient(mc->check(mc.player.inventoryMenu.slots.stream().filter(slot->slot.getItem().is(Items.DIAMOND)).mapToInt(slot->slot.getItem().getCount()).sum()==16,"shift-click preserves inventory items"));
            fixture(c,"chest",mc->new ContainerScreen(ChestMenu.threeRows(1,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Chest")));
            fixture(c,"crafting",mc->new CraftingScreen(new CraftingMenu(2,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Crafting")));
            fixture(c,"furnace",mc->new FurnaceScreen(new FurnaceMenu(3,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Furnace")));
            fixture(c,"brewing",mc->new BrewingStandScreen(new BrewingStandMenu(4,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Brewing")));
            fixture(c,"loom",mc->new LoomScreen(new LoomMenu(5,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Loom")));
            fixture(c,"stonecutter",mc->new StonecutterScreen(new StonecutterMenu(6,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Stonecutter")));
            fixture(c,"anvil",mc->new AnvilScreen(new AnvilMenu(7,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Anvil")));
            fixture(c,"enchanting",mc->new EnchantmentScreen(new EnchantmentMenu(8,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Enchanting")));
            fixture(c,"trading",mc->new MerchantScreen(new MerchantMenu(9,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Trading")));
            fixture(c,"smithing",mc->new SmithingScreen(new SmithingMenu(10,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Smithing")));
            fixture(c,"large-chest",mc->new ContainerScreen(ChestMenu.sixRows(11,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Large chest")));
            fixture(c,"hopper",mc->new HopperScreen(new HopperMenu(12,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Hopper")));
            fixture(c,"dispenser",mc->new DispenserScreen(new DispenserMenu(13,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Dispenser")));
            fixture(c,"shulker",mc->new ShulkerBoxScreen(new ShulkerBoxMenu(14,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Shulker box")));
            fixture(c,"cartography",mc->new CartographyTableScreen(new CartographyTableMenu(15,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Cartography")));
            fixture(c,"grindstone",mc->new GrindstoneScreen(new GrindstoneMenu(16,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Grindstone")));
            fixture(c,"crafter",mc->new CrafterScreen(new CrafterMenu(17,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Crafter")));
            fixture(c,"beacon",mc->new BeaconScreen(new BeaconMenu(18,new net.minecraft.world.SimpleContainer(1)),mc.player.getInventory(),Component.literal("Beacon")));
            fixture(c,"blast-furnace",mc->new BlastFurnaceScreen(new BlastFurnaceMenu(19,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Blast furnace")));
            fixture(c,"smoker",mc->new SmokerScreen(new SmokerMenu(20,mc.player.getInventory()),mc.player.getInventory(),Component.literal("Smoker")));
            fixture(c,"horse",mc->{var horse=new net.minecraft.world.entity.animal.equine.Horse(net.minecraft.world.entity.EntityTypes.HORSE,mc.player.level());horse.setId(13371337);return new HorseInventoryScreen(new HorseInventoryMenu(21,mc.player.getInventory(),new net.minecraft.world.SimpleContainer(0),horse,0),mc.player.getInventory(),horse,0);});
            c.runOnClient(mc->{mc.gui.setScreen(null);theme().page(InventoryScreen.class.getName()).style.setValue(PageAppearance.ScreenStyle.INHERIT);value("Container Style",ThemeManager.ContainerStyle.MINECRAFT);mc.gui.setScreen(new InventoryScreen(mc.player));check(!MenuTheme.enabled(),"global Minecraft style works");});shot(c,"global-minecraft");
            world.getServer().runCommand("gamemode creative @a");c.waitTicks(5);
            c.runOnClient(mc->{value("Container Style",ThemeManager.ContainerStyle.RISECODE);mc.gui.setScreen(new CreativeModeInventoryScreen(mc.player,mc.player.level().enabledFeatures(),false));});shot(c,"creative-risecode");
            c.runOnClient(mc->{mc.gui.setScreen(new ChatScreen(".tog",false));});shot(c,"command-suggestions");
            c.runOnClient(mc->{Client.INSTANCE.getClientPreferences().suggestions.setValue(false);mc.gui.setScreen(new ChatScreen(".tog",false));});shot(c,"command-suggestions-off");
            c.runOnClient(mc->{Client.INSTANCE.getClientPreferences().suggestions.setValue(true);mc.gui.setScreen(null);Client.INSTANCE.getClickGui().open(null);var gui=Client.INSTANCE.getClickGui();value("Window Width",360.0);value("Window Height",240.0);value("Interface Scale",1.3);gui.selectPage(ClickGui.Page.COMBAT);gui.getSettingsPopup().toggle();});shot(c,"small-window-popup");
            c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getSettingsPopup().popupBounds();check(b.x()>=0&&b.y()>=0,"small-window popup stays on screen");gui.getSettingsPopup().close();mc.gui.setScreen(null);theme().reset();});
            polishOutline(c,world.getServer()::runCommand);
        }
        Client.INSTANCE.getLogger().info("Release 0.20 native checks passed ({} assertions)",checks);
    }
}
