package dev.riserecode;

import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.setting.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import com.google.gson.*;

/** Paired warm UI costs. Render-frame work excludes Fabric barriers; intervals explicitly include them. */
public final class UxPerformanceTest {
    private static final boolean ENABLED=Boolean.getBoolean("riserecode.uxPerformanceOnly");
    public static boolean recording;
    private static int frames,extracts,intervals;private static final double[] work=new double[100_000],extract=new double[100_000],interval=new double[100_000];
    private static long bytes,cpu,extractBytes,previous,lastAction,start;
    private static String scene="";
    public static void rendered(long nanos,long allocated,long spent){if(ENABLED&&recording&&frames<work.length){work[frames++]=nanos/1e6;bytes+=allocated;cpu+=spent;}}
    public static void extracted(long nanos,long allocated){if(ENABLED&&recording&&extracts<extract.length){extract[extracts++]=nanos/1e6;extractBytes+=allocated;}}
    public static void frame(){if(!ENABLED)return;var mc=net.minecraft.client.Minecraft.getInstance();mc.getFramerateLimitTracker().onInputReceived();long now=System.nanoTime();if(recording&&previous!=0&&intervals<interval.length)interval[intervals++]=(now-previous)/1e6;previous=now;
        if(now-lastAction<100_000_000L)return;lastAction=now;var gui=Client.INSTANCE.getClickGui();
        if(scene.equals("appearance")){var e=gui.getThemeEditor();var b=e.getBounds();e.mouseScrolled(b.centerX(),b.centerY(),((now-start)/2_000_000_000L&1)==0?-2:2);}
        if(scene.equals("resources")){var e=gui.getResourcesEditor();e.search().setValue(((now-start)/2_000_000_000L&1)==0?"":"block");gui.keyPressed(new net.minecraft.client.input.KeyEvent(258,0,0));}
    }
    private static long gcCount(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionCount())).sum();}
    private static long gcMillis(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum();}
    private static void stats(JsonObject out,String prefix,double[] values,int n){if(n==0)return;var copy=Arrays.copyOf(values,n);Arrays.sort(copy);out.addProperty(prefix+"MedianMs",copy[n/2]);out.addProperty(prefix+"P95Ms",copy[(int)(n*.95)]);out.addProperty(prefix+"P99Ms",copy[(int)(n*.99)]);out.addProperty(prefix+"Over16_7",Arrays.stream(copy).filter(v->v>16.7).count());out.addProperty(prefix+"Over33_3",Arrays.stream(copy).filter(v->v>33.3).count());}
    private static void number(String key,double value){((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting(key)).setValue(value);}
    public static void run(ClientGameTestContext c){c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);String label=System.getProperty("riserecode.uxPerfLabel","candidate");Path root=Path.of(System.getProperty("riserecode.uxPerfEvidence","/tmp/riserecode-ux-perf"));
        c.getInput().resizeWindow(1280,960);c.runOnClient(mc->{mc.options.guiScale().set(2);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(60);mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);mc.options.pauseOnLostFocus=false;Client.INSTANCE.getModuleManager().disableAll();Client.INSTANCE.getThemeManager().reset();number("Window Width",600);number("Window Height",420);number("Font Scale",1);((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(false);var threads=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();threads.setThreadAllocatedMemoryEnabled(true);threads.setThreadCpuTimeEnabled(true);Client.INSTANCE.getClickGui().open(null);});c.waitTicks(30);
        var meta=c.computeOnClient(mc->{var j=new JsonObject();j.addProperty("label",label);j.addProperty("java",System.getProperty("java.version"));j.addProperty("processors",Runtime.getRuntime().availableProcessors());j.addProperty("width",mc.getWindow().getScreenWidth());j.addProperty("height",mc.getWindow().getScreenHeight());j.addProperty("guiScale",mc.getWindow().getGuiScale());j.addProperty("requestedFpsCap",60);j.addProperty("testOnlyCapOverride",Integer.getInteger("riserecode.uxPerfCap",0));j.addProperty("effectiveLimit",mc.getFramerateLimitTracker().getFramerateLimit());j.addProperty("vsync",mc.options.enableVsync().get());var mode=org.lwjgl.glfw.GLFW.glfwGetVideoMode(org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor());j.addProperty("monitorHz",mode==null?0:mode.refreshRate());j.addProperty("renderer",com.mojang.blaze3d.systems.RenderSystem.getDevice().getDeviceInfo().toString());j.addProperty("boundary","renderFrame elapsed includes work, present and any configured limiter, excludes Fabric barriers; intervals include harness; extraction and thread CPU measured separately; no GPU timing or display-latency claim");return j;});
        try{Files.createDirectories(root);Files.writeString(root.resolve(label+"-environment.json"),new GsonBuilder().setPrettyPrinting().create().toJson(meta));}catch(Exception e){throw new RuntimeException(e);}
        jdk.jfr.Recording jfr=null;try{jfr=new jdk.jfr.Recording(jdk.jfr.Configuration.getConfiguration("profile"));for(String event:List.of("jdk.InitialEnvironmentVariable","jdk.InitialSystemProperty","jdk.SystemProcess"))jfr.disable(event);jfr.start();
        for(String next:List.of("idle","appearance","resources","preview")){
            c.runOnClient(mc->{recording=false;scene="";var gui=Client.INSTANCE.getClickGui();if(mc.gui.screen()!=gui){mc.gui.screen().onClose();}if(next.equals("idle")){gui.selectPage(ClickGui.Page.COMBAT);}if(next.equals("appearance")){gui.selectPage(ClickGui.Page.APPEARANCE);gui.getSearchField().setValue("");((ColorPicker)gui.getThemeEditor().getControl("Primary Accent")).setExpanded(true);gui.getThemeEditor().reveal("Primary Accent");}if(next.equals("resources")){gui.selectPage(ClickGui.Page.RESOURCES);gui.getResourcesEditor().setCategory("All categories");gui.getResourcesEditor().search().setValue("");if(Client.INSTANCE.getResourcesManager().library().catalog().entries().size()<600)throw new AssertionError("Full resource catalog required");}if(next.equals("preview")){gui.selectPage(ClickGui.Page.COSMETICS);var e=gui.getCosmeticsEditor();e.select("wings-dragon");e.select("pet-bee");e.detailsButton().activate();}scene=next;start=System.nanoTime();});
            c.waitTicks(100);
            for(int trial=0;trial<3;trial++){long beforeCount=gcCount(),beforeMillis=gcMillis();c.runOnClient(mc->{frames=extracts=intervals=0;bytes=cpu=extractBytes=previous=0;recording=true;start=System.nanoTime();});long begin=System.nanoTime();c.waitFor(mc->System.nanoTime()-begin>=30_000_000_000L,2400);
                final int repetition=trial;var row=c.computeOnClient(mc->{recording=false;if(frames<300||extracts<100)throw new AssertionError("Insufficient native render/extraction samples");var j=new JsonObject();j.addProperty("label",label);j.addProperty("scene",next);j.addProperty("trial",repetition+1);j.addProperty("frames",frames);j.addProperty("extractions",extracts);j.addProperty("seconds",(System.nanoTime()-begin)/1e9);stats(j,"renderWork",work,frames);stats(j,"extraction",extract,extracts);stats(j,"harnessInterval",interval,intervals);j.addProperty("allocatedBytesPerFrame",bytes/(double)frames);j.addProperty("cpuMsPerFrame",cpu/1e6/frames);j.addProperty("extractionBytesPerCall",extractBytes/(double)extracts);j.addProperty("gcCollections",gcCount()-beforeCount);j.addProperty("gcMillis",gcMillis()-beforeMillis);j.addProperty("effectiveLimit",mc.getFramerateLimitTracker().getFramerateLimit());j.addProperty("throttleReason",mc.getFramerateLimitTracker().getThrottleReason().name());j.addProperty("systemLoadAverage",ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());return j;});Files.writeString(root.resolve(label+"-trials.jsonl"),row+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);Client.INSTANCE.getLogger().info("UX warm benchmark {}",row);c.waitTicks(20);
            }c.takeScreenshot("ux-perf-"+label+"-"+next);
        }
        }catch(Exception error){throw new RuntimeException(error);}finally{recording=false;scene="";if(jfr!=null){try{jfr.stop();jfr.dump(root.resolve(label+".jfr"));jfr.close();}catch(Exception ignored){}}c.runOnClient(mc->mc.gui.setScreen(null));}
    }
}
