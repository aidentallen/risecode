package dev.riserecode;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.cosmetics.CosmeticLoadout;
import dev.riserecode.module.modules.NametagsModule;
import dev.riserecode.setting.BlockListSetting;
import dev.riserecode.testmixin.LifetimeInvokers;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.theme.PageAppearance;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Disposable deterministic scenes; synthetic players do not claim a live multiplayer benchmark. */
public final class RemediationClientTest {
    private static final double[] frames=new double[100_000],renderFrames=new double[100_000];
    private static int frameCount,renderCount;
    private static long allocatedBytes,cpuNanos;
    private static long[] gcCounts,gcTimes;
    private static boolean moving;
    private static final com.sun.management.ThreadMXBean THREADS=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
    public static long allocated(){return THREADS.getThreadAllocatedBytes(Thread.currentThread().threadId());}
    public static long cpu(){return THREADS.getCurrentThreadCpuTime();}
    public static void rendered(long nanos,long allocated,long cpu){if(recording&&renderCount<renderFrames.length){renderFrames[renderCount++]=nanos/1e6;allocatedBytes+=allocated;cpuNanos+=cpu;}}
    private static long previous;
    private static boolean recording,scrolling;
    private static int scrollFrame,checks;
    private RemediationClientTest(){ }
    private static void check(boolean ok,String message){checks++;if(!ok){throw new AssertionError(message);}}
    public static void frame(){
        if(Boolean.getBoolean("riserecode.remediationOnly")){Minecraft.getInstance().getFramerateLimitTracker().onInputReceived();}
        long now=System.nanoTime();
        if(recording&&previous!=0&&frameCount<frames.length){frames[frameCount++]=(now-previous)/1e6;}
        previous=now;
        if(moving){var p=Minecraft.getInstance().player;if(p!=null){double angle=now/1e9*.25;p.setPos(12+Math.sin(angle)*12,64,12+Math.cos(angle)*12);}}
        if(scrolling&&++scrollFrame%12==0){var editor=Client.INSTANCE.getClickGui().getResourcesEditor();var b=editor.getBounds();editor.mouseScrolled(b.centerX(),b.centerY(),scrollFrame%480<240?-3:3);}
    }
    private static void lifetimes(Minecraft mc){
        var g=new GuiGraphicsExtractor(mc,new GuiRenderState(),0,0);
        for(int mode:new int[]{1,2}){
            LifetimeFaults.mode=mode;
            var poses=new PoseStack();
            try{
                invoke(()->mc.gameRenderer.itemInHandRenderer.renderItem(null,ItemStack.EMPTY,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,poses,null,0),mode);
                check(poses.isEmpty(),"held-item pose restored after cancel/throw");
                invoke(()->((LifetimeInvokers.Map)mc.gameRenderer.itemInHandRenderer).risecode$map(poses,null,0,ItemStack.EMPTY),mode);
                check(poses.isEmpty(),"map pose restored after cancel/throw");
                var matrix=new org.joml.Matrix3x2f(g.pose());
                invoke(()->((LifetimeInvokers.Crosshair)mc.gui.hud).risecode$crosshair(g,DeltaTracker.ZERO),mode);
                check(matrix.equals(g.pose()),"crosshair pose restored after cancel/throw");
            }finally{LifetimeFaults.mode=0;}
        }
        Screen throwing=new Screen(Component.literal("exception fixture")){
            @Override public void extractBackground(GuiGraphicsExtractor graphics,int x,int y,float delta) { }
            @Override public void extractRenderState(GuiGraphicsExtractor graphics,int x,int y,float delta){throw LifetimeFaults.FAILURE;}
        };
        throwing.init(800,450);PageAppearance.context("outer-fixture");
        try{invoke(()->throwing.extractRenderStateWithTooltipAndSubtitles(g,0,0,0),2);check(PageAppearance.context().equals("outer-fixture"),"screen context restores nested owner after exception");}
        finally{PageAppearance.context("");}
    }
    private static void invoke(Runnable action,int mode){try{action.run();check(mode==1,"renderer should throw");}catch(IllegalStateException expected){check(mode==2&&expected==LifetimeFaults.FAILURE,"expected renderer exception propagated");}}
    private static void enabled(String name,boolean value){var module=Client.INSTANCE.getModuleManager().getByName(name).orElseThrow();module.setEnabled(value);}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        boolean baseline=Boolean.getBoolean("riserecode.remediationBaseline");
        c.runOnClient(mc->{Client.INSTANCE.getModuleManager().disableAll();Client.INSTANCE.getThemeManager().reset();mc.options.enableVsync().set(false);mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);mc.options.framerateLimit().set(260);mc.options.renderDistance().set(2);if(!baseline){lifetimes(mc);}mc.gui.setScreen(null);});
        if(!baseline){PerformanceClientChecks.ui(c);}
        if(Boolean.getBoolean("riserecode.withShaderPack")){c.runOnClient(mc->shaderPack(true));}
        try(var world=c.worldBuilder().create()){
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");world.getServer().runCommand("fill -32 48 -32 -1 63 31 stone");world.getServer().runCommand("fill 0 48 -32 31 63 31 stone");world.getServer().runCommand("tp @a 0.5 65 -10.5 0 0");
            world.getConnection().waitForChunksRender();c.waitTicks(60);
            if(Boolean.getBoolean("riserecode.withShaderPack")){c.runOnClient(mc->check(dev.riserecode.render.AmbienceRenderer.shaderPackActive(),"shader-pack scene is active"));}
            c.runOnClient(mc->check(mc.player.getY()>=64&&mc.player.getY()<66,"benchmark camera rests on stone platform"));
            c.runOnClient(mc->{enabled("Watermark",true);enabled("ArrayList",true);enabled("FPS",true);enabled("Coordinates",true);});
            measure(c,"idle-hud");
            c.runOnClient(mc->{
                for(int i=0;i<24;i++){
                    var p=new RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(new UUID(0,1000+i),"Fixture"+i));p.setId(-1000-i);p.setPos((i%8-4)*1.7,64,2+(i/8)*2);p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.DIAMOND_CHESTPLATE));mc.level.addEntity(p);
                }
                var esp=Client.INSTANCE.getModuleManager().getByName("PlayerESP").orElseThrow();
                dev.riserecode.config.SettingCodecs.apply(esp.getSettings().stream().filter(s->s.getName().equals("Mode")).findFirst().orElseThrow(),new com.google.gson.JsonPrimitive("OUTLINE"));
                check(mc.level.players().size()==25,"all synthetic player fixtures present");
                enabled("Nametags",true);enabled("PlayerESP",true);
            });c.waitTicks(40);c.runOnClient(mc->check(dev.riserecode.render.SilhouetteOutlineRenderer.composites()>0,"populated scene exercises concrete silhouette adapter"));if(!baseline){PerformanceClientChecks.equipment(c);}measure(c,"synthetic-24-players");
            c.runOnClient(mc->{for(int i=0;i<24;i++){mc.level.removeEntity(-1000-i,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}enabled("Nametags",false);enabled("PlayerESP",false);
                var module=Client.INSTANCE.getModuleManager().getByName("BlockESP").orElseThrow();((BlockListSetting)module.getSettings().stream().filter(s->s.getName().equals("Blocks")).findFirst().orElseThrow()).setValue(List.of("minecraft:stone"));module.setEnabled(true);
            });
            c.waitFor(mc->Client.INSTANCE.getBlockIndex().pendingSections()==0,7200);c.waitTicks(40);measure(c,"common-stone-esp");
            if(!baseline){PerformanceClientChecks.assets(c);}
            c.runOnClient(mc->{enabled("BlockESP",false);var cosmetics=Client.INSTANCE.getCosmetics();var outfit=CosmeticLoadout.empty();for(String id:new String[]{"wings-dragon","head-halo","tail-fox","pet-bee","aura-magic-ring","particles-hearts"}){outfit=outfit.with(cosmetics.library().find(id).category(),CosmeticLoadout.Item.defaults(id));}cosmetics.apply(outfit);mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);});
            c.waitFor(mc->Client.INSTANCE.getCosmetics().request("pet-bee")!=null,600);c.waitTicks(40);if(!baseline){PerformanceClientChecks.effects(c,"cosmetics");}measure(c,"cosmetics");
            c.runOnClient(mc->{mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);Client.INSTANCE.getCosmetics().apply(CosmeticLoadout.empty());var gui=Client.INSTANCE.getClickGui();gui.open(null);gui.selectPage(ClickGui.Page.RESOURCES);scrolling=true;scrollFrame=0;});
            c.waitTicks(60);if(!baseline){PerformanceClientChecks.effects(c,"resources");}measure(c,"resource-grid-scrolling");c.runOnClient(mc->scrolling=false);
            c.runOnClient(mc->{Client.INSTANCE.getClickGui().onClose();mc.gui.setScreen(null);enabled("Minimap",true);Client.INSTANCE.getMaps().recording(true);moving=true;});
            if(!baseline){PerformanceClientChecks.independentTrails(c);}
            measure(c,"movement-map-recording");c.runOnClient(mc->{moving=false;enabled("Minimap",false);});
            if(!baseline){
                world.getServer().runCommand("setblock 0 64 0 chest");
                c.runOnClient(mc->Client.INSTANCE.getBlockIndex().containersRequested(true));
                c.waitFor(mc->Client.INSTANCE.getBlockIndex().containers().stream().anyMatch(entity->entity.getBlockPos().equals(new net.minecraft.core.BlockPos(0,64,0))),600);
                c.runOnClient(mc->{check(!Client.INSTANCE.getBlockIndex().containers().isEmpty(),"container discovery advances incrementally");Client.INSTANCE.getBlockIndex().unloaded(dev.riserecode.render.ChunkMotionTracker.key(0,0));});
                c.waitFor(mc->Client.INSTANCE.getBlockIndex().containers().stream().noneMatch(entity->(entity.getBlockPos().getX()>>4)==0&&(entity.getBlockPos().getZ()>>4)==0),600);
                c.runOnClient(mc->{check(Client.INSTANCE.getBlockIndex().containers().stream().noneMatch(entity->entity.getBlockPos().equals(new net.minecraft.core.BlockPos(0,64,0))),"container snapshots discard unloaded chunks");Client.INSTANCE.getBlockIndex().loaded(mc.level.getChunk(0,0));Client.INSTANCE.getBlockIndex().containersRequested(false);});
            }
            c.runOnClient(mc->{var p=new RemotePlayer(mc.level,new com.mojang.authlib.GameProfile(new UUID(0,2000),"ResizeFixture"));p.setId(-2000);p.setPos(12,64,48);p.setNoGravity(true);mc.level.addEntity(p);});
            for(int cycle=0;cycle<Integer.getInteger("riserecode.lifetimeCycles",20);cycle++){
                int presetCycle=cycle;c.runOnClient(mc->{if(!baseline){Client.INSTANCE.getPerformance().mode.setValue(dev.riserecode.performance.PerformancePreferences.Mode.values()[presetCycle%4]);}Client.INSTANCE.getClickGui().onClose();enabled("PlayerESP",true);enabled("BlockESP",true);});c.waitTicks(10);
                c.getInput().resizeWindow(cycle%2==0?1280:1600,cycle%2==0?720:900);
                var reload=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->reload.isDone()&&mc.gui.overlay()==null,1200);
                c.waitTicks(10);
                c.runOnClient(mc->{var main=mc.gameRenderer.mainRenderTarget();check(!dev.riserecode.render.SilhouetteOutlineRenderer.maskTargets().isEmpty(),"cycle actually renders a silhouette");check(dev.riserecode.render.SilhouetteOutlineRenderer.maskTargets().stream().allMatch(target->target.width==main.width&&target.height==main.height),"active silhouette targets match resized framebuffer");enabled("PlayerESP",false);enabled("BlockESP",false);System.gc();});c.waitTicks(20);
                int number=cycle;c.runOnClient(mc->{var row=new JsonObject();row.addProperty("cycle",number);row.addProperty("heapUsed",java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());try{row.addProperty("deviceVramUsed",Long.parseLong(Files.readString(Path.of("/sys/class/drm/card1/device/mem_info_vram_used")).trim()));}catch(java.io.IOException | NumberFormatException unavailable){row.addProperty("deviceVramUsed",-1);}
                row.addProperty("silhouetteTargets",dev.riserecode.render.SilhouetteOutlineRenderer.maskTargets().size());write("cycles",row);});
            }
            if(!baseline){c.runOnClient(mc->Client.INSTANCE.getPerformance().mode.setValue(dev.riserecode.performance.PerformancePreferences.Mode.OFF));PerformanceClientChecks.idleTargets(c);}
            int soak=Integer.getInteger("riserecode.soakSeconds",0);
            if(soak>0){
                c.runOnClient(mc->{enabled("Minimap",true);enabled("PlayerESP",true);mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);var cosmetics=Client.INSTANCE.getCosmetics();cosmetics.apply(dev.riserecode.cosmetics.CosmeticLoadout.empty().with(dev.riserecode.cosmetics.CosmeticCategory.COMPANION,dev.riserecode.cosmetics.CosmeticLoadout.Item.defaults("pet-bee")));moving=true;});
                long until=System.nanoTime()+soak*1_000_000_000L;int sample=0;
                while(System.nanoTime()<until){
                    long next=Math.min(until,System.nanoTime()+60_000_000_000L);c.waitFor(mc->System.nanoTime()>=next,7200);
                    int number=sample++;c.runOnClient(mc->{if(number%5==2){Client.INSTANCE.getClickGui().open(null);Client.INSTANCE.getClickGui().selectPage(ClickGui.Page.RESOURCES);scrolling=true;}if(number%5==3){Client.INSTANCE.getClickGui().onClose();mc.gui.setScreen(null);scrolling=false;}if(number%5==0){Client.INSTANCE.getPerformance().mode.setValue(dev.riserecode.performance.PerformancePreferences.Mode.values()[(number/5)%5]);}System.gc();var row=new JsonObject();row.addProperty("sample",number);row.addProperty("heapUsed",java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());row.add("map",new com.google.gson.Gson().toJsonTree(Client.INSTANCE.getMaps().memoryStatistics()));row.add("cosmetics",new com.google.gson.Gson().toJsonTree(Client.INSTANCE.getCosmetics().statistics()));row.add("previews",new com.google.gson.Gson().toJsonTree(dev.riserecode.ui.component.ResourcesPreviews.statistics()));row.addProperty("silhouetteGpuBytes",dev.riserecode.render.SilhouetteOutlineRenderer.ownedGpuBytes());check(Client.INSTANCE.getMaps().statistics().pending()<=16&&Client.INSTANCE.getMaps().statistics().queued()<=16,"soak map work remains bounded");check(Client.INSTANCE.getCosmetics().statistics().pending()<=16,"soak cosmetic work remains bounded");write("soak",row);});
                }
                c.runOnClient(mc->{moving=false;enabled("PlayerESP",false);enabled("Minimap",false);});
            }
            if(!baseline&&Integer.getInteger("riserecode.lifetimeCycles",20)>0){
                world.getServer().runCommand("gamemode spectator @a");world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0.5 100 0.5");
                c.waitFor(mc->mc.level!=null&&mc.level.dimension()==net.minecraft.world.level.Level.NETHER&&Client.INSTANCE.getMaps().dimension().equals("minecraft:the_nether"),1200);
                c.runOnClient(mc->check(Client.INSTANCE.getMaps().statistics().pending()<=16,"dimension transition keeps map queues bounded"));
                world.getServer().runCommand("execute in minecraft:overworld run tp @a 0.5 65 -10.5");
                c.waitFor(mc->mc.level!=null&&mc.level.dimension()==net.minecraft.world.level.Level.OVERWORLD&&Client.INSTANCE.getMaps().dimension().equals("minecraft:overworld"),1200);
                c.runOnClient(mc->check(Client.INSTANCE.getMaps().recording(),"dimension changes preserve recording choice and explored disk cache"));
                // Finish the dimension transition before the Fabric context starts teardown.
                // Closing on the same synthetic tick can strand IntegratedServer.halt's
                // executeBlocking task behind Fabric's server/client phase barrier.
                world.getConnection().waitForChunksRender();c.waitTicks(10);
                try{Thread.sleep(60);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new AssertionError(interrupted);}
            }

        }
        if(Boolean.getBoolean("riserecode.withShaderPack")){c.runOnClient(mc->shaderPack(false));}
        c.runOnClient(mc->{scrolling=recording=false;Client.INSTANCE.getModuleManager().disableAll();});
        Client.INSTANCE.getLogger().info("Remediation native checks passed ({} assertions)",checks);
    }
    private static void shaderPack(boolean enabled){
        try{Class<?> iris=Class.forName("net.irisshaders.iris.Iris");Object config=iris.getMethod("getIrisConfig").invoke(null);config.getClass().getMethod("setShaderPackName",String.class).invoke(config,System.getProperty("riserecode.testShaderPack","ComplementaryUnbound_r5.9.3.zip"));config.getClass().getMethod("setShadersEnabled",boolean.class).invoke(config,enabled);config.getClass().getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);}
        catch(ReflectiveOperationException failure){throw new AssertionError("Iris shader fixture",failure);}
    }
    private static Path evidence(){return Path.of(System.getProperty("riserecode.remediationEvidence","../../../upgrade-evidence/remediation"));}
    private static void measure(ClientGameTestContext c,String scene){
        if(Boolean.getBoolean("riserecode.performanceValidationOnly")){c.waitTicks(5);c.takeScreenshot("validation-"+scene);return;}
        // Warmup is separate from startup and recording. Time is wall-clock, never a frame-count proxy.
        long warmUntil=System.nanoTime()+10_000_000_000L;c.waitFor(mc->System.nanoTime()>=warmUntil,1800);
        try(var jfr=new Recording(Configuration.getConfiguration("profile"))){
            jfr.disable("jdk.InitialEnvironmentVariable");jfr.disable("jdk.InitialSystemProperty");jfr.disable("jdk.SystemProcess");jfr.setName(scene);jfr.start();
            c.runOnClient(mc->{if(!THREADS.isThreadAllocatedMemorySupported()||!THREADS.isCurrentThreadCpuTimeSupported()){throw new AssertionError("Direct render-thread counters required");}THREADS.setThreadAllocatedMemoryEnabled(true);THREADS.setThreadCpuTimeEnabled(true);var collectors=java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();gcCounts=new long[collectors.size()];gcTimes=new long[collectors.size()];for(int i=0;i<collectors.size();i++){gcCounts[i]=collectors.get(i).getCollectionCount();gcTimes[i]=collectors.get(i).getCollectionTime();}frameCount=renderCount=0;allocatedBytes=cpuNanos=0;previous=0;recording=true;});
            long started=System.nanoTime();c.waitFor(mc->System.nanoTime()-started>=30_000_000_000L,3600);
            c.runOnClient(mc->{recording=false;var sorted=java.util.Arrays.copyOf(frames,frameCount);var work=java.util.Arrays.copyOf(renderFrames,renderCount);java.util.Arrays.sort(sorted);java.util.Arrays.sort(work);
                var row=new JsonObject();row.addProperty("warmupSeconds",10);row.addProperty("measurementSeconds",(System.nanoTime()-started)/1e9);row.addProperty("renderFrames",work.length);row.addProperty("renderMedianMs",work[work.length/2]);row.addProperty("renderP95Ms",work[(int)(work.length*.95)]);row.addProperty("renderP99Ms",work[(int)(work.length*.99)]);
                row.addProperty("renderAllocatedBytes",allocatedBytes);row.addProperty("renderAllocatedBytesPerFrame",allocatedBytes/(double)renderCount);row.addProperty("renderCpuNanos",cpuNanos);row.addProperty("renderCpuMsPerFrame",cpuNanos/1e6/renderCount);row.addProperty("scene",scene);row.addProperty("frames",sorted.length);row.addProperty("throttleReason",mc.getFramerateLimitTracker().getThrottleReason().name());row.addProperty("medianMs",sorted[sorted.length/2]);row.addProperty("p95Ms",sorted[(int)(sorted.length*.95)]);row.addProperty("heapUsed",java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
                var gc=new com.google.gson.JsonArray();int collectorIndex=0;for(var collector:java.lang.management.ManagementFactory.getGarbageCollectorMXBeans()){var value=new JsonObject();value.addProperty("name",collector.getName());value.addProperty("collections",collector.getCollectionCount()-gcCounts[collectorIndex]);value.addProperty("milliseconds",collector.getCollectionTime()-gcTimes[collectorIndex++]);gc.add(value);}row.add("gcDuringMeasurement",gc);write("frames",row);
            });
            jfr.stop();Files.createDirectories(evidence());jfr.dump(evidence().resolve(scene+".jfr"));c.takeScreenshot("lightweight-"+scene);
        }catch(Exception failure){throw new AssertionError(failure);}
    }
    private static void write(String file,JsonObject row){try{Files.createDirectories(evidence());Files.writeString(evidence().resolve(file+".jsonl"),row+"\n",java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(java.io.IOException failure){throw new AssertionError(failure);}}
}
