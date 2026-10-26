package dev.riserecode;

import com.google.gson.JsonObject;
import dev.riserecode.performance.*;
import dev.riserecode.performance.PerformancePreferences.Mode;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.ui.render.FramePinnedCache;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.module.modules.SprintModule;
import dev.riserecode.module.modules.FullBrightModule;
import dev.riserecode.render.PackedGeometry;
import java.util.*;
import org.slf4j.LoggerFactory;

/** Global policy transitions, independent budgets, packed compatibility and deferred ownership. */
public final class PerformanceChecks {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok){throw new AssertionError(message);}}
    public static void main(String[] args){
        var saves=new ArrayList<JsonObject>();var prefs=new PerformancePreferences(new JsonObject(),saves::add);
        check(prefs.mode.getValue()==Mode.OFF&&prefs.effective().equals(PerformancePolicy.OFF),"default Off preserves existing choices");
        for(var mode:List.of(Mode.CONSERVATIVE,Mode.BALANCED,Mode.STRONG)){
            prefs.mode.setValue(mode);var p=prefs.effective();
            check(p.blur()&&p.glow()&&p.softShadows()&&p.menuParticles(),"preset suppresses expensive GUI effects");
            check(p.reducedDetail()==(mode!=Mode.CONSERVATIVE),"detail matches preset");
            check(p.suppressTrails()==(mode==Mode.STRONG),"trails match preset");
            check(p.particleDensity()==(mode==Mode.STRONG?PerformancePolicy.Density.OFF:mode==Mode.BALANCED?PerformancePolicy.Density.HALF:PerformancePolicy.Density.FULL),"density matches preset");
            check(p.mapColumns()==16&&p.mapCaptureNanos()==500000&&p.mapCpuEntries()==256&&p.mapCpuBytes()==32L*1024*1024&&p.mapGpuEntries()==128&&p.previewEntries()==24&&p.previewPending()==8&&p.unusedCosmeticEntries()==8&&p.unusedCosmeticBytes()==32L*1024*1024,"independent reduced budgets match specification");
            ((BooleanSetting)prefs.options().get(0)).setValue(false);
            check(prefs.mode.getValue()==Mode.CUSTOM&&!prefs.effective().blur()&&prefs.effective().particleDensity()==p.particleDensity()&&prefs.effective().suppressTrails()==p.suppressTrails(),"edit copies effective preset then changes one control");
            var custom=prefs.custom();prefs.mode.setValue(Mode.OFF);check(prefs.effective().equals(PerformancePolicy.OFF),"Off removes every reduction");prefs.mode.setValue(Mode.BALANCED);prefs.mode.setValue(Mode.CUSTOM);check(prefs.effective().equals(custom),"preset changes retain last Custom");
        }
        for(int i=0;i<prefs.options().size();i++){
            prefs.mode.setValue(Mode.OFF);var option=prefs.options().get(i);
            if(option instanceof BooleanSetting b){b.setValue(true);var actual=prefs.effective();check(prefs.mode.getValue()==Mode.CUSTOM,"individual control creates Custom");for(int j=0;j<prefs.options().size();j++){if(prefs.options().get(j) instanceof BooleanSetting other){check(other.getValue()==(j==i),"each control acts independently");}}}
        }
        prefs.mode.setValue(Mode.OFF);dev.riserecode.ui.theme.ThemeManager.assign(prefs.options().get(5),PerformancePolicy.Density.HALF);
        check(prefs.mode.getValue()==Mode.CUSTOM&&prefs.effective().particleDensity()==PerformancePolicy.Density.HALF&&!prefs.effective().blur()&&!prefs.effective().mapCpu(),"density edit independently copies Off into Custom");
        prefs.mode.setValue(Mode.STRONG);var immutable=PerformancePolicy.current();PerformancePolicy.beginExtraction();prefs.mode.setValue(Mode.OFF);check(PerformancePolicy.frame()==immutable&&PerformancePolicy.current().equals(PerformancePolicy.OFF),"in-flight extraction keeps one immutable policy");PerformancePolicy.beginExtraction();
        var restored=new PerformancePreferences(prefs.snapshot(),ignored->{});check(restored.mode.getValue()==prefs.mode.getValue()&&restored.custom().equals(prefs.custom()),"global preferences round trip including dormant Custom");
        var corrupt=new JsonObject();corrupt.addProperty("mode","INVALID");corrupt.addProperty("custom",false);check(new PerformancePreferences(corrupt,ignored->{}).effective().equals(PerformancePolicy.OFF),"malformed additive preferences retain defaults");
        check(!saves.isEmpty(),"UI edits persist immediately");
        var releases=new ArrayList<String>();var cache=new FramePinnedCache<Integer,String>(4,releases::add);
        for(int i=0;i<4;i++){cache.put(i,"v"+i);}cache.maximum(1);check(cache.size()==4&&cache.overage()==3&&releases.isEmpty(),"shrinking mid-extraction preserves all pins and reports floor");
        cache.remove(0);cache.putPinned(0,"replacement");check(releases.isEmpty(),"same-frame replacement keeps old deferred ID alive");
        cache.discard();check(releases.isEmpty(),"reload retires current frame resources");cache.beginFrame();check(releases.size()==5&&new HashSet<>(releases).size()==5,"each retired resource released exactly once at safe boundary");
        cache.maximum(4);for(int i=0;i<4;i++){cache.put(i,"again"+i);}cache.beginFrame();cache.get(2);cache.maximum(1);check(cache.size()==1&&cache.get(2).equals("again2"),"shrinking evicts unused entries before pinned working set");
        var builder=new PackedGeometry.Builder();for(int i=0;i<128;i++){builder.line(i,1,2,i+1,3,4,i,i+2,2,i%2==0);}
        var point=new net.minecraft.world.phys.Vec3(1,2,3);builder.quad(point,point.add(1,0,0),point.add(1,1,0),point.add(0,1,0),1,2,3,4,false);
        var packed=builder.snapshot();builder.line(0,0,0,0,1,0,0,0,1,true);
        check(packed.lineCount()==128&&packed.lineCount(true)==64&&packed.lineCount(false)==64,"packed immutable snapshot partitions depth once");
        for(int i=0;i<128;i++){var line=packed.lines().get(i);check(line.from().x==i&&line.to().x==i+1&&line.color()==i&&line.endColor()==i+2&&line.width()==2&&line.visibleOnly()==(i%2==0),"compatibility view preserves original primitive order, gradients and width");}
        check(packed.lines()==packed.lines()&&packed.quads()==packed.quads(),"compatibility materializes once on demand");check(packed.quads().getFirst().colorD()==4&&!packed.quads().getFirst().visibleOnly(),"packed quads preserve corner colors and depth");
        try{packed.lines().clear();throw new AssertionError("mutable compatibility");}catch(UnsupportedOperationException expected){checks++;}
        try{builder.line(Double.NaN,0,0,0,1,0,0,0,1,true);throw new AssertionError("nonfinite");}catch(IllegalArgumentException expected){checks++;}
        var modules=new ModuleManager(new EventBus(),LoggerFactory.getLogger("performance"));
        check(modules.getByClass(SprintModule.class).isEmpty(),"negative class lookup cached");var sprint=modules.register(new SprintModule(new EventBus(),null));check(modules.getByClass(SprintModule.class).orElseThrow()==sprint,"registration invalidates negative lookup");
        check(modules.getByClass(dev.riserecode.module.Module.class).orElseThrow()==sprint,"base type resolves first registered match");modules.register(new FullBrightModule(new EventBus(),null));check(modules.getByClass(dev.riserecode.module.Module.class).orElseThrow()==sprint,"cache invalidation retains first-match semantics");
        PerformancePolicy.publish(PerformancePolicy.OFF);PerformancePolicy.beginExtraction();
        System.out.println("Performance checks passed ("+checks+" assertions).");
    }
}
