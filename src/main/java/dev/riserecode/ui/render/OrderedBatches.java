package dev.riserecode.ui.render;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/** Stable painter dependencies: a command only passes earlier commands it cannot overlap. */
public final class OrderedBatches {
    private OrderedBatches(){ }
    public static <T> void emit(List<T> commands,BiPredicate<T,T> compatible,BiPredicate<T,T> overlaps,Consumer<List<T>> output){
        plan(commands,compatible,overlaps).emit(commands,output);
    }
    /** Stores indices only. A cached plan always reads the current frame's commands. */
    public static final class Plan {
        private final int size;
        private final List<int[]> runs;
        private Plan(int size,List<int[]> runs){this.size=size;this.runs=List.copyOf(runs);}
        public <T> void emit(List<T> commands,Consumer<List<T>> output){
            if(commands.size()!=size)throw new IllegalArgumentException("Command count changed");
            for(int[] indices:runs){
                List<T> run=new ArrayList<>(indices.length);
                for(int index:indices)run.add(commands.get(index));
                output.accept(run);
            }
            commands.clear();
        }
    }
    /** Keys must include every input to compatibility/overlap, but never frame payloads. */
    public static final class Cache<K> {
        private final int limit;
        private final LinkedHashMap<List<K>,Plan> plans=new LinkedHashMap<>(16,.75f,true);
        public Cache(int limit){if(limit<1)throw new IllegalArgumentException("Empty plan cache");this.limit=limit;}
        public <T> Plan get(List<K> key,List<T> commands,BiPredicate<T,T> compatible,BiPredicate<T,T> overlaps){
            if(key.size()!=commands.size())throw new IllegalArgumentException("Incomplete command key");
            Plan result=plans.get(key);
            if(result==null){
                result=plan(commands,compatible,overlaps);plans.put(List.copyOf(key),result);
                if(plans.size()>limit)plans.remove(plans.keySet().iterator().next());
            }
            return result;
        }
        public void clear(){plans.clear();}
        public int size(){return plans.size();}
    }
    public static <T> Plan plan(List<T> commands,BiPredicate<T,T> compatible,BiPredicate<T,T> overlaps){
        int size=commands.size(),remaining=0;
        List<int[]> runs=new ArrayList<>();
        boolean[] emitted=new boolean[size];
        int[] blockers=new int[size];
        int[] materials=new int[size];
        List<T> representatives=new ArrayList<>();
        java.util.BitSet[] followers=new java.util.BitSet[size];
        for(int i=0;i<size;i++){
            T earlier=commands.get(i);if(earlier==null){emitted[i]=true;continue;}remaining++;
            int material=0;while(material<representatives.size()&&!compatible.test(representatives.get(material),earlier))material++;
            if(material==representatives.size())representatives.add(earlier);materials[i]=material;
            for(int j=i+1;j<size;j++){
                T later=commands.get(j);if(later!=null&&overlaps.test(earlier,later)){
                    if(followers[i]==null)followers[i]=new java.util.BitSet(size);
                    followers[i].set(j);blockers[j]++;
                }
            }
        }
        while(remaining>0){
            int first=0;while(emitted[first])first++;
            int[] indices=new int[remaining];int count=0;
            for(int i=0;i<commands.size();i++){
                if(emitted[i]||materials[first]!=materials[i])continue;
                if(blockers[i]==0){
                    indices[count++]=i;emitted[i]=true;remaining--;
                    var next=followers[i];if(next!=null)for(int j=next.nextSetBit(i+1);j>=0;j=next.nextSetBit(j+1))blockers[j]--;
                }
            }
            runs.add(java.util.Arrays.copyOf(indices,count));
        }
        return new Plan(size,runs);
    }
}
