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
    private final ArrayDeque<Task> pending=new ArrayDeque<>();
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
            if (pending.size()>=MAX_PENDING) { dropped++; continue; }
            scheduled.add(task); pending.add(task);
        }
    }
    public void unload(long chunk) {
        pending.removeIf(task -> { if (task.chunk()==chunk) { scheduled.remove(task); return true; } return false; });
        outlines.removeIf(outline -> outline.owner()==chunk);
    }
    public void tick() {
        if (level==null || pending.isEmpty()) { return; }
        Task task=pending.remove(); scheduled.remove(task);
        ChunkPos pos=new ChunkPos(ChunkPos.getX(task.chunk()),ChunkPos.getZ(task.chunk()));
        if (!task.type().enabled.get() || !level.hasChunk(pos.x(), pos.z())) { return; }
        outlines.removeIf(outline -> outline.owner()==task.chunk() && outline.type()==task.type());
        long start=System.nanoTime();
        try {
            for (Finder finder : task.type().finderBuilder.build(level,pos)) {
                ChunkPos at=finder.getChunkPos();
                if (!level.hasChunk(at.x(),at.z()) || !finder.isValidDimension(level.dimensionType())) { continue; }
                finder.findInChunk();
                for (Cuboid cuboid : finder.outlines()) {
                    if (outlines.size()<MAX_OUTLINES) { outlines.add(new Outline(task.chunk(),task.type(),cuboid)); }
                }
            }
        } finally { scans++; nanos+=System.nanoTime()-start; }
    }
    public List<Outline> outlines() { return List.copyOf(outlines); }
    public int pending() { return pending.size(); }
    public long dropped() { return dropped; }
    public String diagnostics() { return scans+" scans; finder CPU total "+nanos/1_000_000+" ms; dropped "+dropped; }
    public void clear() { pending.clear(); scheduled.clear(); outlines.clear(); level=null; }
}
