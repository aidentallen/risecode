package dev.riserecode.seedcracker;

import dev.riserecode.seedcracker.finder.Finder;
import dev.riserecode.seedcracker.finder.ReloadFinders;
import dev.riserecode.seedcracker.render.Cuboid;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;

/** Client-thread-only finder execution. Queues and rendered primitives are bounded. */
public final class CollectionQueue {
    private record Task(long chunk, Finder.Type type) { }
    public record Outline(long owner, Finder.Type type, Cuboid cuboid) { }
    private static final int MAX_PENDING=8192, MAX_OUTLINES=8192;
    private final java.util.EnumMap<Finder.Type, ArrayDeque<Task>> pending = new java.util.EnumMap<>(Finder.Type.class);
    private final java.util.EnumMap<Finder.Type, Work> active = new java.util.EnumMap<>(Finder.Type.class);
    private static final Finder.Type[] TYPES = Finder.Type.values();
    private int nextType;
    private static final class Work {
        final Task task;
        final List<Finder> finders;
        int cursor;
        Work(Task task, List<Finder> finders) { this.task = task; this.finders = finders; }
    }
    private final Set<Task> scheduled=new HashSet<>();
    private final List<Outline> outlines=new ArrayList<>();
    private ClientLevel level;
    private long dropped, scans, nanos;
    public void world(ClientLevel level) {
        clear(); this.level=level;
        if (level!=null) { ReloadFinders.reloadHeight(level.getMinY(), level.getMaxY()); }
    }
    public void chunk(long chunk) {
        if (level==null) { return; }
        boolean end=level.dimension()==net.minecraft.world.level.Level.END;
        boolean overworld=level.dimension()==net.minecraft.world.level.Level.OVERWORLD;
        for (Finder.Type type : Finder.Type.values()) {
            boolean endFinder=type==Finder.Type.END_CITY || type==Finder.Type.END_PILLARS;
            if (endFinder ? !end : !overworld) { continue; }
            Task task=new Task(type==Finder.Type.END_PILLARS ? ChunkPos.pack(0,0) : chunk,type);
            if (!type.enabled.get() || scheduled.contains(task)) { continue; }
            if (scheduled.size()>=MAX_PENDING) { dropped++; continue; }
            scheduled.add(task); pending.computeIfAbsent(type, ignored -> new ArrayDeque<>()).add(task);
        }
    }
    public void unload(long chunk) {
        for (var type : TYPES) {
            Work work = active.get(type);
            if (work != null && work.task.chunk() == chunk) { retire(type); }
            var queue = pending.get(type);
            if (queue != null) { queue.removeIf(task -> { if (task.chunk() == chunk) { scheduled.remove(task); return true; } return false; }); }
        }
        outlines.removeIf(outline -> outline.owner() == chunk);
    }
    private void retire(Finder.Type type) {
        Work work = active.remove(type);
        if (work != null) { scheduled.remove(work.task); }
    }
    /** Manual rescan prioritizes the caller's new nearest-first traversal over old load events. */
    public void restartPending() { active.clear(); pending.clear(); scheduled.clear(); nextType = 0; }
    @SuppressWarnings("try")
    public void tick() {
        if (level == null || scheduled.isEmpty()) { return; }
        Finder.Type selected = null;
        for (int i = 0; i < TYPES.length; i++) {
            Finder.Type type = TYPES[nextType]; nextType = (nextType + 1) % TYPES.length;
            var queue = pending.get(type);
            if (active.containsKey(type) || queue != null && !queue.isEmpty()) { selected = type; break; }
        }
        if (selected == null) { return; }
        long start = System.nanoTime();
        try (var budget = new FinderBudget(2048, 1_500_000)) {
            Work work = active.get(selected);
            if (work == null) {
                Task task = pending.get(selected).removeFirst();
                ChunkPos pos = new ChunkPos(ChunkPos.getX(task.chunk()), ChunkPos.getZ(task.chunk()));
                if (!selected.enabled.get() || !level.hasChunk(pos.x(), pos.z())) { scheduled.remove(task); return; }
                outlines.removeIf(outline -> outline.owner() == task.chunk() && outline.type() == task.type());
                try { work = new Work(task, selected.finderBuilder.build(level, pos)); }
                catch (RuntimeException failure) { scheduled.remove(task); throw failure; }
                active.put(selected, work);
            }
            if (!selected.enabled.get()) { retire(selected); return; }
            while (work.cursor < work.finders.size()) {
                Finder finder = work.finders.get(work.cursor);
                ChunkPos at = finder.getChunkPos();
                if (level.hasChunk(at.x(), at.z()) && finder.isValidDimension(level.dimensionType())) {
                    finder.findInChunk();
                    for (Cuboid cuboid : finder.outlines()) {
                        if (outlines.size() < MAX_OUTLINES) { outlines.add(new Outline(work.task.chunk(), selected, cuboid)); }
                    }
                }
                work.cursor++;
            }
            scans++; retire(selected);
        } catch (FinderBudget.Yield yielded) {
            // Round-robin types prevent a long shipwreck scan from starving small structure scans.
        } catch (RuntimeException failure) {
            retire(selected); throw failure;
        } finally { nanos += System.nanoTime() - start; }
    }
    public List<Outline> outlines() { return List.copyOf(outlines); }
    public int pending() { return scheduled.size(); }
    public long dropped() { return dropped; }
    public String diagnostics() { return scans+" scans; finder CPU total "+nanos/1_000_000+" ms; dropped "+dropped; }
    public void clear() { restartPending(); outlines.clear(); level=null; }
}
