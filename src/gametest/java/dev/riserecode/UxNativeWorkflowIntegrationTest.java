package dev.riserecode;

import dev.riserecode.map.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.component.*;
import dev.riserecode.ui.component.setting.StringControl;
import dev.riserecode.ui.hud.HudEditorScreen;
import dev.riserecode.ui.menu.*;
import dev.riserecode.ui.theme.*;
import dev.riserecode.testmixin.KeyboardInputInvoker;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;

/** Actual native gears, retained failures, world-backed map tasks and modifier-down callbacks. */
public final class UxNativeWorkflowIntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void key(ClientGameTestContext c,int code){c.getInput().pressKey(code);c.waitTicks(2);}
    private static Object field(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static String focused(net.minecraft.client.Minecraft mc){
        var screen=mc.gui.screen();if(screen instanceof WorldMapScreen){return ((KeyboardNavigation)field(screen,"navigation")).focusedLabel();}
        if(screen instanceof KeyboardNavigation.ManagedScreen managed){String label=managed.riserecode$keyboardNavigation().focusedLabel();if(!label.isEmpty())return label;}
        return screen.getFocused() instanceof AbstractWidget widget?widget.getMessage().getString():"";
    }
    private static void focus(ClientGameTestContext c,String prefix){for(int i=0;i<200;i++){if(c.computeOnClient(mc->focused(mc).startsWith(prefix)))return;key(c,258);}throw new AssertionError("Cannot reach "+prefix+"; focused="+c.computeOnClient(UxNativeWorkflowIntegrationTest::focused));}
    private static void press(ClientGameTestContext c,String prefix){focus(c,prefix);key(c,257);}
    private static void callback(net.minecraft.client.Minecraft mc,int key,int modifiers,int action){((KeyboardInputInvoker)mc.keyboardHandler).riserecode$press(mc.getWindow().handle(),action,new KeyEvent(key,0,modifiers));}
    private static void replace(ClientGameTestContext c,String value){c.runOnClient(mc->{callback(mc,65,net.minecraft.client.input.InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER,1);callback(mc,65,net.minecraft.client.input.InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER,0);});if(value.isEmpty()){key(c,259);}else{c.getInput().typeChars(value);}c.waitTicks(2);}
    private static void pointer(ClientGameTestContext c,UIComponent.Bounds b){double[] xy=c.computeOnClient(mc->{double x=b.centerX(),y=b.centerY();if(mc.gui.screen() instanceof PageAppearanceScreen page){double scale=(double)field(page,"visualScale");x=page.width-16+(x-page.width+16)*scale;y=16+(y-16)*scale;}return new double[]{x*mc.getWindow().getScreenWidth()/mc.gui.screen().width,y*mc.getWindow().getScreenHeight()/mc.gui.screen().height};});c.getInput().setCursorPos(xy[0],xy[1]);c.waitTick();c.getInput().pressMouse(0);c.waitTicks(3);}
    private static PageAppearanceScreen page(net.minecraft.client.Minecraft mc){return (PageAppearanceScreen)mc.gui.screen();}
    private static void number(String key,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(key)).setValue(value);}
    private static void nativeBounds(ClientGameTestContext c,String name){c.runOnClient(mc->{int count=0;for(var child:mc.gui.screen().children()){if(child instanceof AbstractButton w&&w.visible){count++;check(w.getX()>=0&&w.getY()>=0&&w.getX()+w.getWidth()<=mc.gui.screen().width&&w.getY()+w.getHeight()<=mc.gui.screen().height,name+": actual native button remains inside window: "+w.getMessage().getString());}}check(count>0,name+": actual native buttons inspected");});}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();Client.INSTANCE.getThemeManager().reset();((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);});
        for(int[] size:new int[][]{{960,720,3},{1440,900,3},{1920,1080,2}}){c.getInput().resizeWindow(size[0],size[1]);c.runOnClient(mc->{mc.options.guiScale().set(size[2]);number("Font Scale",size[2]==3?2:1);mc.gui.setScreen(new OptionsScreen(new TitleScreen(),mc.options,false));});c.waitTicks(5);nativeBounds(c,"Options "+size[0]);c.takeScreenshot("ux9-native-options-"+size[0]);press(c,"Done");c.runOnClient(mc->check(mc.gui.screen() instanceof TitleScreen,"Native Options Done returns to caller"));}
        c.getInput().resizeWindow(960,720);c.runOnClient(mc->{mc.options.guiScale().set(3);number("Font Scale",2);mc.gui.setScreen(new JoinMultiplayerScreen(new TitleScreen()));});c.waitTicks(8);nativeBounds(c,"Multiplayer");c.takeScreenshot("ux9-native-multiplayer");key(c,256);c.runOnClient(mc->check(mc.gui.screen() instanceof TitleScreen,"Multiplayer Escape returns to caller without connection"));
        pageFailure(c,false,"title");pageEntrance(c);
        c.runOnClient(mc->{var map=new WorldMapScreen(mc.gui.screen(),Client.INSTANCE.getMaps());check(map.helpText().contains("Offline:")&&!map.helpText().contains("Connected: Add"),"Offline map Help explains unavailable Add");mc.gui.setScreen(map);});c.waitTicks(4);press(c,"Help");c.runOnClient(mc->check(((InfoScreen)mc.gui.screen()).text().contains("Offline:"),"Visible keyboard Help opens offline guide"));key(c,256);key(c,256);
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runOnServer(server->{check(server.getPlayerList().getPlayers().stream().allMatch(p->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(p.nameAndId())),"Disposable world has only its owner");CameraFovClientTest.ownerOnlyShutdown=true;});
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");c.waitFor(mc->Client.INSTANCE.getMaps().ready(),600);
            hudModifiers(c);
            for(boolean compact:new boolean[]{true,false}){c.getInput().resizeWindow(compact?960:1600,compact?720:1000);c.runOnClient(mc->{mc.options.guiScale().set(compact?3:2);number("Font Scale",compact?2:1);});mapJourney(c,compact);}
            c.getInput().resizeWindow(960,720);c.runOnClient(mc->{mc.options.guiScale().set(3);number("Font Scale",2);mc.gui.setScreen(null);});
            openChest(c,world);var chest=c.computeOnClient(mc->(AbstractContainerScreen<?>)mc.gui.screen());int id=chest.getMenu().containerId;
            pageFailure(c,true,"container");c.runOnClient(mc->check(mc.gui.screen()==chest&&mc.player.containerMenu==chest.getMenu()&&mc.player.containerMenu.containerId==id&&mc.player.containerMenu.getCarried().is(net.minecraft.world.item.Items.DIAMOND)&&mc.player.containerMenu.getCarried().getCount()==3,"Gear/details/choice return retains actual live chest and carried stack"));
            openPage(c);c.runOnClient(mc->{page(mc).editor().getAction("Import Image").activate();page(mc).details().activate();});c.waitTicks(3);world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().closeContainer());c.waitFor(mc->mc.gui.screen()==null&&mc.player.containerMenu==mc.player.inventoryMenu,200);c.runOnClient(mc->check(!PageAppearanceScreen.suspends(chest),"Server close while details is nested clears suspension and child"));
            openChest(c,world);var abandoned=c.computeOnClient(mc->(AbstractContainerScreen<?>)mc.gui.screen());openPage(c);c.runOnClient(mc->{page(mc).editor().getAction("Import Image").activate();page(mc).details().activate();});c.waitTicks(2);c.runOnClient(mc->mc.gui.setScreen(new PauseScreen(true)));c.waitTicks(4);c.runOnClient(mc->check(!PageAppearanceScreen.suspends(abandoned)&&mc.player.containerMenu==mc.player.inventoryMenu,"Abandoned retained details releases suspended owner and live client menu"));
            world.getServer().runOnServer(server->{var player=server.getPlayerList().getPlayers().getFirst();check(player.containerMenu==player.inventoryMenu,"Abandonment close reaches server and returns carried stack ownership");});c.runOnClient(mc->mc.gui.setScreen(null));
        }finally{CameraFovClientTest.ownerOnlyShutdown=false;}
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});Client.INSTANCE.getLogger().info("UX native page container and map workflow checks passed: {}",checks);
    }
    private static void openPage(ClientGameTestContext c){c.waitTicks(3);var b=c.computeOnClient(mc->{var gear=mc.gui.screen().children().stream().filter(PageGear.class::isInstance).map(PageGear.class::cast).findFirst().orElseThrow();return new UIComponent.Bounds(gear.getX(),gear.getY(),gear.getWidth(),gear.getHeight());});pointer(c,b);c.runOnClient(mc->check(mc.gui.screen() instanceof PageAppearanceScreen,"Actual native gear opens page editor through mouse callback"));}
    private static void pageFailure(ClientGameTestContext c,boolean disabled,String tag){
        var caller=c.computeOnClient(mc->mc.gui.screen());String target=MenuTheme.pageId(caller);c.runOnClient(mc->{var p=Client.INSTANCE.getThemeManager().page(target);p.inherit.setValue(false);if(disabled)p.style.setValue(PageAppearance.ScreenStyle.MINECRAFT);});openPage(c);
        c.runOnClient(mc->page(mc).editor().reveal("Background File"));c.waitTicks(3);focus(c,"Background File,");key(c,257);String requested="/tmp/ux-page-"+"long_path_segment_".repeat(18)+"missing.png";replace(c,requested);press(c,"Import Image");c.runOnClient(mc->{check(page(mc).message().contains(requested)&&page(mc).message().contains("Choose a PNG"),"Page failure retains exact path and actionable cause");check(((StringControl)page(mc).editor().getControl("Background File")).field().getValue().equals(requested),"Failed import retains editable path");});c.takeScreenshot("ux9-page-error-"+tag);
        press(c,"Page status,");c.runOnClient(mc->check(((InfoScreen)mc.gui.screen()).text().contains(requested),"Page status opens complete retained error by Tab Enter"));for(int i=0;i<40&&c.computeOnClient(mc->((InfoScreen)mc.gui.screen()).scrollPosition()<((InfoScreen)mc.gui.screen()).maximumScroll());i++)key(c,267);c.takeScreenshot("ux9-page-error-final-"+tag);key(c,256);c.runOnClient(mc->{check(((StringControl)page(mc).editor().getControl("Background File")).field().getValue().equals(requested),"Details return preserves path");check(focused(mc).startsWith("Page status,"),"Details return retains status opener focus");});
        var status=c.computeOnClient(mc->page(mc).details().getBounds());pointer(c,status);c.runOnClient(mc->check(mc.gui.screen() instanceof InfoScreen,"Page full details reachable through actual pointer callback"));key(c,256);
        c.runOnClient(mc->{var e=page(mc).editor();e.reveal("Background File");});c.waitTicks(3);focus(c,"Background File,");key(c,257);Path png=Path.of("/tmp/ux9-valid-page.png");try{javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_ARGB),"png",png.toFile());}catch(Exception e){throw new AssertionError(e);}replace(c,png.toString());press(c,"Import Image");c.runOnClient(mc->check(page(mc).message().startsWith("Image imported"),"Correcting retained path successfully imports for page"));
        c.runOnClient(mc->{page(mc).editor().reveal("Background");});c.waitTicks(3);focus(c,"Background,");key(c,257);c.runOnClient(mc->check(mc.gui.screen() instanceof ChoiceScreen,"Page settings opens nested choice after details recovery"));key(c,256);key(c,256);c.runOnClient(mc->check(mc.gui.screen()==caller,"Page editor returns to original caller including disabled page styling"));
    }
    private static void pageEntrance(ClientGameTestContext c){
        c.runOnClient(mc->{((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(false);number("Opening Duration",1);});
        openPage(c);key(c,258);
        c.runOnClient(mc->check(!focused(mc).isEmpty()&&(double)field(mc.gui.screen(),"visualScale")<1,"Page keyboard focus is available before slow entrance settles"));
        c.takeScreenshot("ux9-page-early-input");
        var done=c.computeOnClient(mc->((ActionButton)field(mc.gui.screen(),"done")).getBounds());pointer(c,done);
        c.runOnClient(mc->{check(mc.gui.screen() instanceof TitleScreen,"Displayed page Done pointer target accepts input during entrance");((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);number("Opening Duration",.2);});
    }
    private static void hudModifiers(ClientGameTestContext c){
        c.runOnClient(mc->{((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(false);number("Expansion Duration",1);mc.gui.setScreen(new PauseScreen(true));Client.INSTANCE.openHudEditor();});c.waitTicks(4);focus(c,"HUD element,");key(c,257);c.runOnClient(mc->{check(((HudEditorScreen)mc.gui.screen()).getSelected()!=null&&!FocusedInput.active(),"HUD element selected without text input");double opening=((dev.riserecode.ui.animation.Animation)field(mc.gui.screen(),"contextOpening")).getValue();check(opening>0&&opening<1,"HUD keyboard selection is usable during slow inspector entrance");});c.takeScreenshot("ux9-hud-early-inspector");
        for(int ctrl:new int[]{341,345})for(int shift:new int[]{340,344})for(boolean shiftFirst:new boolean[]{false,true}){
            c.runOnClient(mc->((HudEditorScreen)mc.gui.screen()).getSelected().layout().position(.4,.4));c.getInput().holdKey(shiftFirst?shift:ctrl);c.getInput().holdKey(shiftFirst?ctrl:shift);c.waitTick();c.runOnClient(mc->{check(mc.gui.screen() instanceof HudEditorScreen,"Both modifier-down events preserve HUD: "+ctrl+"/"+shift+" order "+shiftFirst);callback(mc,262,3,1);callback(mc,262,3,0);});c.getInput().releaseKey(ctrl);c.getInput().releaseKey(shift);c.waitTicks(2);c.runOnClient(mc->{var editor=(HudEditorScreen)mc.gui.screen();check(Math.abs((editor.getSelected().layout().x()-.4)*editor.width-10)<.001,"Native callback moves exactly ten pixels for both modifier sides/orders");});
        }
        c.runOnClient(mc->{var e=(HudEditorScreen)mc.gui.screen();e.getSelected().layout().position(.4,.4);callback(mc,262,2,1);callback(mc,262,2,0);check(Math.abs((e.getSelected().layout().x()-.4)*e.width-1)<.001,"Ctrl arrow remains one-pixel movement");});c.takeScreenshot("ux9-hud-both-modifier-sides");key(c,256);c.runOnClient(mc->{check(mc.gui.screen() instanceof PauseScreen,"HUD Escape returns after modifier use");((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);});
    }
    private static double[] view(WorldMapScreen map){return new double[]{(double)field(map,"centerX"),(double)field(map,"centerZ"),(double)field(map,"zoom")};}
    private static void mapJourney(ClientGameTestContext c,boolean compact){
        c.runOnClient(mc->mc.gui.setScreen(new WorldMapScreen(new PauseScreen(true),Client.INSTANCE.getMaps())));
        c.waitTicks(6);
        var map=c.computeOnClient(mc->(WorldMapScreen)mc.gui.screen());
        double[] before=c.computeOnClient(mc->view(map));
        mapLayout(c,map);press(c,"Panel");c.waitTicks(2);mapLayout(c,map);c.takeScreenshot("ux9-map-panel-"+compact);press(c,"Fullscreen");c.waitTicks(2);
        press(c,"Help");
        c.runOnClient(mc->{
            String help=((InfoScreen)mc.gui.screen()).text();
            check(help.contains("Arrows: pan")&&help.contains("Connected: Add")&&help.contains("Cancel leaves"),"Map Help teaches connected keyboard creation and cancellation");
        });
        key(c,256);key(c,262);key(c,61);
        c.runOnClient(mc->check((double)field(map,"targetX")>before[0]&&(double)field(map,"targetZoom")>before[2],"Native arrow and plus explore the map"));
        c.waitTicks(4);
        double[] explored=c.computeOnClient(mc->view(map));
        int count=Client.INSTANCE.getMaps().waypoints().all().size();
        press(c,"Add");
        c.runOnClient(mc->check(mc.gui.screen() instanceof WaypointEditScreen&&Client.INSTANCE.getMaps().waypoints().all().size()==count,"Add opens uncommitted destination at viewed center"));
        c.runOnClient(mc->check(focused(mc).equals("Name"),"Keyboard Add starts on the waypoint Name field"));focus(c,"Name");replace(c,"");press(c,"Save");
        c.runOnClient(mc->{
            check(mc.gui.screen() instanceof WaypointEditScreen&&Client.INSTANCE.getMaps().waypoints().all().size()==count,"Invalid empty name stays editable without adding waypoint");
            check(field(mc.gui.screen(),"error").toString().startsWith("Name"),"Empty-name validation owns Name field: "+field(mc.gui.screen(),"error"));
        });
        String name="UX Map "+(compact?"compact":"normal");
        focus(c,"Name");replace(c,name);
        c.runOnClient(mc->check(((EditBox)mc.gui.screen().getFocused()).getValue().equals(name),"Native typing sets waypoint name before coordinate validation"));
        focus(c,"X");replace(c,"bad");press(c,"Save");
        c.runOnClient(mc->check(mc.gui.screen() instanceof WaypointEditScreen&&focused(mc).equals("X"),"Invalid coordinate returns focus to the owning field"));
        replace(c,"42");press(c,"Save");
        c.runOnClient(mc->check(mc.gui.screen()==map&&Client.INSTANCE.getMaps().waypoints().all().size()==count+1,"Keyboard correction saves waypoint and returns to same map"));
        c.takeScreenshot("ux9-map-created-"+compact);
        press(c,"Waypoints");press(c,name+", edit waypoint");
        focus(c,"Name");replace(c,name+" renamed");press(c,"Save");
        c.runOnClient(mc->check(focused(mc).startsWith(name+" renamed"),"Waypoint edit returns focus to same renamed destination"));
        press(c,name+" renamed, delete waypoint");key(c,256);
        c.runOnClient(mc->check(Client.INSTANCE.getMaps().waypoints().all().size()==count+1,"Escape cancels waypoint deletion"));
        press(c,name+" renamed, delete waypoint");press(c,"Yes");
        c.runOnClient(mc->check(Client.INSTANCE.getMaps().waypoints().all().size()==count,"Confirmed deletion removes only created destination"));
        press(c,"Done");
        c.runOnClient(mc->{
            double[] current=view(map);
            check(mc.gui.screen()==map&&Math.abs(current[0]-explored[0])<.001&&Math.abs(current[2]-explored[2])<.001,"Help and waypoint workflows retain explored map view");
        });
        key(c,256);
        c.runOnClient(mc->check(mc.gui.screen() instanceof PauseScreen,"Map Escape returns to caller after complete journey"));
    }
    private static void mapLayout(ClientGameTestContext c,WorldMapScreen map){
        c.runOnClient(mc->{
            var gear=map.children().stream().filter(PageGear.class::isInstance).map(PageGear.class::cast).findFirst().orElseThrow();
            double bottom=(double)field(map,"y")+(double)field(map,"h")-ScreenText.lineHeight(.65)*2-8;
            for(Object value:(List<?>)field(map,"buttons")){
                var b=((ActionButton)value).getBounds();
                boolean gearOverlap=b.x()<gear.getX()+gear.getWidth()&&b.x()+b.width()>gear.getX()&&b.y()<gear.getY()+gear.getHeight()&&b.y()+b.height()>gear.getY();
                check(b.x()>=0&&b.x()+b.width()<=map.width&&b.y()>=0&&b.y()+b.height()<=bottom&&!gearOverlap,"Measured map toolbar stays above footer and outside gear: "+((ActionButton)value).label());
            }
        });
    }
    private static void openChest(ClientGameTestContext c,TestSingleplayerContext world){c.runOnClient(mc->mc.gui.setScreen(null));world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,player)->net.minecraft.world.inventory.ChestMenu.threeRows(id,inv,new net.minecraft.world.SimpleContainer(27)),Component.literal("UX native chest")));p.containerMenu.setCarried(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,3));p.containerMenu.broadcastChanges();});c.waitFor(mc->mc.gui.screen() instanceof AbstractContainerScreen<?> && mc.player.containerMenu.getCarried().getCount()==3,200);c.waitTicks(3);}
}
