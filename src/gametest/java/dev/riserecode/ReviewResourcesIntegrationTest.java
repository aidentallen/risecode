package dev.riserecode;

import com.mojang.blaze3d.platform.InputConstants;
import dev.riserecode.resources.*;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.setting.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/** Pointer-free recovery at small/large-font sizes and finite real reload/cache retention cycles. */
public final class ReviewResourcesIntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static ResourcesManager manager(){return Client.INSTANCE.getResourcesManager();}
    private static ClickGui gui(){return Client.INSTANCE.getClickGui();}
    private static void key(ClientGameTestContext c,int code){KeyboardReviewIntegrationTest.key(c,code);}
    private static void focus(ClientGameTestContext c,String prefix){KeyboardReviewIntegrationTest.focus(c,prefix);}
    private static void press(ClientGameTestContext c,String prefix){focus(c,prefix);key(c,InputConstants.KEY_RETURN);}
    private static ResourcesState state(String... ids){return new ResourcesState(manager().library().catalog().revision(),List.of(ids));}
    private static void settled(ClientGameTestContext c){c.waitFor(mc->!manager().busy()&&mc.gui.overlay()==null,1200);c.waitTicks(3);}
    private static void select(ClientGameTestContext c,String id){
        press(c,"Search resources…");KeyboardReviewIntegrationTest.type(c,manager().library().catalog().byId().get(id).name());c.waitTicks(3);
        press(c,"Resource, "+manager().library().catalog().byId().get(id).name()+",");
    }
    private static void keyboardFlow(ClientGameTestContext c,double fontScale){
        c.runOnClient(mc->{((DoubleSetting)Client.INSTANCE.getThemeManager().getSetting("Font Scale")).setValue(fontScale);gui().open(null);gui().selectPage(ClickGui.Page.RESOURCES);});c.waitTicks(20);
        if(c.computeOnClient(mc->gui().accessibleControlLabels().stream().anyMatch(label->label.startsWith("Browse, button")))){press(c,"Browse, button");}
        select(c,"StemToLog");select(c,"DirtPathToGrassPath");
        press(c,"Selected (2), button");press(c,"StemToLog, move resource up");
        var expected=c.computeOnClient(mc->gui().getResourcesEditor().draft());
        check(expected.selected().getFirst().equals("StemToLog"),"Pointer-free explicit resource reorder at font scale "+fontScale);
        press(c,"Apply, button");settled(c);check(manager().state().equals(expected),"Keyboard applies exact ordered resource draft");
        press(c,"Browse, button");select(c,"CherryPicking");
        c.runOnClient(mc->ResourcesTestBootstrap.failNextReload.set(true));press(c,"Apply, button");settled(c);
        check(manager().state().equals(expected)&&manager().status().startsWith("Previous resources restored"),"Keyboard-triggered failed reload restores prior ordered selection");
        check(c.computeOnClient(mc->mc.getNarrator().isActive()),"Native narrator available for contextual recovery status dispatch");
        press(c,"Cancel, button");
        check(c.computeOnClient(mc->gui().getResourcesEditor().draft().equals(expected)),"Keyboard discards failed draft after recovery");
        press(c,"Selected (2), button");press(c,"Clear, button");press(c,"Apply, button");settled(c);
        check(manager().state().selected().isEmpty(),"Keyboard can clear and apply recovered resources");
        c.takeScreenshot("review-resources-keyboard-font-"+fontScale);
        key(c,InputConstants.KEY_ESCAPE);c.waitFor(mc->mc.gui.screen()!=gui(),240);
        check(!gui().isOpen(),"Keyboard returns after resource recovery without pointer");
    }
    private static ResourcesManager.ApplyResult apply(ClientGameTestContext c,ResourcesState state){
        var result=c.computeOnClient(mc->manager().apply(state));c.waitFor(mc->result.isDone(),1200);settled(c);return result.join();
    }
    private record Sample(int cycle,long directories,long diskBytes,long gpuTextures,long gpuViews,long gpuBuffers,long fontNativeBytes,long heapUsed){ }
    private static Sample sample(ClientGameTestContext c,int cycle,ReviewGpuIntegrationTest.Fault probe){
        return c.computeOnClient(mc->{
            long directories=0,bytes=0;Path generations=manager().library().directory().resolve("generations");
            try{
                if(Files.isDirectory(generations)){
                    try(var files=Files.list(generations)){directories=files.filter(Files::isDirectory).count();}
                    try(var files=Files.walk(generations)){for(var file:files.filter(Files::isRegularFile).toList()){bytes+=Files.size(file);}}
                }
            }catch(java.io.IOException failure){throw new AssertionError(failure);}
            var sample=new Sample(cycle,directories,bytes,probe.liveTextures(),probe.liveViews(),probe.liveBuffers(),ReviewGpuIntegrationTest.fontNativeBytes(),java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
            probe.discardClosed();return sample;
        });
    }
    private static void stress(ClientGameTestContext c){
        var samples=new ArrayList<Sample>();
        var probe=c.computeOnClient(mc->{try{return new ReviewGpuIntegrationTest.Fault(ReviewGpuIntegrationTest.Phase.NONE);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);}});
        try{
            long textureBudget=0,viewBudget=0,bufferBudget=0,nativeBudget=0,diskBudget=0,heapBudget=0;
            for(int cycle=0;cycle<10;cycle++){
                var desired=cycle%2==0?state("StemToLog"):state("DirtPathToGrassPath");
                check(apply(c,desired).success(),"Stress normal Apply completes, cycle "+cycle);
                c.runOnClient(mc->ResourcesTestBootstrap.failNextReload.set(true));
                check(!apply(c,state("CherryPicking")).success()&&manager().state().equals(desired),"Stress failed Apply/recovery preserves selection, cycle "+cycle);
                c.waitTicks(8);c.runOnClient(mc->System.gc());c.waitTicks(3);
                var current=sample(c,cycle,probe);samples.add(current);
                check(current.directories()<=3,"Active/rollback/failed generation bound, cycle "+cycle);
                if(cycle<2){
                    textureBudget=Math.max(textureBudget,current.gpuTextures()+8);viewBudget=Math.max(viewBudget,current.gpuViews()+8);bufferBudget=Math.max(bufferBudget,current.gpuBuffers()+8);
                    nativeBudget=Math.max(nativeBudget,current.fontNativeBytes()+4096);diskBudget=Math.max(diskBudget,Math.max(1_048_576,current.diskBytes()*2));heapBudget=Math.max(heapBudget,current.heapUsed()+16*1024*1024);
                }else{
                    check(current.gpuTextures()<=textureBudget&&current.gpuViews()<=viewBudget&&current.gpuBuffers()<=bufferBudget,"Post-warmup actual live GPU handles stay within declared baseline tolerance");
                    check(current.fontNativeBytes()<=nativeBudget&&current.diskBytes()<=diskBudget&&current.heapUsed()<=heapBudget,"Post-GC native/disk/heap samples remain within baseline-derived budgets");
                }
                Client.INSTANCE.getLogger().info("Resources retention cycle {}",current);
            }
            var report=new com.google.gson.JsonObject();report.add("samples",new com.google.gson.Gson().toJsonTree(samples));
            report.addProperty("scope","Two warmup cycles then eight checked cycles; actual backend handle flags and font LWJGL allocator reporting; explicit GC heap includes engine/test state; closed probe metadata pruned. No long soak or driver-global VRAM claim.");
            report.addProperty("textureBudget",textureBudget);report.addProperty("viewBudget",viewBudget);report.addProperty("bufferBudget",bufferBudget);report.addProperty("fontNativeBudget",nativeBudget);report.addProperty("diskBudget",diskBudget);report.addProperty("heapBudget",heapBudget);
            try{Files.writeString(Path.of("review-resources-retention.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}catch(java.io.IOException failure){throw new AssertionError(failure);}
        }finally{c.runOnClient(mc->probe.close());}
        check(apply(c,ResourcesState.empty()).success(),"Clear test resources after bounded stress");
    }
    public static void run(ClientGameTestContext c){
        c.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        c.runOnClient(mc->{
            check(manager().library().progress().ready(),"Complete offline fixture cache supplied");Client.INSTANCE.getModuleManager().disableAll();
            ((BooleanSetting)Client.INSTANCE.getThemeManager().getSetting("Reduced Motion")).setValue(true);
            mc.options.narrator().set(net.minecraft.client.NarratorStatus.ALL);mc.options.pauseOnLostFocus=false;
        });
        c.getInput().resizeWindow(1280,800);c.runOnClient(mc->mc.options.guiScale().set(2));c.waitTicks(5);
        keyboardFlow(c,1);keyboardFlow(c,1.35);
        stress(c);
        Client.INSTANCE.getLogger().info("Review Resources keyboard/stress native checks passed ({} assertions)",checks);
    }
}
