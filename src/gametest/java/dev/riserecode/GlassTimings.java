package dev.riserecode;

import com.google.gson.*;
import com.mojang.blaze3d.systems.*;
import dev.riserecode.render.glass.GlassRenderer;
import java.nio.file.*;
import java.util.*;

/** Diagnostic full-frame samples. Warm-up is controlled by the native test, never included. */
public final class GlassTimings {
    private static final Map<String,List<double[]>> CPU=new LinkedHashMap<>();
    private static final Map<String,List<Double>> GPU=new LinkedHashMap<>();
    private static final Map<String,Long> MEMORY=new LinkedHashMap<>();
    private static final String[] LABELS=new String[32];
    private static GpuQueryPool queries;
    private static int next,slot=-1;
    private static String scene="";
    private GlassTimings() { }
    public static void scene(String label){scene=label;if(!label.isEmpty())MEMORY.put(label,GlassRenderer.ownedGpuBytes());}
    public static void begin(){
        if(!Boolean.getBoolean("riserecode.glassOnly"))return;
        if(queries==null)queries=RenderSystem.getDevice().createTimestampQueryPool(64);
        slot=next;next=(next+1)%32;
        if(LABELS[slot]!=null){
            var values=queries.getValues(slot*2,2);
            if(values[0].isEmpty()||values[1].isEmpty()){slot=-1;return;}
            double period=RenderSystem.getDevice().getDeviceInfo().timestampPeriod();
            GPU.computeIfAbsent(LABELS[slot],ignored->new ArrayList<>()).add((values[1].getAsLong()-values[0].getAsLong())*period/1e6);LABELS[slot]=null;
        }
        if(!scene.isEmpty()){LABELS[slot]=scene;RenderSystem.getDevice().createCommandEncoder().writeTimestamp(queries,slot*2);}
    }
    public static void end(){if(queries!=null&&slot>=0&&LABELS[slot]!=null)RenderSystem.getDevice().createCommandEncoder().writeTimestamp(queries,slot*2+1);}
    public static void rendered(long wall,long bytes,long cpu){if(!scene.isEmpty())CPU.computeIfAbsent(scene,ignored->new ArrayList<>()).add(new double[]{wall/1e6,cpu/1e6,bytes});}
    public static void save(){
        var result=new JsonObject();result.addProperty("interpretation","Warmed diagnostic frame costs on a shared host; includes the entire rendered frame. Owned Glass texture/buffer bytes exclude driver overhead. These are not isolated effect GPU costs or an FPS guarantee.");var scenes=new JsonObject();
        CPU.forEach((label,values)->{var data=new JsonObject();data.addProperty("frames",values.size());data.addProperty("wallFrameMeanMs",values.stream().mapToDouble(v->v[0]).average().orElse(0));data.addProperty("renderThreadCpuMeanMs",values.stream().mapToDouble(v->v[1]).average().orElse(0));data.addProperty("allocatedBytesPerFrame",values.stream().mapToDouble(v->v[2]).average().orElse(0));data.addProperty("gpuFrameMeanMs",GPU.getOrDefault(label,List.of()).stream().mapToDouble(v->v).average().orElse(-1));data.addProperty("gpuSamples",GPU.getOrDefault(label,List.of()).size());data.addProperty("ownedGlassGpuBytes",MEMORY.get(label));scenes.add(label,data);});result.add("scenes",scenes);
        try{Files.writeString(Path.of("027-costs.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(java.io.IOException e){throw new AssertionError(e);}
        if(queries!=null){queries.close();queries=null;}
    }
}
