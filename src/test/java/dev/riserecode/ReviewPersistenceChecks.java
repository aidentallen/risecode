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
            candidateAndAllocation(); recovery();
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
