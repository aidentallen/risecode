package dev.riserecode;

import dev.riserecode.module.modules.*;
import dev.riserecode.performance.PerformancePreferences;
import dev.riserecode.render.*;
import dev.riserecode.render.placement.*;
import dev.riserecode.setting.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.*;
import dev.riserecode.util.RenderUtil;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Disposable-world native checks. No shared instance configuration or running game is touched. */
public final class Release025IntegrationTest {
    private static int checks;
    private static final AtomicLong CLOCK=new AtomicLong(1_000_000_000L);
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static ThemeManager theme(){return Client.INSTANCE.getThemeManager();}
    private static void value(String name,Object value){ThemeManager.assign(theme().getSetting(name),value);theme().changed();}
    private static final class TypographyProbe extends Screen {
        TypographyProbe(){super(Component.literal("Typography"));}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float dt){
            RenderUtil.drawRoundedRect(g,10,10,170,65,3,0xFF18212C);
            RenderUtil.text(g,font,"RiseRecode · ÁßΩ Ж 中 😀",18,18,1.5,0xFFFFFFFF);
            RenderUtil.text(g,font,"Fallback clipping / shadows",18,42,1,0xFFFFFFFF);
        }
    }
    private static void shaders(boolean enabled){
        var config=net.irisshaders.iris.Iris.getIrisConfig();config.setShaderPackName(System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.setShadersEnabled(enabled);
        try{config.save();net.irisshaders.iris.Iris.reload();}catch(java.io.IOException failure){throw new AssertionError(failure);}
    }
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();theme().reset();mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.options.pauseOnLostFocus=false;});
        ui(c);
        try(var world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server->{check(server.getPlayerList().getPlayers().stream().allMatch(player->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(player.nameAndId())),"owner-only disposable world cleanup");CameraFovClientTest.ownerOnlyShutdown=true;});
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("fill -8 64 -8 8 64 8 stone");world.getServer().runCommand("tp @a 0.5 65 4.5 180 5");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");c.waitTicks(15);
            c.runOnClient(mc->{mc.gui.setScreen(null);mc.player.getAbilities().flying=true;mc.player.onUpdateAbilities();});
            scaling(c,"native");lifecycle(c);chests(c,world);
            if(Boolean.getBoolean("riserecode.withShaderPack")){
                c.runOnClient(mc->shaders(true));c.waitFor(mc->ShaderWorldOverlay.active()&&mc.gui.overlay()==null,1200);
                scaling(c,"shader");
                c.runOnClient(mc->{boolean previous=net.irisshaders.iris.shadows.ShadowRenderer.ACTIVE;try{net.irisshaders.iris.shadows.ShadowRenderer.ACTIVE=true;check(ShaderWorldOverlay.shadowPass(),"Iris shadow state reached");Client.INSTANCE.getWorldRenderer().submit(null);check(true,"shadow guard executes before every custom silhouette lambda");}finally{net.irisshaders.iris.shadows.ShadowRenderer.ACTIVE=previous;}});chests(c,world);
                c.runOnClient(mc->shaders(false));c.waitFor(mc->!ShaderWorldOverlay.active()&&mc.gui.overlay()==null,900);
            }
            c.runOnClient(mc->{Client.INSTANCE.getPerformance().worldResolution.setValue(100.0);PlacementAnimations.timeSource(System::nanoTime);Client.INSTANCE.getModuleManager().disableAll();});c.waitTicks(3);
            c.runOnClient(mc->check(WorldResolution.ownedGpuBytes()==0,"100% releases scale targets"));
        }
        finally{CameraFovClientTest.ownerOnlyShutdown=false;}
        c.runOnClient(mc->{theme().reset();mc.gui.setScreen(null);});
        c.runOnClient(mc->Release025Timings.save());
        Client.INSTANCE.getLogger().info("Release 0.25 native checks passed ({} assertions)",checks);
    }
    private static void ui(ClientGameTestContext c){
        c.runOnClient(mc->{
            check(CustomTextRenderer.available(ThemeManager.Typeface.GOOGLE_SANS)&&CustomTextRenderer.available(ThemeManager.Typeface.QRAFTYS),"both fonts available offline");
            check(mc.getResourceManager().getResource(net.minecraft.resources.Identifier.fromNamespaceAndPath("riserecode","font/qraftys.json")).isPresent(),"bundled namespaced font registered");
            check(ClickGui.VISIBLE_PAGES.contains(ClickGui.Page.PERFORMANCE),"Performance is main navigation");
            value("Reduced Motion",true);Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.APPEARANCE);
        });c.waitTicks(5);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var header=gui.getThemeEditor().section("Typography");var b=header.getBounds();boolean before=header.isExpanded();
            var mouse=new MouseButtonEvent(b.centerX()*gui.getCanvasScale(),b.centerY()*gui.getCanvasScale(),new MouseButtonInfo(1,0));
            gui.mouseClicked(mouse,false);gui.mouseReleased(mouse);check(header.isExpanded()!=before,"right click uses appearance card transform");
            gui.getSettingsPopup().activate(7,true);check(gui.getPage()==ClickGui.Page.PERFORMANCE,"footer reaches canonical Performance page");
            gui.getSettingsPopup().activate(2,true);check(gui.getPage()==ClickGui.Page.CLIENT_SOUNDS&&gui.getClientSettingsEditor().editor().section("Client Sounds").isExpanded(),"popup details expand canonical page");
        });
        c.waitTicks(4);c.takeScreenshot("025-client-sounds-page");
        c.runOnClient(mc->Client.INSTANCE.getClickGui().openClientSettings(dev.riserecode.ui.menu.ClientSettingsScreen.Section.PERFORMANCE));c.waitTicks(5);c.takeScreenshot("025-performance-page");
        c.runOnClient(mc->{value("Reduced Motion",false);Client.INSTANCE.getClickGui().getSettingsPopup().toggle();});c.waitTicks(1);
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var popup=gui.getSettingsPopup();var raw=popup.popupBounds();var shown=popup.displayedPopupBounds();var row=popup.rowBounds(2);
            double x=shown.x()+(row.centerX()-raw.x())*shown.width()/raw.width(),y=shown.y()+(row.centerY()-raw.y())*shown.height()/raw.height();
            var mouse=new MouseButtonEvent(x*gui.getCanvasScale(),y*gui.getCanvasScale(),new MouseButtonInfo(1,0));gui.mouseClicked(mouse,false);gui.mouseReleased(mouse);
            check(gui.getPage()==ClickGui.Page.CLIENT_SOUNDS,"right click during popup entrance opens detail page");
        });
        for(var face:new ThemeManager.Typeface[]{ThemeManager.Typeface.GOOGLE_SANS,ThemeManager.Typeface.QRAFTYS})for(int bold:new int[]{0,100}){
            c.runOnClient(mc->{value("Font Selection",face);value("Font Boldness",(double)bold);mc.gui.setScreen(new TypographyProbe());});c.waitTicks(5);c.takeScreenshot("025-font-"+face+"-"+bold);
            c.runOnClient(mc->{double width=CustomTextRenderer.width(mc.font,"Advance ΩЖ中",false);value("Font Boldness",bold==0?100.0:0.0);check(CustomTextRenderer.width(mc.font,"Advance ΩЖ中",false)==width,"stroke preserves advances and wrapping");});
        }
        c.runOnClient(mc->{value("Font Boldness",0.0);value("Reduced Motion",false);mc.gui.setScreen(null);});
    }
    private static void scaling(ClientGameTestContext c,String label){
        c.runOnClient(mc->{
            var esp=Client.INSTANCE.getModuleManager().getByClass(PlayerEspModule.class).orElseThrow();
            ThemeManager.assign(esp.getSettings().stream().filter(s->s.getName().equals("Mode")).findFirst().orElseThrow(),PlayerEspModule.Mode.OUTLINE);esp.setEnabled(true);
            for(int i=0;i<3;i++){
                var remote=new net.minecraft.client.player.RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(new UUID(0,25001+i),"ESPFixture"+i));remote.setId(25001+i);remote.setPos(-1.5+i*.8,65,.5);
                if(i==1){Client.INSTANCE.getFriendManager().add(remote.getGameProfile().name(),remote.getUUID());remote.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));}
                mc.level.addEntity(remote);
            }
        });
        for(var upscaler:PerformancePreferences.Upscaler.values())for(int percent:new int[]{100,75,50,25}){
            c.runOnClient(mc->{var p=Client.INSTANCE.getPerformance();p.upscaler.setValue(upscaler);p.worldResolution.setValue((double)percent);});c.waitTicks(8);
            c.runOnClient(mc->{
                var actual=WorldResolution.status();var main=mc.gameRenderer.mainRenderTarget();var expected=WorldResolution.Extents.of(main.width,main.height,percent);
                check(actual.requested()==percent&&actual.actual().equals(expected),label+" actual scaled extent "+percent+" "+actual.label());
                check(actual.reason().isEmpty(),label+" requested path supported: "+actual.reason());
                check(main.width==mc.getWindow().getWidth()&&main.height==mc.getWindow().getHeight(),"presentation and input stay native");
                if(percent<100)check(actual.upscaler()==upscaler,"requested upscaler executes");
            });c.takeScreenshot("025-"+label+"-"+upscaler+"-"+percent);
            c.runOnClient(mc->Release025Timings.scene(label+"-"+upscaler+"-"+percent));c.waitTicks(6);c.runOnClient(mc->Release025Timings.scene(""));
        }
        for(boolean visible:new boolean[]{true,false}){
            c.runOnClient(mc->{var esp=Client.INSTANCE.getModuleManager().getByClass(PlayerEspModule.class).orElseThrow();ThemeManager.assign(esp.getSettings().stream().filter(s->s.getName().equals("VisibleOnly")).findFirst().orElseThrow(),visible);var player=mc.level.getEntity(25001);player.setPos(player.getX()+.15,65,.5);});
            c.waitTicks(4);c.takeScreenshot("025-"+label+"-moving-friends-armor-depth-"+visible);
        }
        long allocations=c.computeOnClient(mc->WorldResolution.allocations());c.waitTicks(15);
        c.runOnClient(mc->{check(WorldResolution.allocations()==allocations,"steady scale does not allocate GPU targets per frame");check(SilhouetteOutlineRenderer.composites()>0,"true model silhouettes composited before upscale");mc.gui.setScreen(new TypographyProbe());});c.waitTicks(5);c.takeScreenshot("025-"+label+"-native-gui-over-scaled-world");
        c.runOnClient(mc->mc.gui.setScreen(null));
        var reload=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->reload.isDone()&&mc.gui.overlay()==null,1200);c.waitTicks(5);
        c.runOnClient(mc->{check(WorldResolution.status().actual().worldWidth()<mc.gameRenderer.mainRenderTarget().width,"scale restored after resource reload");Client.INSTANCE.getPerformance().worldResolution.setValue(100.0);});c.waitTicks(4);
    }
    private static void field(String name,Object value){try{var f=WorldResolution.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}catch(ReflectiveOperationException e){throw new AssertionError(e);}}
    private static void lifecycle(ClientGameTestContext c){
        c.runOnClient(mc->{WorldResolution.close();field("checked",true);field("fsrSupported",false);field("fsrReason","FSR1 unavailable; using Bilinear");var p=Client.INSTANCE.getPerformance();p.upscaler.setValue(PerformancePreferences.Upscaler.FSR1);p.worldResolution.setValue(75.0);});c.waitTicks(3);
        c.runOnClient(mc->{check(WorldResolution.status().upscaler()==PerformancePreferences.Upscaler.BILINEAR&&!WorldResolution.status().reason().isEmpty(),"unsupported FSR1 falls back to Bilinear with reason");check(Client.INSTANCE.getPerformance().upscaler.getValue()==PerformancePreferences.Upscaler.FSR1,"fallback preserves requested upscaler");field("scalingFailed",true);});c.waitTicks(3);
        c.runOnClient(mc->{check(WorldResolution.status().actual().pixelScale()==1&&WorldResolution.ownedGpuBytes()==0,"unsupported scaling falls back to native and releases targets");check(Client.INSTANCE.getPerformance().worldResolution.getValue()==75,"native fallback preserves saved resolution");WorldResolution.close();mc.getWindow().setWindowed(1279,719);});c.waitTicks(5);
        c.runOnClient(mc->{var main=mc.gameRenderer.mainRenderTarget();check(WorldResolution.status().actual().equals(WorldResolution.Extents.of(main.width,main.height,75)),"resize applies scaled extents at next frame boundary");mc.gui.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));});c.waitTicks(4);c.takeScreenshot("025-paused-scaled-world");
        c.runOnClient(mc->{check(mc.gameRenderer.mainRenderTarget().width==mc.getWindow().getWidth(),"paused GUI remains native");mc.gui.setScreen(null);mc.getWindow().toggleFullScreen();});c.waitTicks(5);
        c.runOnClient(mc->{check(mc.gameRenderer.mainRenderTarget().width==mc.getWindow().getWidth(),"fullscreen presentation target follows native output");mc.getWindow().toggleFullScreen();mc.getWindow().setWindowed(1280,720);Client.INSTANCE.getPerformance().worldResolution.setValue(100.0);});c.waitTicks(4);
    }
    private static boolean bbeFlag(net.minecraft.client.Minecraft mc,BlockPos pos,String method){
        try{return (boolean)mc.level.getBlockEntity(pos).getClass().getMethod(method).invoke(mc.level.getBlockEntity(pos));}catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private static void chests(ClientGameTestContext c,net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world){
        var pos=new BlockPos(0,65,0);
        c.runOnClient(mc->{for(int id=25001;id<=25003;id++){var remote=mc.level.getEntity(id);if(remote!=null)remote.discard();}
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("betterblockentities"))try{Class.forName("betterblockentities.client.gui.config.ConfigCache").getField("updateType").setInt(null,0);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
            PlacementAnimations.clear(true);PlacementAnimations.timeSource(CLOCK::get);value("Reduced Motion",false);var animation=Client.INSTANCE.getModuleManager().getByClass(BlockAnimationsModule.class).orElseThrow();animation.setEnabled(false);animation.setEnabled(true);});
        for(String block:new String[]{"chest","trapped_chest"})for(String facing:new String[]{"north","south","east","west"}){
            world.getServer().runCommand("setblock 0 65 0 "+block+"[facing="+facing+"]");c.waitTicks(3);
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("betterblockentities"))c.waitFor(mc->bbeFlag(mc,pos,"bbe$isTerrainMeshReady"),400);
            c.runOnClient(mc->{CLOCK.addAndGet(2_000_000_000L);var module=Client.INSTANCE.getModuleManager().getByClass(BlockAnimationsModule.class).orElseThrow();module.duration.setValue(1.0);check(PlacementAnimations.start(pos,mc.level.getBlockState(pos),mc.player.position(),module.motionSettings(),64,64),"starts "+block+" "+facing);});
            c.waitFor(mc->PlacementAnimations.at(pos)!=null,400);
            c.runOnClient(mc->{check(PlacementAnimations.at(pos).progress()==0,"motion starts after acknowledged ownership");CLOCK.addAndGet(500_000_000L);});c.waitTicks(3);
            c.runOnClient(mc->{check(PlacementAnimations.at(pos)!=null&&PlacementAnimations.at(pos).progress()==.5,"full duration remains after mesh handoff");if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("betterblockentities"))check(!bbeFlag(mc,pos,"bbe$isTerrainMeshReady"),"BBE cached mesh ownership transferred to native lid/model renderer");});
            c.takeScreenshot("025-placement-"+block+"-"+facing);
            if(facing.equals("north")){c.runOnClient(mc->mc.level.blockEvent(pos,mc.level.getBlockState(pos).getBlock(),1,1));c.waitTicks(5);c.takeScreenshot("025-opening-"+block);c.runOnClient(mc->mc.level.blockEvent(pos,mc.level.getBlockState(pos).getBlock(),1,0));}
            c.runOnClient(mc->CLOCK.addAndGet(1_000_000_000L));c.waitFor(mc->PlacementAnimations.pending()==0,400);
        }
        for(String block:new String[]{"chest","trapped_chest"})for(String facing:new String[]{"north","south","east","west"}){
            var state=net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING,net.minecraft.core.Direction.valueOf(facing.toUpperCase(Locale.ROOT))).setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT);
            var other=pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
            world.getServer().runCommand("fill -2 65 -2 2 66 2 air");
            world.getServer().runCommand("setblock 0 65 0 "+block+"[facing="+facing+",type=left]");world.getServer().runCommand("setblock "+other.getX()+" 65 "+other.getZ()+" "+block+"[facing="+facing+",type=right]");c.waitTicks(4);
            c.runOnClient(mc->{var module=Client.INSTANCE.getModuleManager().getByClass(BlockAnimationsModule.class).orElseThrow();check(PlacementAnimations.start(other,mc.level.getBlockState(other),mc.player.position(),module.motionSettings(),64,64),"second chest starts coherent group "+block+" "+facing);});
            c.waitFor(mc->PlacementAnimations.draws().size()==2,400);
            c.runOnClient(mc->CLOCK.addAndGet(500_000_000L));c.waitTicks(3);
            c.runOnClient(mc->{var left=PlacementAnimations.at(pos);var right=PlacementAnimations.at(other);check(left!=null&&right!=null&&left.progress()==right.progress(),"double chest shares timing");
                var seam=new Vec3((pos.getX()+other.getX()+1)/2.0,65.5,(pos.getZ()+other.getZ()+1)/2.0);check(PlacementAnimations.transform(left,seam).distanceTo(PlacementAnimations.transform(right,seam))<.00001,"double chest seam remains joined under rotation and scaling");});c.takeScreenshot("025-double-"+block+"-"+facing);
            world.getServer().runCommand("setblock "+other.getX()+" 65 "+other.getZ()+" air");c.waitTicks(3);
            c.runOnClient(mc->check(PlacementAnimations.pending()==0,"removal releases entire group"));
        }
        for(String block:new String[]{"barrel","shulker_box","ender_chest"}){
            if(block.equals("ender_chest")&&c.computeOnClient(mc->ShaderWorldOverlay.active()))continue; // Intentional shader-emissive exclusion.
            world.getServer().runCommand("setblock 0 65 0 "+block);c.waitTicks(3);
            c.runOnClient(mc->{var module=Client.INSTANCE.getModuleManager().getByClass(BlockAnimationsModule.class).orElseThrow();check(PlacementAnimations.start(pos,mc.level.getBlockState(pos),mc.player.position(),module.motionSettings(),64,64),"representative block entity "+block);});c.waitFor(mc->PlacementAnimations.at(pos)!=null,400);
            c.runOnClient(mc->CLOCK.addAndGet(2_000_000_000L));c.waitFor(mc->PlacementAnimations.pending()==0,400);
        }
        var timeoutPos=new BlockPos(2,65,0);world.getServer().runCommand("setblock 2 65 0 stone");c.waitTicks(3);
        c.runOnClient(mc->{var module=Client.INSTANCE.getModuleManager().getByClass(BlockAnimationsModule.class).orElseThrow();check(PlacementAnimations.start(timeoutPos,mc.level.getBlockState(timeoutPos),mc.player.position(),module.motionSettings(),64,64),"handoff timeout fixture starts");
            CLOCK.addAndGet(1_100_000_000L);PlacementAnimations.frame(CLOCK.get());check(PlacementAnimations.draws().isEmpty(),"unacknowledged ownership abandons motion after one second");});c.waitFor(mc->PlacementAnimations.pending()==0,400);
        c.runOnClient(mc->{if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("betterblockentities"))check(PlacementAnimations.bbeSuppressions()>0,"BBE terrain geometry was actually suppressed by stamped mesh ownership");PlacementAnimations.stop();PlacementAnimations.clear(true);PlacementAnimations.timeSource(System::nanoTime);});
    }
}
