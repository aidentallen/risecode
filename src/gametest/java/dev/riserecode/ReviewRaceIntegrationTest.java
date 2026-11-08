package dev.riserecode;

import dev.riserecode.event.PacketEvent;
import dev.riserecode.module.modules.NoFallModule;
import dev.riserecode.module.modules.SeedCrackerModule;
import dev.riserecode.seedcracker.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.concurrent.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Deterministic client/packet interleavings and actual archive rejection/IO failure recovery. */
public final class ReviewRaceIntegrationTest {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
    private static Field field(Object owner, String name) throws ReflectiveOperationException { Field field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field; }
    private static Object get(Object owner,String name) { try{return field(owner,name).get(owner);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);} }
    private static void set(Object owner,String name,Object value) { try{field(owner,name).set(owner,value);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);} }
    private static void save(SeedCrackerModule module) { try{var method=module.getClass().getDeclaredMethod("save");method.setAccessible(true);method.invoke(module);}catch(ReflectiveOperationException failure){throw new AssertionError(failure);} }
    private static void awaitSave(SeedCrackerModule module) {
        try { ((CompletableFuture<?>)get(module,"pendingSave")).get(5,TimeUnit.SECONDS); }
        catch(Exception failure){throw new AssertionError("Archive save failed",failure);}
    }
    public static void run(ClientGameTestContext context) {
        context.waitFor(mc->Client.INSTANCE.isInitialized()&&Client.INSTANCE.resourcesReady()&&mc.gui.overlay()==null,1200);
        context.runOnClient(mc->Client.INSTANCE.getModuleManager().disableAll());
        try(var world=context.worldBuilder().adjustSettings(settings->settings.setSeed("456")).create()) {
            world.getConnection().waitForChunksRender();
            var seed=Client.INSTANCE.getModuleManager().getByClass(SeedCrackerModule.class).orElseThrow();
            context.runOnClient(mc->{
                seed.setEnabled(true);
                for(var setting:seed.getSettings()){if(setting instanceof dev.riserecode.setting.BooleanSetting bool&&setting.getName().startsWith("Find ")){bool.setValue(false);}}
                seed.data().addBaseData(Features.DESERT_PYRAMID.at(0,0),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_LIFTING);save(seed);
            });awaitSave(seed);context.waitTicks(2);
            var store=(SeedStore)get(seed,"store");var identity=(SeedStore.Identity)get(seed,"identity");Path file=store.path(identity),backup=file.resolveSibling("review-prior.json.bak");
            resultPersistence(context,seed,store,identity);
            preparationClose(context);
            try {
                Files.move(file,backup);Files.createDirectory(file);
                context.runOnClient(mc->seed.clearData());
                context.waitFor(mc->seed.status().contains("Could not save data"),200);
                Files.delete(file);Files.move(backup,file);
                context.runOnClient(mc->save(seed));awaitSave(seed);context.waitTicks(2);
                check(store.restore(identity).observations().isEmpty(),"Empty clear retries after IO recovery without new observations");
                context.runOnClient(mc->{seed.data().addBaseData(Features.DESERT_PYRAMID.at(0,0),dev.riserecode.seedcracker.cracker.DataAddedEvent.POKE_LIFTING);save(seed);});awaitSave(seed);context.waitTicks(2);
                ((SeedArchiveWorker)get(seed,"archiveWorker")).close();
                context.runOnClient(mc->seed.clearData());context.waitTicks(2);
                check(((CompletableFuture<?>)get(seed,"pendingSave")).isCompletedExceptionally(),"Clear encountered rejected executor");
                context.runOnClient(mc->{set(seed,"archiveWorker",new SeedArchiveWorker(store));seed.setEnabled(false);});
                check(store.restore(identity).observations().isEmpty(),"Disable retries rejected empty clear and flushes it");
            } catch(java.io.IOException failure){throw new AssertionError(failure);}
            finally { try{if(Files.exists(backup)){if(Files.isDirectory(file)){Files.delete(file);}Files.move(backup,file,StandardCopyOption.REPLACE_EXISTING);}}catch(java.io.IOException failure){throw new AssertionError(failure);} }
            mapFlushes(context);
            staleCandidates(context);
            denseCandidates(context);
            clutch(context);
        }
        Client.INSTANCE.getLogger().info("Review race native checks passed ({} assertions)",checks);
    }
    private static void invoke(Object owner,String name,Class<?>[] types,Object... args) {
        try{var method=owner.getClass().getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(owner,args);}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static void resultPersistence(ClientGameTestContext context,SeedCrackerModule module,SeedStore store,SeedStore.Identity identity) {
        context.runOnClient(mc->{
            var job=new CrackingJob(new CrackingJob.Snapshot(module.data().observations(),java.util.List.of(),identity.hash(),java.util.List.of()),1);
            set(job,"finalProgress",new CrackingJob.Progress("Complete",1,1,1,java.util.List.of(123L),java.util.List.of(),true,false,"Complete"));
            set(module,"job",job);set(module,"jobSession",get(module,"session"));set(module,"resultAccepted",false);
            invoke(module,"acceptResult",new Class<?>[]{});job.close();save(module);
        });awaitSave(module);context.waitTicks(2);
        try {check(store.restore(identity).structureSeeds().equals(java.util.List.of(123L)),"Manual result persists after successful same-observation autosave");}
        catch(java.io.IOException failure){throw new AssertionError(failure);}
        var blocker=new CountDownLatch(1);var entered=new CountDownLatch(1);
        context.runOnClient(mc->{
            var worker=(ThreadPoolExecutor)get(get(module,"archiveWorker"),"worker");
            worker.execute(()->{entered.countDown();try{blocker.await();}catch(InterruptedException failure){Thread.currentThread().interrupt();}});
            set(module,"structureSeeds",java.util.List.of(789L));save(module);
            invoke(module,"acceptLookupResults",new Class<?>[]{java.util.List.class},java.util.List.of(456L));
        });
        try{check(entered.await(2,TimeUnit.SECONDS),"Earlier snapshot held behind actual archive worker barrier");}
        catch(InterruptedException failure){throw new AssertionError(failure);}
        blocker.countDown();awaitSave(module);context.waitTicks(2);
        context.runOnClient(mc->save(module));awaitSave(module);context.waitTicks(2);
        try {check(store.restore(identity).verifiedSeeds().equals(java.util.List.of(456L)),"Earlier acknowledgment cannot hide newer result-only lookup payload");}
        catch(java.io.IOException failure){throw new AssertionError(failure);}
        context.runOnClient(mc->{set(module,"structureSeeds",java.util.List.of(1001L));module.setEnabled(false);});
        try{check(store.restore(identity).structureSeeds().equals(java.util.List.of(1001L)),"Disable flushes latest result without new observations");}
        catch(java.io.IOException failure){throw new AssertionError(failure);}
        context.runOnClient(mc->module.setEnabled(true));
    }
    private static void preparationClose(ClientGameTestContext context) {
        var release=new CountDownLatch(1);var entered=new CountDownLatch(2);
        var manager=context.computeOnClient(mc->{
            try{return new dev.riserecode.resources.ResourcesManager(Files.createTempDirectory("risecode-prepare-close-"),new dev.riserecode.event.EventBus());}
            catch(java.io.IOException failure){throw new AssertionError(failure);}
        });
        var library=manager.library();
        var a=library.work(()->{entered.countDown();release.await();return 1;});
        var b=library.work(()->{entered.countDown();release.await();return 2;});
        try{check(entered.await(2,TimeUnit.SECONDS),"Both actual resource IO lanes occupied");}
        catch(InterruptedException failure){throw new AssertionError(failure);}
        var result=context.computeOnClient(mc->manager.apply(dev.riserecode.resources.ResourcesState.empty()));
        check(!result.isDone(),"Apply preparation queued behind occupied workers");
        context.runOnClient(mc->manager.close());
        check(result.isDone()&&!result.join().success()&&!manager.busy(),"Close terminates admitted Apply before client-loop progress");
        check(a.isDone()&&b.isDone(),"Close terminates running library futures too");
        release.countDown();context.waitTicks(2);
        check(manager.state().equals(dev.riserecode.resources.ResourcesState.empty()),"No late preparation commits after close");
        check(library.work(()->3).isCompletedExceptionally(),"Post-close executor admission returns terminal failure");
        try(var paths=Files.walk(library.directory().getParent().getParent())){for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList()){Files.deleteIfExists(path);}}
        catch(java.io.IOException failure){throw new AssertionError(failure);}
    }
    @SuppressWarnings("unchecked")
    private static void mapFlushes(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            for (boolean ready : new boolean[]{false,true}) { for(boolean transition:new boolean[]{false,true}) {
                Path root;
                try { root=Files.createTempDirectory("risecode-map-native-"); }
                catch(java.io.IOException failure){throw new AssertionError(failure);}
                var bus=new dev.riserecode.event.EventBus();
                var map=new dev.riserecode.map.MapService(root,bus,mc);
                Path directory=root.resolve("old-world"),file=directory.resolve("waypoints.json");
                try {
                    set(map,"worldDirectory",directory);set(map,"world","old-world");set(map,"waypointsReady",ready);set(map,"recording",false);
                    if(ready){map.waypoints().death(41,65,42,"minecraft:overworld",123);}
                    else {
                        var type=Class.forName("dev.riserecode.map.MapService$PendingDeath");var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
                        ((java.util.ArrayDeque<Object>)get(map,"bufferedDeaths")).add(constructor.newInstance(41,65,42,"minecraft:overworld",123L));
                    }
                    var queue=(BlockingQueue<Object>)get(map,"completed");
                    var type=Class.forName("dev.riserecode.map.MapService$Completion");var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
                    Object completion=constructor.newInstance(0L,(Runnable)()->{});
                    while(queue.offer(completion)){ /* Fill actual completion lane. */ }
                    var worker=(ThreadPoolExecutor)get(map,"worker");var reached=new CountDownLatch(1);
                    worker.execute(()->{reached.countDown();try{queue.put(completion);}catch(InterruptedException stopped){Thread.currentThread().interrupt();}});
                    check(reached.await(2,TimeUnit.SECONDS),"Map worker blocks on saturated completion lane");
                    for(int i=0;i<16;i++){worker.execute(()->{});}
                    if(transition) {
                        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
                        do { bus.post(new dev.riserecode.event.UpdateEvent(mc,1));Thread.sleep(1); }
                        while(get(map,"level")!=mc.level&&System.nanoTime()<deadline);
                        check(get(map,"level")==mc.level,"Map transition waits for acknowledged old-world flush");
                    }
                    map.close();
                    var reopened=new dev.riserecode.map.WaypointStore();reopened.load(dev.riserecode.config.JsonStorage.read(file));
                    check(reopened.all().stream().anyMatch(w->w.x()==41&&w.z()==42),"Actual saturated map flush persists death; ready="+ready+", transition="+transition);
                } catch(Exception failure){throw new AssertionError(failure);}
                finally {
                    if(!((ThreadPoolExecutor)get(map,"worker")).isShutdown()){map.close();}
                    try(var files=Files.walk(root)){for(Path path:files.sorted(java.util.Comparator.reverseOrder()).toList()){Files.deleteIfExists(path);}}
                    catch(java.io.IOException failure){throw new AssertionError(failure);}
                }
            } }
        });
    }
    @SuppressWarnings("try")
    private static void staleCandidates(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            var chunk = new net.minecraft.world.level.ChunkPos(0, 0);
            var local = new BlockPos(9, 64, 9);
            for (var finder : java.util.List.of(
                    new dev.riserecode.seedcracker.finder.structure.BuriedTreasureFinder(mc.level, chunk),
                    new dev.riserecode.seedcracker.finder.structure.ShipwreckFinder(mc.level, chunk))) {
                try {
                    var positions = dev.riserecode.seedcracker.finder.BlockFinder.class.getDeclaredField("searchPositions");
                    positions.setAccessible(true); positions.set(finder, java.util.List.of(local));
                    mc.level.setBlock(local, Blocks.CHEST.defaultBlockState(), 3);
                    boolean yielded = false;
                    try (var budget = new FinderBudget(1, 1_000_000_000L)) { finder.findInChunk(); }
                    catch (FinderBudget.Yield expected) { yielded = true; }
                    check(yielded, "Candidate validation resumes after scan quota for " + finder.getClass().getSimpleName());
                    mc.level.setBlock(local, Blocks.AIR.defaultBlockState(), 3);
                    try (var budget = new FinderBudget(2048, 1_000_000_000L)) {
                        check(finder.findInChunk().isEmpty(), "Replaced chest is rejected without property exception for " + finder.getClass().getSimpleName());
                    }
                } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
            }
        });
    }
    @SuppressWarnings("try")
    private static void denseCandidates(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            SeedCracker.bind(new SeedCracker(new dev.riserecode.seedcracker.cracker.storage.DataStorage(), () -> {}));
            var positions = new java.util.ArrayList<BlockPos>();
            for (int y=64;y<72;y++) { for(int z=0;z<16;z++) { for(int x=0;x<16;x++) {
                var pos=new BlockPos(x,y,z);positions.add(pos);mc.level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
            } } }
            var immutable=java.util.List.copyOf(positions);
            var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
            long thread=Thread.currentThread().threadId();
            long maxNanos=0,maxBytes=0,constructionNanos=0;int totalSlices=0;
            try {
                var field=dev.riserecode.seedcracker.finder.BlockFinder.class.getDeclaredField("searchPositions");field.setAccessible(true);
                for(int sample=0;sample<4;sample++) {
                    long start=System.nanoTime();
                    var sliced=new dev.riserecode.seedcracker.finder.structure.ShipwreckFinder(mc.level,new net.minecraft.world.level.ChunkPos(0,0));
                    var whole=new dev.riserecode.seedcracker.finder.structure.ShipwreckFinder(mc.level,new net.minecraft.world.level.ChunkPos(0,0));
                    constructionNanos=Math.max(constructionNanos,System.nanoTime()-start);
                    field.set(sliced,immutable);field.set(whole,immutable);
                    java.util.List<BlockPos> actual=null;int slices=0;
                    while(actual==null&&slices<2048) {
                        long bytes=bean.getThreadAllocatedBytes(thread);start=System.nanoTime();
                        try(var budget=new FinderBudget(2048,1_500_000)){actual=sliced.findInChunk();}
                        catch(FinderBudget.Yield expected){ /* Resume the same immutable candidate owner. */ }
                        maxNanos=Math.max(maxNanos,System.nanoTime()-start);maxBytes=Math.max(maxBytes,bean.getThreadAllocatedBytes(thread)-bytes);slices++;
                    }
                    totalSlices+=slices;
                    check(actual!=null&&actual.equals(whole.findInChunk()),"Dense unchanged sliced/unsliced candidates agree, sample "+sample);
                }
            } catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
            finally { SeedCracker.bind(null); }
            Client.INSTANCE.getLogger().info("Finder stress: 2048 chest candidates, 4 samples, {} slices, maximum client-call {} ns, maximum thread allocation {} bytes, maximum two-finder construction {} ns; excludes GPU/frame time",totalSlices,maxNanos,maxBytes,constructionNanos);
        });
    }
    private static void clutch(ClientGameTestContext context) {
        var clutch=Client.INSTANCE.getModuleManager().getByClass(NoFallModule.class).orElseThrow();
        context.runOnClient(mc->{
            clutch.getMode().setValue(NoFallModule.Mode.WATER);clutch.setEnabled(true);
            mc.player.setPos(0,120,0);mc.player.setOnGround(false);mc.player.setDeltaMovement(0,-.2,0);mc.player.fallDistance=20;
            set(clutch,"previousMode",NoFallModule.Mode.WATER);set(clutch,"observedLevel",mc.level);set(clutch,"observedPlayer",mc.player);
            try {
                var type=Class.forName("dev.riserecode.module.modules.NoFallModule$Attempt");var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
                var pos=new BlockPos(0,64,0);var hit=new BlockHitResult(new Vec3(0,64,0),Direction.UP,pos.below(),false);
                Object a=constructor.newInstance(NoFallModule.Mode.WATER,pos,hit,InteractionHand.MAIN_HAND,0,Items.WATER_BUCKET);
                Object b=constructor.newInstance(NoFallModule.Mode.WATER,pos,hit,InteractionHand.MAIN_HAND,0,Items.WATER_BUCKET);
                set(clutch,"attempt",a);set(clutch,"requested",true);set(clutch,"acknowledged",false);
                var packet=new ClientboundBlockUpdatePacket(pos,Blocks.WATER.defaultBlockState());
                var source=mc.getConnection().getConnection();
                Thread netty=Thread.ofPlatform().start(()->Client.INSTANCE.getEventBus().post(new PacketEvent.Receive(packet,source)));
                netty.join();set(clutch,"attempt",b);
            }catch(ReflectiveOperationException|InterruptedException failure){throw new AssertionError(failure);}
        });
        context.runOnClient(mc->{
            check(!(boolean)get(clutch,"acknowledged"),"Old attempt cannot confirm replacement at same position");
            try {
                var type=Class.forName("dev.riserecode.module.modules.NoFallModule$Attempt");var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);
                var pos=new BlockPos(0,64,0);var hit=new BlockHitResult(new Vec3(0,64,0),Direction.UP,pos.below(),false);
                set(clutch,"attempt",constructor.newInstance(NoFallModule.Mode.WATER,pos,hit,InteractionHand.MAIN_HAND,0,Items.WATER_BUCKET));
                set(clutch,"requested",true);set(clutch,"observedLevel",mc.level);set(clutch,"observedPlayer",mc.player);set(clutch,"acknowledged",false);
                var packet=new ClientboundBlockUpdatePacket(pos,Blocks.WATER.defaultBlockState());
                Client.INSTANCE.getEventBus().post(new PacketEvent.Receive(packet,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.CLIENTBOUND)));
                check(!(boolean)get(clutch,"acknowledged"),"Different connection cannot acknowledge current attempt");
                Client.INSTANCE.getEventBus().post(new PacketEvent.Receive(packet,mc.getConnection().getConnection()));
                check((boolean)get(clutch,"acknowledged"),"Current connection can acknowledge current attempt");
                clutch.getMode().setValue(NoFallModule.Mode.COBWEB);
                check(get(clutch,"attempt")==null&&!(boolean)get(clutch,"acknowledged"),"Mode change clears pending attempt and acknowledgment immediately");
            } catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
            clutch.setEnabled(false);
        });
    }
}
