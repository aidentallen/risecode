package dev.riserecode.seedcracker;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.rand.seed.PillarSeed;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.structure.OldStructure;
import com.seedfinding.mcfeature.structure.PillagerOutpost;
import com.seedfinding.mcfeature.structure.Shipwreck;
import com.seedfinding.mcfeature.structure.TriangularStructure;
import com.seedfinding.mcfeature.structure.UniformStructure;
import com.seedfinding.mcseed.lcg.LCG;
import dev.riserecode.seedcracker.cracker.PillarData;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Modern structure lifting and End-pillar reversal adapted from SeedCrackerX
 * TimeMachine (MIT, b72a829f768967fa737a4f1f63b32e37b9619152).
 * All inputs are immutable. Workers never reference a Minecraft world or player.
 */
public final class CrackingJob implements AutoCloseable {
    private static final LCG INVERSE_PILLAR_LCG=LCG.JAVA.combine(-2);
    public static final int MAX_CANDIDATES = 250_000;
    public record Snapshot(List<Observation> observations, List<Integer> pillars, long hash,
            List<Long> priorStructures) {
        public Snapshot { observations = List.copyOf(observations); pillars = List.copyOf(pillars); priorStructures = List.copyOf(priorStructures); }
    }
    public record Progress(String phase, long tested, int structureCandidates, int worldCandidates,
            List<Long> structures, List<Long> worlds, boolean complete, boolean cancelled, String message) {
        public Progress { structures = List.copyOf(structures); worlds = List.copyOf(worlds); }
    }
    private final Snapshot snapshot;
    private final int workers;
    private final ThreadPoolExecutor pool;
    private final Set<Long> structures = ConcurrentHashMap.newKeySet(), worlds = ConcurrentHashMap.newKeySet();
    private final AtomicLong tested = new AtomicLong();
    private volatile boolean cancelled, done;
    private volatile Progress finalProgress;
    private volatile String phase = "Starting", message = "";
    private final Thread coordinator;

    public static int defaultWorkers() { return Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors()-1)); }
    public CrackingJob(Snapshot snapshot, int workers) {
        if (workers < 1 || workers > 16) { throw new IllegalArgumentException("Use 1–16 cracking workers"); }
        this.snapshot = snapshot; this.workers = workers;
        pool = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(16),
                runnable -> Thread.ofPlatform().daemon().name("risecode-seed-worker").unstarted(runnable), new ThreadPoolExecutor.AbortPolicy());
        coordinator = Thread.ofPlatform().daemon().name("risecode-seed-attempt").unstarted(this::run);
    }
    public void start() { coordinator.start(); }
    private boolean stopped() { return cancelled || Thread.currentThread().isInterrupted(); }
    private void check() { if (stopped()) { throw new CancellationException(); } }
    private void parallel(java.util.function.IntConsumer work) {
        var tasks = new ArrayList<CompletableFuture<Void>>();
        for (int i=0; i<workers; i++) {
            final int part = i;
            tasks.add(CompletableFuture.runAsync(() -> work.accept(part), pool));
        }
        CompletableFuture.allOf(tasks.toArray(CompletableFuture<?>[]::new)).join();
        check();
    }
    private void run() {
        try {
            List<Feature.Data<?>> data = snapshot.observations().stream().map(Observation::data).toList();
            List<Feature.Data<?>> regular = data.stream().filter(d -> !(d.feature instanceof PillagerOutpost)).toList();
            var liftable = data.stream().filter(d -> d.feature instanceof OldStructure || d.feature instanceof Shipwreck)
                    .map(d -> (UniformStructure.Data<?>)d).toList();
            var pillars = new ArrayList<Integer>();
            if (!snapshot.pillars().isEmpty()) {
                phase = "End pillar reversal";
                var pillarData = new PillarData(snapshot.pillars());
                for (int seed=0; seed<(1<<16); seed++) { check(); if (pillarData.test(seed)) { pillars.add(seed); } }
                if (pillars.isEmpty()) { throw new IllegalArgumentException("End pillar heights do not match a native pillar layout; clear and rescan"); }
            }
            if (!snapshot.priorStructures().isEmpty()) {
                phase = "Reducing structure candidates";
                ChunkRand rand = new ChunkRand();
                for (long seed : snapshot.priorStructures()) {
                    check(); if (matches(seed, regular, rand) && (pillars.isEmpty() || pillars.contains((int)PillarSeed.fromStructureSeed(seed)))) { addStructure(seed); }
                }
            } else if (!pillars.isEmpty() && regularBits(snapshot.observations()) >= 32) {
                phase = "End structure search";
                parallel(part -> {
                    ChunkRand rand = new ChunkRand();
                    long count = 0;
                    for (int pillar : pillars) {
                        for (long partial=part; partial<(1L<<32); partial+=workers) {
                            check();
                            long seed = reversePillar(partial, pillar);
                            if (matches(seed, regular, rand)) { addStructure(seed); }
                            if ((++count & 65535)==0) { tested.addAndGet(65536); }
                        }
                    }
                });
            } else if (liftingBits(snapshot.observations()) >= 40 && regularBits(snapshot.observations()) >= 32) {
                phase = "Lifting lower 19 bits";
                List<Long> lower = new ArrayList<>(); ChunkRand rand = new ChunkRand();
                for (long seed=0; seed<(1L<<19); seed++) {
                    check(); boolean match = true;
                    for (UniformStructure.Data<?> datum : liftable) {
                        rand.setRegionSeed(seed, datum.regionX, datum.regionZ, datum.feature.getSalt(), datum.feature.getVersion());
                        int offset = ((UniformStructure<?>)datum.feature).getOffset();
                        if (rand.nextInt(offset)%4 != datum.offsetX%4 || rand.nextInt(offset)%4 != datum.offsetZ%4) { match = false; break; }
                    }
                    if (match) { lower.add(seed); }
                }
                phase = "Lifting upper 29 bits";
                parallel(part -> {
                    ChunkRand workerRand = new ChunkRand(); long count=0;
                    for (long low : lower) {
                        for (long upper=part; upper<(1L<<29); upper+=workers) {
                            check(); long seed = (upper<<19)|low;
                            if (matches(seed, regular, workerRand)) { addStructure(seed); }
                            if ((++count & 65535)==0) { tested.addAndGet(65536); }
                        }
                    }
                });
            } else {
                message = "Insufficient data: need 40 lifting + 32 regular bits, or End pillars + 32 regular bits. Collect more, then Start.";
                return;
            }
            check();
            if (structures.isEmpty()) { message = "No matching structure seeds. Collect more structures, check generation settings, or clear incorrect data."; return; }
            if (snapshot.hash()==0) { message = "Structure seeds only: server supplied no usable seed hash. World seeds remain unverified."; return; }
            if (structures.size()>1000) { message = "Too many structure candidates for hash search. Collect more structures, then Start."; return; }
            phase = "Verifying world seeds against hash";
            List<Long> seeds = List.copyOf(structures);
            parallel(part -> {
                long count=0;
                for (long structure : seeds) {
                    for (long upper=part; upper<(1L<<16); upper+=workers) {
                        check(); long seed=(upper<<48)|structure;
                        if (WorldSeed.toHash(seed)==snapshot.hash()) { worlds.add(seed); }
                        if ((++count & 1023)==0) { tested.addAndGet(1024); }
                    }
                }
            });
            message = worlds.isEmpty() ? "No hash match. Observations may be incorrect or belong to a world with custom generation." :
                    worlds.size()==1 ? "World seed verified against the server seed hash." : "Multiple hash-verified world seeds; collect more data.";
        } catch (CancellationException failure) { cancelled=true; message="Stopped; collection continues. Start a new attempt manually.";
        } catch (RuntimeException failure) {
            Throwable cause=failure; while (cause.getCause()!=null) { cause=cause.getCause(); }
            if (cause instanceof CancellationException || cancelled) { cancelled=true; message="Stopped; collection continues."; }
            else { message="Cracking failed: "+cause.getMessage(); }
        } finally {
            pool.shutdownNow();
            phase=cancelled ? "Stopped" : "Finished"; done=true; finalProgress=snapshotProgress();
        }
    }
    private void addStructure(long seed) {
        if (structures.size()>=MAX_CANDIDATES) { throw new IllegalArgumentException("Candidate limit reached; collect more independent structures"); }
        structures.add(seed);
    }
    private static boolean matches(long seed, List<Feature.Data<?>> data, ChunkRand rand) {
        for (Feature.Data<?> datum : data) { if (!datum.testStart(seed, rand)) { return false; } }
        return true;
    }
    public static long reversePillar(long partial, int pillar) {
        long state=((partial & 0xFFFF0000L)<<16)|((long)pillar<<16)|(partial & 0xFFFFL);
        return INVERSE_PILLAR_LCG.nextSeed(state)^LCG.JAVA.multiplier;
    }
    public static double regularBits(List<Observation> data) {
        return data.stream().map(Observation::data).filter(d -> !(d.feature instanceof PillagerOutpost)).mapToDouble(d -> {
            if (d.feature instanceof UniformStructure<?> f) { return bits(f.getOffset()); }
            if (d.feature instanceof TriangularStructure<?> f) { return bits(f.getPeak()); }
            return Math.log(100)/Math.log(2); // buried treasure
        }).sum();
    }
    public static double liftingBits(List<Observation> data) {
        return data.stream().map(Observation::data).filter(d -> d.feature instanceof OldStructure || d.feature instanceof Shipwreck)
                .mapToDouble(d -> bits(((UniformStructure<?>)d.feature).getOffset())).sum();
    }
    private static double bits(int offset) { return Math.log((double)offset*offset)/Math.log(2); }
    public Progress progress() { var terminal=finalProgress; return terminal==null ? snapshotProgress() : terminal; }
    private Progress snapshotProgress() {
        boolean finished=done;
        return new Progress(phase,tested.get(),structures.size(),worlds.size(),
                finished && !cancelled && !message.startsWith("Cracking failed") ? structures.stream().sorted().toList() : List.of(),
                finished && !cancelled && !message.startsWith("Cracking failed") ? worlds.stream().sorted().toList() : List.of(),finished,cancelled,message);
    }
    public boolean terminated() { return done && pool.isTerminated(); }
    @Override public void close() { cancelled=true; pool.shutdownNow(); coordinator.interrupt(); }
}
