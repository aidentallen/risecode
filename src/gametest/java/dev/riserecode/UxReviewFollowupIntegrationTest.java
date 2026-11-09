package dev.riserecode;

import dev.riserecode.cosmetics.*;
import dev.riserecode.map.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.component.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.menu.*;
import dev.riserecode.ui.theme.*;
import dev.riserecode.util.RenderUtil;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;

/** Review11: field-owned validation, view-only inspection, and sampled rendered text surfaces. */
public final class UxReviewFollowupIntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Object field(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static void key(ClientGameTestContext c,int code){c.getInput().pressKey(code);c.waitTicks(2);}
    private static void nativePress(ClientGameTestContext c,String label){for(int i=0;i<80;i++){if(c.computeOnClient(mc->mc.gui.screen().getFocused() instanceof Button b&&b.getMessage().getString().startsWith(label))){key(c,257);return;}key(c,258);}throw new AssertionError("Cannot focus "+label);}
    private static EditBox input(net.minecraft.client.Minecraft mc,String name){return mc.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).filter(b->b.getMessage().getString().equals(name)).findFirst().orElseThrow();}
    private static void number(String name,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(name)).setValue(value);}
    private static void replace(ClientGameTestContext c,String name,String value){c.runOnClient(mc->{var f=input(mc,name);mc.gui.screen().setFocused(f);f.setFocused(true);f.setCursorPosition(0);f.setHighlightPos(f.getValue().length());});c.getInput().typeChars(value);c.waitTicks(2);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);c.getInput().resizeWindow(960,720);c.runOnClient(mc->{mc.options.guiScale().set(3);Client.INSTANCE.getThemeManager().reset();number("Font Scale",2);number("Window Width",360);number("Window Height",240);((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);var gui=Client.INSTANCE.getClickGui();gui.open(null);gui.selectPage(ClickGui.Page.COSMETICS);gui.getCosmeticsEditor().discard();gui.getCosmeticsEditor().category(CosmeticCategory.CAPE);gui.getCosmeticsEditor().search().setValue("");});c.waitTicks(4);
        cosmetics(c);readability(c);
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runOnServer(server->{check(server.getPlayerList().getPlayers().stream().allMatch(p->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(p.nameAndId())),"Validation fixture world contains only its owner");CameraFovClientTest.ownerOnlyShutdown=true;});c.waitFor(mc->Client.INSTANCE.getMaps().ready(),600);waypoint(c);
        }finally{CameraFovClientTest.ownerOnlyShutdown=false;}
        c.runOnClient(mc->{Client.INSTANCE.getThemeManager().reset();mc.gui.setScreen(null);});Client.INSTANCE.getLogger().info("UX independent review followup checks passed: {}",checks);
    }
    private static void cosmetics(ClientGameTestContext c){
        var editor=Client.INSTANCE.getClickGui().getCosmeticsEditor();var baseline=c.computeOnClient(mc->editor.draft());var applied=c.computeOnClient(mc->editor.manager().outfit());var item=c.computeOnClient(mc->editor.manager().library().definitions().stream().filter(d->d.category()==CosmeticCategory.CAPE&&editor.manager().library().available(d)).findFirst().orElseThrow());
        UxAuxiliaryIntegrationTest.focus(c,"Preview "+item.name()+", button");key(c,257);c.waitTicks(3);c.runOnClient(mc->{var s=(CosmeticDetailsScreen)mc.gui.screen();check(s.getNarrationMessage().getString().contains(item.name()),"Inspection narration identifies the actual item");check(s.previewView()&&s.inspectedId().equals(item.id()),"Item-specific row opens installed item in large preview");check(editor.draft().equals(baseline)&&editor.manager().outfit().equals(applied),"Inspecting never selects or applies an outfit item");check(s.avatarBounds().height()>=100&&s.avatarBounds().y()+s.avatarBounds().height()<=s.height-30,"Compact inspection dedicates over100 logical pixels to model above actions");});c.takeScreenshot("ux14-installed-preview-only-font2");
        nativePress(c,"View controls");nativePress(c,"Choose pose");nativePress(c,"Crouching");nativePress(c,"Large preview");c.runOnClient(mc->check(editor.previewPose()==CosmeticsEditor.PreviewPose.CROUCHING&&editor.draft().equals(baseline),"Pose controls return to large preview without changing draft"));c.takeScreenshot("ux14-installed-crouch-preview-font2");key(c,256);c.runOnClient(mc->check(mc.gui.screen()==Client.INSTANCE.getClickGui()&&editor.draft().equals(baseline),"Preview return keeps exact outfit baseline"));
        UxAuxiliaryIntegrationTest.focus(c,"Preview "+item.name()+", button");key(c,257);nativePress(c,"Select in draft");c.runOnClient(mc->check(editor.isDraftSelected(item.id())&&editor.isDirty()&&editor.manager().outfit().equals(applied),"Explicit Select in draft changes only draft, never Apply"));key(c,256);c.runOnClient(mc->editor.discard());c.waitTicks(3);
        UxAuxiliaryIntegrationTest.focus(c,"Import a cape, button");key(c,257);c.runOnClient(mc->{var s=(CosmeticDetailsScreen)mc.gui.screen();check(!s.previewView()&&s.path().visible,"Dedicated empty-outfit import route exposes PNG input");check(editor.draft().equals(baseline),"Opening import does not change draft");});key(c,256);c.runOnClient(mc->mc.gui.setScreen(null));
    }
    private static final class ReadabilityProbe extends Screen {
        private final int backdrop;
        private ReadabilityProbe(int color){super(Component.literal("Rendered readability probe"));backdrop=color;}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float dt){
            g.fill(0,0,width,height,backdrop);RenderUtil.drawWindow(g,12,12,width-24,height-24,1);
            RenderUtil.text(g,font,"Secondary descriptions",32,44,1,Theme.SECONDARY);RenderUtil.text(g,font,"Muted placeholder text",32,82,1,Theme.MUTED);RenderUtil.text(g,font,"Primary headings",32,120,1,Theme.TEXT);
        }
    }
    private static void readability(ClientGameTestContext c){
        c.getInput().resizeWindow(960,720);c.runOnClient(mc->{mc.options.guiScale().set(3);number("Font Scale",1);number("Panel Opacity",0);});c.waitTicks(3);
        for(double font:new double[]{1,2}){c.runOnClient(mc->{number("Font Scale",font);number("Panel Opacity",0);});
            for(int color:new int[]{0xFFFFFFFF,0xFF9BCAF2,0xFF70A946,0xFF151920}){c.runOnClient(mc->mc.gui.setScreen(new ReadabilityProbe(color)));c.waitTicks(3);c.takeScreenshot("ux14-readability-font-"+font+"-"+Integer.toHexString(color));}
            c.runOnClient(mc->{((ColorSetting)Client.INSTANCE.getThemeManager().getSetting("Background Color")).setValue(0xFFFFFFFF);number("Panel Opacity",1);mc.gui.setScreen(new ReadabilityProbe(0xFFFFFFFF));});c.waitTicks(3);c.takeScreenshot("ux14-readability-font-"+font+"-custom-white");
        }c.runOnClient(mc->{check(Theme.bool("Readable Surfaces",false),"Readable backing is enabled by default independent of decorative panel opacity");Client.INSTANCE.getThemeManager().reset();number("Font Scale",2);mc.gui.setScreen(null);});
    }
    private static void waypoint(ClientGameTestContext c){
        c.runOnClient(mc->mc.options.guiScale().set(3));var maps=Client.INSTANCE.getMaps();var point=new WaypointStore.Waypoint(UUID.randomUUID(),"Validation sample",1,64,2,maps.dimension(),"",0xFF64D8C4,true,false,false);var parent=c.computeOnClient(mc->new WorldMapScreen(new PauseScreen(true),maps));int count=maps.waypoints().all().size();c.runOnClient(mc->mc.gui.setScreen(new WaypointEditScreen(parent,maps,point)));c.waitTicks(3);
        replace(c,"X","oops");nativePress(c,"Save");c.runOnClient(mc->check(field(mc.gui.screen(),"error").toString().startsWith("X:"),"Save associates invalid coordinate with X"));replace(c,"X","12");c.runOnClient(mc->check(field(mc.gui.screen(),"error").toString().isEmpty(),"Typed valid correction clears stale owning error before Save"));key(c,258);c.getInput().resizeWindow(1280,960);c.runOnClient(mc->mc.options.guiScale().set(2));c.waitTicks(3);c.runOnClient(mc->check(field(mc.gui.screen(),"error").toString().isEmpty(),"Corrected feedback stays accurate after blur and real resize"));c.takeScreenshot("ux14-waypoint-corrected-after-resize");
        replace(c,"X","again invalid");c.runOnClient(mc->check(field(mc.gui.screen(),"error").toString().startsWith("X:"),"Re-invalidating the owning field restores current error"));replace(c,"Dimension","minecraft:the_nether");replace(c,"Color","FFD166");c.runOnClient(mc->check(field(mc.gui.screen(),"error").toString().startsWith("X:"),"Other valid fields do not clear an unrelated X error"));nativePress(c,"Save");c.runOnClient(mc->check(maps.waypoints().all().size()==count,"Invalid complete form cannot commit"));replace(c,"X","42");nativePress(c,"Save");c.runOnClient(mc->{check(mc.gui.screen()==parent&&maps.waypoints().all().stream().anyMatch(w->w.id().equals(point.id())&&w.x()==42),"Corrected form saves only fully valid values and returns");maps.waypoints().remove(point.id());maps.saveWaypoints();mc.gui.setScreen(null);});
    }
}
