package dev.riserecode;

import dev.riserecode.config.JsonStorage;
import dev.riserecode.map.RetainedWrite;
import dev.riserecode.map.WaypointStore;
import dev.riserecode.render.RendererCapabilities;
import dev.riserecode.seedcracker.CandidateCursor;
import dev.riserecode.seedcracker.FinderBudget;
import dev.riserecode.util.AllocationScope;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Saturated ordered writes, cooperative downstream work, capability and allocation rollback regressions. */
public final class ReviewPersistenceChecks {
    private static int checks;
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("risecode-flush-review-");
        var executor = new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1));
        var completions = new ArrayBlockingQueue<Integer>(1); completions.put(1);
        var blocked = new CountDownLatch(1);
        try {
            executor.execute(() -> { blocked.countDown(); try { completions.put(2); } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); } });
            check(blocked.await(2,TimeUnit.SECONDS), "Worker reached full completion lane");
            executor.execute(() -> { });
            var calls = new AtomicInteger();
            var clock = new AtomicLong();
            Path file = directory.resolve("waypoints.json");
            var old = new WaypointStore(); JsonStorage.write(file,old.snapshot());
            var flush = new RetainedWrite(() -> {
                if (calls.getAndIncrement() == 0) { throw new java.io.IOException("Injected one-shot failure"); }
                var store = new WaypointStore(); store.load(JsonStorage.read(file, 8*1024*1024));
                store.death(1,64,2,"minecraft:overworld",1234); JsonStorage.write(file,store.snapshot());
            },clock::get);
            check(!flush.progress(executor) && flush.failure() instanceof RejectedExecutionException, "Full worker lane retains unadmitted death flush");
            completions.poll();
            long deadline = System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
            boolean finished = false;
            while (!(finished = flush.progress(executor)) && System.nanoTime() < deadline) { completions.poll(); clock.addAndGet(250_000_000); Thread.sleep(1); }
            check(finished && calls.get() == 2, "Retained control write retries IO failure after saturation");
            var reopened = new WaypointStore(); reopened.load(JsonStorage.read(file));
            check(reopened.all().stream().anyMatch(w -> w.x()==1 && w.z()==2), "Reopened waypoint index contains buffered death");
            Files.writeString(file," ".repeat(100));
            boolean capped = false; try { JsonStorage.read(file,32); } catch (java.io.IOException expected) { capped = true; }
            check(capped,"Local JSON acquisition is bounded before parse");
            candidateAndAllocation(); recovery(); boundedCaches(directory);
        } finally { completions.poll(); executor.shutdownNow(); executor.awaitTermination(2,TimeUnit.SECONDS); try (var files=Files.walk(directory)) { for(Path path:files.sorted(Comparator.reverseOrder()).toList()){Files.deleteIfExists(path);} } }
        check(RendererCapabilities.resolveMotion(null,null,true), "Vanilla motion supported");
        check(!RendererCapabilities.resolveMotion("unsupported",null,true), "Unsupported Sodium disables shared motion");
        check(!RendererCapabilities.resolveMotion("0.9.2+mc26.2","unsupported",true), "Unsupported Iris disables shared motion");
        check(!RendererCapabilities.resolveMotion("0.9.2+mc26.2",null,false), "Signature failure disables shared motion");
        check(RendererCapabilities.resolveMotion("0.9.2+mc26.2","1.11.4+mc26.2",true), "Pinned signatures preserve motion");
        var policyField=dev.riserecode.render.ChunkTerrainRenderer.class.getDeclaredField("policy");policyField.setAccessible(true);
        Object previousPolicy=policyField.get(null);boolean previousCapability=RendererCapabilities.chunkAdapters;
        try {
            var tracker=new dev.riserecode.render.ChunkMotionTracker();
            var settings=new dev.riserecode.render.ChunkMotionTracker.Settings(dev.riserecode.render.ChunkMotionTracker.Direction.UP,8,.5,dev.riserecode.render.ChunkMotionTracker.Curve.OUT_QUART);
            tracker.loaded(dev.riserecode.render.ChunkMotionTracker.key(2,3));tracker.frame(1_000_000_000L,settings);
            var policy=new dev.riserecode.event.ChunkRenderPolicyEvent();policy.animate(tracker,settings);policyField.set(null,policy);
            RendererCapabilities.chunkAdapters=RendererCapabilities.resolveMotion("unsupported",null,true);
            check(dev.riserecode.render.ChunkTerrainRenderer.offset(2,3)==0&&!dev.riserecode.render.ChunkTerrainRenderer.motionActive(),"Unsupported shared capability prevents dependent block-entity offsets");
        } finally {policyField.set(null,previousPolicy);RendererCapabilities.chunkAdapters=previousCapability;}
        System.out.println("Review persistence checks passed ("+checks+" assertions).");
    }
    private static void sparse(Path path,long size) throws java.io.IOException {
        Files.createDirectories(path.getParent());
        try(var channel=Files.newByteChannel(path,StandardOpenOption.CREATE,StandardOpenOption.WRITE)){channel.position(size-1);channel.write(java.nio.ByteBuffer.wrap(new byte[]{0}));}
    }
    private static void boundedCaches(Path directory) throws Exception {
        Path growing=directory.resolve("growing");Files.write(growing,new byte[8192]);
        try(var input=new java.io.FilterInputStream(Files.newInputStream(growing)) {
            boolean appended;
            @Override public int read(byte[] bytes,int offset,int length) throws java.io.IOException {
                int count=super.read(bytes,offset,length);
                if(!appended&&count>0){appended=true;Files.write(growing,new byte[8192],StandardOpenOption.APPEND);}return count;
            }
        }) {
            boolean rejected=false;try{dev.riserecode.util.BoundedInput.read(input,8192);}catch(java.io.IOException expected){rejected=true;}
            check(rejected,"Growing file cannot bypass stream acquisition cap");
        }
        for(String name:List.of("catalog.json","imports.json")) {
            Path root=directory.resolve(name+"-cache");sparse(root.resolve(name),1_048_577);
            try(var cosmetics=new dev.riserecode.cosmetics.CosmeticsLibrary(root,(path,limit)->{throw new AssertionError("Unexpected network");})) {
                check(cosmetics.status().contains("unavailable"),"Oversized cosmetics metadata preserves bundled fallback: "+name);
            }
        }
        Path root=directory.resolve("resources");String revision="a".repeat(64);
        Files.createDirectories(root);Files.writeString(root.resolve("catalog-pointer.json"),"{\"revision\":\""+revision+"\"}");
        sparse(root.resolve("revisions").resolve(revision).resolve("catalog.json"),4*1024*1024+1);
        try(var library=new dev.riserecode.resources.ResourcesLibrary(root,88,0)) {
            check(library.catalog()==null&&library.progress().phase().contains("unavailable"),"Oversized resource catalog fails before parse with retry status");
            sparse(root.resolve("objects").resolve(revision),8*1024*1024+1);
            var read=library.getClass().getDeclaredMethod("read",String.class,int.class);read.setAccessible(true);
            boolean rejected=false;
            try{read.invoke(library,revision,8*1024*1024);}catch(java.lang.reflect.InvocationTargetException expected){rejected=expected.getCause() instanceof java.io.IOException;}
            check(rejected,"Oversized resource thumbnail rejects bounded acquisition");
            var store=library.getClass().getDeclaredMethod("store",byte[].class);store.setAccessible(true);
            byte[] valid={1,2,3};String hash=dev.riserecode.resources.ResourceIo.hash(valid);
            sparse(root.resolve("objects").resolve(hash),100);
            store.invoke(library,(Object)valid);
            check(Arrays.equals(Files.readAllBytes(root.resolve("objects").resolve(hash)),valid),"Repair replaces only owned corrupt object atomically");
        }
        check(dev.riserecode.util.BoundedInput.read(new java.io.ByteArrayInputStream(new byte[8192]),8192).length==8192,"Exactly maximum sized input remains valid");
    }
    private static void recovery() {
        for (boolean succeed : new boolean[]{true,false}) {
            var reload=new CompletableFuture<Void>();var outcomes=new ArrayList<dev.riserecode.resources.ReloadRecovery.Outcome>();
            var state=new dev.riserecode.resources.ReloadRecovery(Runnable::run,()->reload,outcomes::add);
            var primary=new IllegalStateException("primary");var secondary=new IllegalStateException("recovery");
            check(state.fail(primary)&&state.fail(primary),"Duplicate primary event remains owned during recovery");
            if(succeed){reload.complete(null);}else{reload.completeExceptionally(secondary);}
            state.fail(secondary);
            check(outcomes.size()==1&&outcomes.getFirst().restored()==succeed&&outcomes.getFirst().original()==primary,"Recovery completion terminates once and preserves original outcome");
            if(!succeed){check(outcomes.getFirst().recovery()==secondary,"Exceptional completion without event remains a failure");}
        }
        var outcomes=new ArrayList<dev.riserecode.resources.ReloadRecovery.Outcome>();
        var sync=new dev.riserecode.resources.ReloadRecovery(Runnable::run,()->{throw new IllegalStateException("synchronous");},outcomes::add);
        sync.fail(new IllegalStateException("primary"));check(outcomes.size()==1&&!outcomes.getFirst().restored(),"Synchronous recovery start failure terminates accurately");
        var pending=new CompletableFuture<Void>();outcomes.clear();
        var closing=new dev.riserecode.resources.ReloadRecovery(Runnable::run,()->pending,outcomes::add);
        closing.fail(new IllegalStateException("primary"));closing.cancel();pending.complete(null);
        check(outcomes.isEmpty(),"Late recovery completion cannot override close outcome");
    }
    private static void candidateAndAllocation() {
        var candidates = new CandidateCursor<Integer>(); var consumed = new ArrayList<Integer>();
        for (int slice=0;slice<3;slice++) {
            var budget = new FinderBudget(128,TimeUnit.SECONDS.toNanos(2));
            try { candidates.consume(List.of(1,2,3),128,value->{consumed.add(value);return value!=2;}); }
            catch (FinderBudget.Yield yielded) { check(consumed.size()==slice+1,"Consumption yields before another candidate's side effects"); }
            finally { budget.close(); }
        }
        check(consumed.equals(List.of(1,2,3)) && candidates.consume(List.of(1,2,3),128,value->{throw new AssertionError("Reprocessed candidate");}).equals(List.of(1,3)),"Sliced consumption completes once without duplicate observations");
        for(int fail=0;fail<4;fail++) {
            var released=new ArrayList<Integer>();
            try(var allocation=new AllocationScope()) {
                for(int phase=0;phase<4;phase++) { if(phase==fail){throw new IllegalStateException("Injected phase failure");}allocation.own(phase,released::add); }
            } catch(IllegalStateException expected) { /* rollback under constructor failure */ }
            var expected=new ArrayList<Integer>();for(int phase=fail-1;phase>=0;phase--){expected.add(phase);}
            check(released.equals(expected),"Partial owner releases every prior allocation in reverse order: phase "+fail);
        }
    }
}
