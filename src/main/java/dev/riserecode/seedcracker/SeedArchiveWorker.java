package dev.riserecode.seedcracker;

import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.*;

/** Serial bounded archive IO. All payloads are immutable client-thread snapshots. */
public final class SeedArchiveWorker implements AutoCloseable {
    private final SeedStore store;
    private final java.util.concurrent.ConcurrentMap<SeedStore.Identity, Throwable> failures = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), r -> Thread.ofPlatform().daemon().name("risecode-seed-archive").unstarted(r), new ThreadPoolExecutor.AbortPolicy());
    public SeedArchiveWorker(SeedStore store) { this.store = store; }
    public CompletableFuture<Void> save(SeedStore.Saved snapshot, boolean preserve) {
        return this.<Void>submit(() -> {
            SeedStore.Saved saved = snapshot;
            if (preserve && Files.exists(store.path(snapshot.identity()))) {
                var prior = store.restore(snapshot.identity());
                var observations = new LinkedHashSet<>(prior.observations()); observations.addAll(snapshot.observations());
                var verified = new LinkedHashSet<>(prior.verifiedSeeds()); verified.addAll(snapshot.verifiedSeeds());
                saved = new SeedStore.Saved(1, snapshot.identity(), List.copyOf(observations), snapshot.pillars().isEmpty() ? prior.pillars() : snapshot.pillars(),
                        snapshot.structureSeeds().isEmpty() ? prior.structureSeeds() : snapshot.structureSeeds(), List.copyOf(verified));
            }
            store.save(saved); return null;
        }).whenComplete((ignored, failure) -> {
            if (failure == null) { failures.remove(snapshot.identity()); }
            else { failures.put(snapshot.identity(), failure); }
        });
    }
    public CompletableFuture<SeedStore.Saved> restore(SeedStore.Identity identity) { return submit(() -> store.restore(identity)); }
    private <T> CompletableFuture<T> submit(Callable<T> action) {
        var result = new CompletableFuture<T>();
        try { worker.execute(() -> { try { result.complete(action.call()); } catch (Exception failure) { result.completeExceptionally(failure); } }); }
        catch (RejectedExecutionException busy) { result.completeExceptionally(busy); }
        return result;
    }
    public boolean terminated() { return worker.isTerminated(); }
    @Override public void close() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(2, TimeUnit.SECONDS)) { throw new IllegalStateException("Seed archive flush deadline exceeded; queued writes continue in order"); }
            if (!failures.isEmpty()) {
                var failure = new IllegalStateException("One or more seed archives could not be saved; prior files retained");
                failures.values().forEach(failure::addSuppressed); throw failure;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Seed archive flush interrupted; queued writes continue in order", interrupted);
        }
    }
}
