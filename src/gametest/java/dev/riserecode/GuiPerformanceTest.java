package dev.riserecode;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.DoubleSetting;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;

/** Personal appearance, disposable world, production-JAR A/B. No user instance is modified. */
public final class GuiPerformanceTest {
    private static final boolean ACTIVE=Boolean.getBoolean("riserecode.guiPerformanceOnly");
    private static final double[] TIMES=new double[100_000],GPU=new double[100_000],BLUR=new double[100_000];
    private static final boolean[] PENDING=new boolean[8];
    private static GpuQueryPool queries;
    private static int slot=-1,count,gpuCount,blurCount;
    private static long scrollStarted,lastScroll;
    private static long allocated,cpu;
    public static long guiNanos,elements,draws;
    private static boolean recording,scrolling;
    public static boolean freezeTheme;
    public static boolean freezeVersion;
    public static boolean blurPattern,fullBlur;
    private static int patternScene;
    /** Replace the completed scene with a deterministic GPU checkerboard for pixel comparisons. */
    public static void beforeBlur(com.mojang.blaze3d.pipeline.RenderTarget main){
        if(!blurPattern)return;
        var encoder=RenderSystem.getDevice().createCommandEncoder();
        for(int row=0;row<4;row++)for(int column=0;column<8;column++){
            int left=column*main.width/8,right=(column+1)*main.width/8,bottom=row*main.height/4,top=(row+1)*main.height/4;
            var color=((row+column+patternScene)&1)==0?new org.joml.Vector4f(.9f,.1f,.25f,1):new org.joml.Vector4f(.1f,.8f,.9f,1);
            encoder.createRenderPass(()->"GUI blur reference input",main.getColorTextureView(),java.util.Optional.of(color),null,java.util.OptionalDouble.empty(),new com.mojang.blaze3d.systems.RenderPass.RenderArea(left,bottom,right-left,top-bottom)).close();
        }
        if(fullBlur){try{var field=dev.riserecode.util.BlurUtil.class.getDeclaredField("guiRegion");field.setAccessible(true);field.set(null,null);}catch(NoSuchFieldException baseline){/* 0.24.1 already filters the whole texture. */}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}}
    }
    private static int checks;
    private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    public static void frame(){
        if(!ACTIVE)return;
        var mc=Minecraft.getInstance();mc.getFramerateLimitTracker().onInputReceived();
        long now=System.nanoTime();
        if(scrolling&&now-lastScroll>=50_000_000L){lastScroll=now;var panel=Client.INSTANCE.getClickGui().getActivePanel();var b=panel.getBounds();panel.mouseScrolled(b.centerX(),b.centerY(),((now-scrollStarted)/3_000_000_000L&1)==0?-2:2);}
        if(queries==null)queries=RenderSystem.getDevice().createTimestampQueryPool(32);
        slot=(slot+1)%8;
        if(PENDING[slot]){
            var values=queries.getValues(slot*4,4);
            if(Arrays.stream(values).allMatch(java.util.OptionalLong::isPresent)){
                if(recording&&gpuCount<GPU.length){GPU[gpuCount++]=(values[3].getAsLong()-values[0].getAsLong())/1e6;BLUR[blurCount++]=(values[2].getAsLong()-values[1].getAsLong())/1e6;}
                PENDING[slot]=false;
            }else{slot=-1;}
        }
    }
    public static void gpu(int phase){if(!ACTIVE||slot<0||queries==null)return;RenderSystem.getDevice().createCommandEncoder().writeTimestamp(queries,slot*4+phase);if(phase==3)PENDING[slot]=true;}
    public static void rendered(long nanos,long bytes,long spent){if(recording&&count<TIMES.length){TIMES[count++]=nanos/1e6;allocated+=bytes;cpu+=spent;}}
    private static Path evidence(){return Path.of(System.getProperty("riserecode.guiEvidence","upgrade-evidence/gui"));}
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{
            Client.INSTANCE.getModuleManager().disableAll();mc.options.pauseOnLostFocus=false;mc.options.enableVsync().set(false);
            mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);mc.options.framerateLimit().set(260);mc.options.renderDistance().set(12);mc.options.guiScale().set(3);mc.gui.setScreen(null);
            var threads=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();threads.setThreadAllocatedMemoryEnabled(true);threads.setThreadCpuTimeEnabled(true);
            check(Client.INSTANCE.getPerformance().mode.getValue()==dev.riserecode.performance.PerformancePreferences.Mode.OFF,"full appearance tested with performance reductions Off");
            check(Client.INSTANCE.getThemeManager().choice("Font Selection",dev.riserecode.ui.theme.ThemeManager.Typeface.class)==dev.riserecode.ui.theme.ThemeManager.Typeface.QRAFTYS,"personal Qraftys font loaded");
            check(Client.INSTANCE.getThemeManager().number("GUI Blur Radius")==22,"personal radius-22 blur loaded");
            if(Boolean.getBoolean("riserecode.withShaderPack")){check(dev.riserecode.render.AmbienceRenderer.shaderPackActive(),"Complementary shaders actually active");}
            String ablation=System.getProperty("riserecode.guiAblation","");
            if(ablation.equals("blur"))((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting("GUI Blur Radius")).setValue(0.0);
            if(ablation.equals("text-effects")){((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Global Glow")).setValue(false);((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Text Shadow")).setValue(false);}
        });
        try(var world=c.worldBuilder().create()){
            world.getServer().runOnServer(server->{check(server.getPlayerList().getPlayers().stream().allMatch(player->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(player.nameAndId())),"owner-only disposable cleanup");CameraFovClientTest.ownerOnlyShutdown=true;});
            world.getServer().runCommand("gamemode creative @a");world.getServer().runCommand("gamerule minecraft:advance_time false");world.getServer().runCommand("time set noon");world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -32 62 -32 31 63 31 stone");world.getServer().runCommand("fill -12 64 2 -6 69 8 stone_bricks");world.getServer().runCommand("fill 3 64 2 11 64 10 water");world.getServer().runCommand("tp @a 0.5 65 -12.5 0 0");
            world.getServer().runOnServer(server->server.getPlayerList().setViewDistance(12));
            world.getConnection().waitForChunksRender(false,2400);c.waitTicks(80);
            c.runOnClient(mc->{check(mc.getFramerateLimitTracker().getFramerateLimit()==Integer.MAX_VALUE,"closed fixture uses uncapped limit");check(mc.getFramerateLimitTracker().getThrottleReason().name().equals("NONE"),"no closed activity throttle");});
            measure(c,"closed");
            c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.open(null);gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setValue("");});c.waitTicks(30);measure(c,"search");
            c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getActivePanel().getVisibleButtons().getFirst().getBounds();gui.getActivePanel().mouseClicked(b.centerX(),b.y()+12,1);});c.waitTicks(30);
            c.runOnClient(mc->check(Client.INSTANCE.getClickGui().getActivePanel().getVisibleButtons().getFirst().isExpanded(),"expanded benchmark actually opens module settings"));measure(c,"expanded");
            c.runOnClient(mc->{scrolling=true;scrollStarted=lastScroll=System.nanoTime();});measure(c,"scrolling");c.runOnClient(mc->scrolling=false);
            c.runOnClient(mc->mc.gui.setScreen(null));measure(c,"closed-after");
            c.runOnClient(mc->Client.INSTANCE.getClickGui().open(null));c.waitTicks(30);
            if(Boolean.getBoolean("riserecode.guiValidationOnly"))validate(c);
            c.runOnClient(mc->{recording=false;mc.gui.setScreen(null);if(queries!=null){queries.close();queries=null;}});
            world.getServer().runOnServer(server->{check(server.getPlayerList().getPlayers().stream().allMatch(player->((net.minecraft.client.server.IntegratedServer)server).isSingleplayerOwner(player.nameAndId())),"owner-only disposable cleanup");CameraFovClientTest.ownerOnlyShutdown=true;});c.waitTicks(10);
        }finally{CameraFovClientTest.ownerOnlyShutdown=false;recording=false;}
        Client.INSTANCE.getLogger().info("GUI performance checks passed ({} assertions)",checks);
    }
    private static void measure(ClientGameTestContext c,String state){
        if(Boolean.getBoolean("riserecode.guiValidationOnly")){c.waitTicks(5);c.takeScreenshot("gui-"+state);return;}
        String profileScene=System.getProperty("riserecode.guiProfileScene","");
        if(Boolean.getBoolean("riserecode.guiProfile")&&!profileScene.isEmpty()&&!profileScene.equals(state)){c.waitTicks(5);return;}
        long warmNanos=state.equals("closed-after")?5_000_000_000L:10_000_000_000L;
        long measureNanos=state.equals("closed-after")?10_000_000_000L:30_000_000_000L;
        long warm=System.nanoTime();c.waitFor(mc->System.nanoTime()-warm>=warmNanos,1800);
        try(var jfr=new Recording(Configuration.getConfiguration("profile"))){
            if(Boolean.getBoolean("riserecode.guiProfile")){jfr.disable("jdk.InitialEnvironmentVariable");jfr.disable("jdk.InitialSystemProperty");jfr.disable("jdk.SystemProcess");jfr.start();}
            c.runOnClient(mc->{count=gpuCount=blurCount=0;allocated=cpu=guiNanos=elements=draws=0;recording=true;});
            long start=System.nanoTime();c.waitFor(mc->System.nanoTime()-start>=measureNanos,3600);
            c.runOnClient(mc->{recording=false;check(count>100,"enough timed frames");check(mc.getFramerateLimitTracker().getFramerateLimit()==Integer.MAX_VALUE,"identical uncapped limit");check(mc.getFramerateLimitTracker().getThrottleReason().name().equals("NONE"),"no activity throttling");
                var row=new JsonObject();row.addProperty("label",System.getProperty("riserecode.guiLabel","candidate"));row.addProperty("scene",state);row.addProperty("frames",count);row.addProperty("seconds",(System.nanoTime()-start)/1e9);
                stats(row,"frame",TIMES,count);stats(row,"gpuGui",GPU,gpuCount);stats(row,"gpuBlur",BLUR,blurCount);
                row.addProperty("cpuMsPerFrame",cpu/1e6/count);row.addProperty("allocatedBytesPerFrame",allocated/(double)count);row.addProperty("guiSubmitMsPerFrame",guiNanos/1e6/count);row.addProperty("elementsPerFrame",elements/(double)count);row.addProperty("drawsPerFrame",draws/(double)count);row.addProperty("width",mc.getWindow().getScreenWidth());row.addProperty("height",mc.getWindow().getScreenHeight());row.addProperty("guiScale",mc.getWindow().getGuiScale());
                try{Files.createDirectories(evidence());Files.writeString(evidence().resolve("frames.jsonl"),row+"\n",java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(java.io.IOException failure){throw new AssertionError(failure);}Client.INSTANCE.getLogger().info("GUI benchmark {}",row);
            });
            if(Boolean.getBoolean("riserecode.guiProfile")){jfr.stop();Files.createDirectories(evidence());jfr.dump(evidence().resolve(state+".jfr"));}c.takeScreenshot("gui-"+state);
        }catch(Exception failure){throw new AssertionError(failure);}
    }
    private static void stats(JsonObject row,String prefix,double[] values,int size){if(size==0)return;double[] sorted=Arrays.copyOf(values,size);Arrays.sort(sorted);row.addProperty(prefix+"MedianMs",sorted[size/2]);row.addProperty(prefix+"P95Ms",sorted[(int)(size*.95)]);row.addProperty(prefix+"P99Ms",sorted[(int)(size*.99)]);}
    private static void validate(ClientGameTestContext c){
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.selectPage(ClickGui.Page.SEARCH);gui.getSearchField().setValue("not-a-module-∅∈中文");});c.waitTicks(10);c.takeScreenshot("gui-unicode");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();check(gui.getActivePanel().getVisibleButtons().isEmpty(),"search invalidates filtered layout");gui.getSearchField().setValue("");});c.waitTicks(10);
        for(int i=0;i<20;i++){c.runOnClient(mc->{mc.gui.setScreen(null);Client.INSTANCE.getClickGui().open(null);});c.waitTicks(2);}
        c.getInput().resizeWindow(1537,865);c.waitTicks(10);c.takeScreenshot("gui-fractional-resize");
        c.runOnClient(mc->{var b=Client.INSTANCE.getClickGui().getWindowBounds();check(Double.isFinite(b.width())&&b.width()>0,"resize retains finite logical window");});
        final double[] previousWidth={0};
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();var b=gui.getWindowBounds();previousWidth[0]=b.width();double scale=gui.getCanvasScale(),x=(b.x()+b.width()-3)*scale,y=(b.y()+b.height()-3)*scale;var info=new net.minecraft.client.input.MouseButtonInfo(0,0);
            gui.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,info),false);gui.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(x+8,y+5,info),8,5);gui.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(x+8,y+5,info));});c.waitTicks(15);
        c.runOnClient(mc->check(Client.INSTANCE.getClickGui().getWindowBounds().width()>previousWidth[0],"resize hit testing updates cached window layout"));c.takeScreenshot("gui-drag-resize");
        c.runOnClient(mc->{var gui=Client.INSTANCE.getClickGui();gui.getSearchField().setValue("");var b=gui.getActivePanel().getVisibleButtons().getFirst().getBounds();var info=new net.minecraft.client.input.MouseButtonInfo(1,0);double scale=gui.getCanvasScale();boolean before=gui.getActivePanel().getVisibleButtons().getFirst().isExpanded();var click=new net.minecraft.client.input.MouseButtonEvent(b.centerX()*scale,(b.y()+10)*scale,info);gui.mouseClicked(click,false);gui.mouseReleased(click);check(gui.getActivePanel().getVisibleButtons().getFirst().isExpanded()!=before,"module click tracks resized displayed geometry");});c.waitTicks(8);
        c.getInput().resizeWindow(2048,1152);c.waitTicks(10);
        c.waitFor(mc->Client.INSTANCE.getClickGui().isOpen(),240);
        c.runOnClient(mc->{freezeTheme=freezeVersion=blurPattern=true;patternScene=0;});c.waitTicks(16);c.takeScreenshot("gui-whole-window-qraftys");
        c.runOnClient(mc->{blurPattern=false;});
        c.runOnClient(mc->{freezeTheme=true;mc.gui.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("GUI fidelity")){
            @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta){g.fill(0,0,width,height,0xFF182431);}
            @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta){
                String[] lines={"Qrafty's glyphs ABCDEFG abcdefg 0123456789","Unicode: ∅ ∈ 中文 🚀 … ✓","Thin stems: iii lll !!! [] {} ()","Spacing: WWW MMM www mmm"};
                for(int i=0;i<lines.length;i++){dev.riserecode.util.RenderUtil.text(g,font,lines[i],30,28+i*32,1,0xFFFFFFFF);}
                dev.riserecode.util.RenderUtil.accentText(g,font,"Gradient, glow and shadow",30,174,1,1);
                dev.riserecode.util.RenderUtil.scissorPush(g,30,210,250,28);
                try{dev.riserecode.util.RenderUtil.accentText(g,font,"Clipped glow and Unicode 中文 ends",22,215,1.3,.8);}finally{dev.riserecode.util.RenderUtil.scissorPop(g);}
                dev.riserecode.util.RenderUtil.text(g,font,"Fractional scale and opacity",30.25,265.75,.83,0x8FFFFFFF);
                var fading=dev.riserecode.ui.menu.MenuTransition.widget(g,0,0,1,.37);
                try{
                    dev.riserecode.util.RenderUtil.scissorPush(g,35,300,225,25);
                    dev.riserecode.ui.render.UiBatch.begin(g);
                    try{dev.riserecode.util.RenderUtil.text(g,font,"Faded fallback 中文 and bitmap",30.25,308.75,.83,0xFFFFFFFF);}
                    finally{dev.riserecode.ui.render.UiBatch.end();dev.riserecode.util.RenderUtil.scissorPop(g);}
                }finally{fading.close();}
            }
        });});c.waitTicks(5);c.takeScreenshot("gui-fidelity-qraftys");
        c.runOnClient(mc->{@SuppressWarnings("unchecked") var face=(dev.riserecode.setting.EnumSetting<dev.riserecode.ui.theme.ThemeManager.Typeface>)Client.INSTANCE.getThemeManager().getSetting("Font Selection");face.setValue(dev.riserecode.ui.theme.ThemeManager.Typeface.INTER);});c.waitTicks(5);c.takeScreenshot("gui-fidelity-inter");
        c.runOnClient(mc->{long generation=dev.riserecode.ui.font.CustomTextRenderer.generation();dev.riserecode.ui.font.CustomTextRenderer.close();check(dev.riserecode.ui.font.CustomTextRenderer.generation()>generation&&dev.riserecode.ui.font.CustomTextRenderer.cachedLayouts()==0,"font reload discards cached geometry");});c.waitTicks(5);c.takeScreenshot("gui-fidelity-reloaded");
        c.runOnClient(mc->{check(dev.riserecode.ui.font.CustomTextRenderer.cachedLayouts()<=2048,"layout cache remains bounded");freezeTheme=false;});
        var reload=c.computeOnClient(mc->mc.reloadResourcePacks());c.waitFor(mc->reload.isDone()&&mc.gui.overlay()==null&&Client.INSTANCE.resourcesReady(),1200);
        c.runOnClient(mc->{check(!reload.isCompletedExceptionally(),"resource reload succeeds with cached text and blur targets");freezeTheme=true;});c.waitTicks(16);c.takeScreenshot("gui-fidelity-resource-reload");
        c.runOnClient(mc->{freezeTheme=true;blurPattern=true;});
        for(int scene=0;scene<3;scene++){
            if(scene==2){c.getInput().resizeWindow(1537,865);c.waitTicks(10);}
            final int value=scene;
            c.runOnClient(mc->{patternScene=value;fullBlur=false;mc.gui.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Blur regions")){
                @Override public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta){ }
                @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int x,int y,float delta){
                    g.pose().pushMatrix();g.pose().translate(value*17.25f,value*9.5f);g.pose().scale(value==1?.83f:1.07f,value==1?.83f:1.07f);
                    try{dev.riserecode.util.BlurUtil.drawBox(g,20,20,150,100,8,1);dev.riserecode.util.BlurUtil.drawBox(g,width-170,height-180,140,150,14,1);dev.riserecode.util.BlurUtil.drawBox(g,-30,height-50,100,90,8,1);}finally{g.pose().popMatrix();}
                }
            });});c.waitTicks(16);c.takeScreenshot("gui-blur-roi-"+scene);
            c.runOnClient(mc->fullBlur=true);c.waitTicks(4);c.takeScreenshot("gui-blur-full-"+scene);
        }
        c.runOnClient(mc->{blurPattern=fullBlur=freezeTheme=freezeVersion=false;});
    }
}
