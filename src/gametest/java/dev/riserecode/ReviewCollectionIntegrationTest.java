package dev.riserecode;

import dev.riserecode.seedcracker.*;
import dev.riserecode.seedcracker.finder.Finder;
import dev.riserecode.seedcracker.cracker.storage.DataStorage;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** Actual admission/fair-yield/observation/archive path with fixed positive and mutable layouts. */
public final class ReviewCollectionIntegrationTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value){throw new AssertionError(message);}}
    private static final class Metrics {
        final com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        long wall,cpu,bytes;int maxQueue;final List<Long> samples=new ArrayList<>();
        void tick(CollectionQueue queue){
            maxQueue=Math.max(maxQueue,queue.pending());long thread=Thread.currentThread().threadId(),b=bean.getThreadAllocatedBytes(thread),c=bean.getCurrentThreadCpuTime(),n=System.nanoTime();
            queue.tick();long elapsed=System.nanoTime()-n;samples.add(elapsed);wall=Math.max(wall,elapsed);cpu=Math.max(cpu,bean.getCurrentThreadCpuTime()-c);bytes=Math.max(bytes,bean.getThreadAllocatedBytes(thread)-b);
        }
    }
    private static Set<Observation> whole(net.minecraft.client.multiplayer.ClientLevel level,List<Finder.Type> types,DataStorage data){
        data.clear();
        for(var type:types){for(var finder:type.finderBuilder.build(level,new ChunkPos(0,0))){var at=finder.getChunkPos();if(level.hasChunk(at.x(),at.z())&&finder.isValidDimension(level.dimensionType())){finder.findInChunk();}}}
        return Set.copyOf(data.observations());
    }
    public static void run(ClientGameTestContext context){
        context.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        context.runOnClient(mc->Client.INSTANCE.getModuleManager().disableAll());
        try(var world=context.worldBuilder().create()){
            world.getConnection().waitForChunksRender();
            context.runOnClient(mc->{
                Features.init(dev.riserecode.seedcracker.config.Config.get().getVersion());
                var enabled=new EnumMap<Finder.Type,Boolean>(Finder.Type.class);for(var type:Finder.Type.values()){enabled.put(type,type.enabled.get());type.enabled.set(false);}
                var data=new DataStorage();SeedCracker.bind(new SeedCracker(data,()->{}));
                try{
                    var queue=new CollectionQueue();queue.world(mc.level);
                    // Two distinct positive structure layouts: dry treasure holder and mast shipwreck neighborhood.
                    mc.level.setBlock(new BlockPos(9,63,9),Blocks.STONE.defaultBlockState(),3);mc.level.setBlock(new BlockPos(9,64,9),Blocks.CHEST.defaultBlockState(),3);
                    var ship=new BlockPos(4,64,5);mc.level.setBlock(ship,Blocks.CHEST.defaultBlockState(),3);
                    for(int i=0;i<5;i++){mc.level.setBlock(ship.offset(i%3-1,2,i/3-1),Blocks.OAK_TRAPDOOR.defaultBlockState(),3);}
                    var types=List.of(Finder.Type.BURIED_TREASURE,Finder.Type.SHIPWRECK);types.forEach(type->type.enabled.set(true));
                    var expected=Set.of(Observation.of(Features.BURIED_TREASURE.at(0,0)),Observation.of(Features.SHIPWRECK.at(0,-1)));
                    var actual=whole(mc.level,types,data);check(actual.equals(expected),"Unsliced fixed treasure/mast layouts yield exact expected structures: "+actual);
                    data.clear();queue.chunk(ChunkPos.pack(0,0));var metrics=new Metrics();
                    int ticks=0;while(queue.pending()>0&&ticks++<3000){metrics.tick(queue);}
                    check(queue.pending()==0&&ticks>2,"Actual collection admission yields and drains both finder lanes");
                    check(Set.copyOf(data.observations()).equals(expected),"Actual sliced queue agrees with unsliced positive observations");
                    Path root=Files.createTempDirectory("risecode-positive-archive-");
                    try{
                        var store=new SeedStore(root);var identity=new SeedStore.Identity("positive-fixture",0,"26.2/native");
                        try(var archive=new SeedArchiveWorker(store)){archive.save(new SeedStore.Saved(1,identity,data.observations(),List.of(),List.of(),List.of()),false).get(5,java.util.concurrent.TimeUnit.SECONDS);}
                        check(Set.copyOf(store.restore(identity).observations()).equals(expected),"Positive queue observations survive actual archive and reopen");
                    }finally{try(var paths=Files.walk(root)){for(var p:paths.sorted(Comparator.reverseOrder()).toList()){Files.deleteIfExists(p);}}}
                    // Begin a real long scan, then replace its chest before candidate consumption.
                    data.clear();queue.restartPending();Finder.Type.BURIED_TREASURE.enabled.set(false);queue.chunk(ChunkPos.pack(0,0));metrics.tick(queue);
                    check(queue.pending()>0,"Mutable layout pauses inside actual queued scan");
                    mc.level.setBlock(ship,Blocks.AIR.defaultBlockState(),3);
                    ticks=0;while(queue.pending()>0&&ticks++<3000){metrics.tick(queue);}
                    check(queue.pending()==0&&data.observations().isEmpty(),"Queued mutation retires stale candidate without a false ship observation");
                    check(whole(mc.level,List.of(Finder.Type.SHIPWRECK),data).isEmpty(),"Stable post-mutation unsliced layout also rejects missing chest");
                    metrics.samples.sort(Long::compare);
                    Client.INSTANCE.getLogger().info("CollectionQueue end-to-end: slices={}, maxQueue={}, max wall={} ns, max thread CPU={} ns, max allocation={} bytes, wall p50/p95/p99={}/{}/{} ns; excludes whole-frame/GPU timing",metrics.samples.size(),metrics.maxQueue,metrics.wall,metrics.cpu,metrics.bytes,metrics.samples.get(metrics.samples.size()/2),metrics.samples.get((int)(metrics.samples.size()*.95)),metrics.samples.get((int)(metrics.samples.size()*.99)));
                    check(metrics.maxQueue==2,"Fixed collection workload queue bound is two admitted lanes");queue.clear();
                }catch(Exception failure){throw new AssertionError(failure);}
                finally{SeedCracker.bind(null);enabled.forEach((type,value)->type.enabled.set(value));}
            });
        }
        Client.INSTANCE.getLogger().info("Review collection native checks passed ({} assertions)",checks);
    }
}
