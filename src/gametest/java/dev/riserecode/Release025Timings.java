package dev.riserecode;

import com.mojang.blaze3d.systems.*;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Diagnostic shared-host costs, separated by CPU, GPU and wall frame time. Never FPS evidence. */
public final class Release025Timings {
    private static final Map<String,List<double[]>> CPU=new LinkedHashMap<>();
    private static final Map<String,List<Double>> GPU=new LinkedHashMap<>();
    private static final String[] labels=new String[16];
    private static GpuQueryPool queries;
    private static int slot=-1;
    private static String scene="";
    private static boolean finished;
    public static void scene(String value){scene=value;}
    public static void begin(){
        if(finished||!Boolean.getBoolean("riserecode.release025Only"))return;
        if(queries==null)queries=RenderSystem.getDevice().createTimestampQueryPool(32);
        slot=(slot+1)%16;
        if(labels[slot]!=null){
            var values=queries.getValues(slot*2,2);
            if(values[0].isEmpty()||values[1].isEmpty()){slot=-1;return;}
            GPU.computeIfAbsent(labels[slot],ignored->new ArrayList<>()).add((values[1].getAsLong()-values[0].getAsLong())/1e6);labels[slot]=null;
        }
        if(!scene.isEmpty())RenderSystem.getDevice().createCommandEncoder().writeTimestamp(queries,slot*2);
    }
    public static void end(){if(queries!=null&&slot>=0&&!scene.isEmpty()){RenderSystem.getDevice().createCommandEncoder().writeTimestamp(queries,slot*2+1);labels[slot]=scene;}}
    public static void rendered(long wall,long bytes,long cpu){if(!scene.isEmpty())CPU.computeIfAbsent(scene,ignored->new ArrayList<>()).add(new double[]{wall/1e6,cpu/1e6,bytes});}
    public static void save(){
        var result=new JsonObject();result.addProperty("interpretation","Diagnostic shared-host samples. CPU/GPU costs are separate; FPS comparisons are inconclusive.");var scenes=new JsonObject();
        CPU.forEach((label,values)->{var data=new JsonObject();data.addProperty("frames",values.size());data.addProperty("wallFrameMs",values.stream().mapToDouble(v->v[0]).average().orElse(0));data.addProperty("renderThreadCpuMs",values.stream().mapToDouble(v->v[1]).average().orElse(0));data.addProperty("allocatedBytesPerFrame",values.stream().mapToDouble(v->v[2]).average().orElse(0));data.addProperty("gpuMs",GPU.getOrDefault(label,List.of()).stream().mapToDouble(v->v).average().orElse(-1));data.addProperty("gpuSamples",GPU.getOrDefault(label,List.of()).size());scenes.add(label,data);});result.add("scenes",scenes);
        try{Files.writeString(Path.of("025-costs.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(java.io.IOException e){throw new AssertionError(e);}
        finished=true;if(queries!=null){queries.close();queries=null;}
    }
}
