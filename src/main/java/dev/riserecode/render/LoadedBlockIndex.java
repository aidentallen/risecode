package dev.riserecode.render;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;

/** Main-thread, loaded-only index. Palette rejection avoids scanning unrelated sections.
 * At most two sections and ~1 ms are processed per tick; no chunks are requested or generated. */
public final class LoadedBlockIndex implements AutoCloseable {
    private record Scan(LevelChunk chunk, int section) { }
    private final Minecraft mc;
    private final EventBus.Subscription subscription;
    private ClientLevel level;
    private final Map<Long, LevelChunk> chunks = new HashMap<>();
    private final Map<Long, Set<BlockPos>> matches = new HashMap<>();
    private final ArrayDeque<Scan> pending = new ArrayDeque<>();
    private Set<String> selected = Set.of();
    private Set<net.minecraft.world.level.block.Block> selectedBlocks = Set.of();
    private List<BlockPos> blocks = List.of();
    private List<BlockEntity> containers = List.of();
    private long scannedSections, rejectedSections;
    private boolean containersRequested, dirty = true;
    private BlockPos lastPlayerPos = BlockPos.ZERO;
    public void containersRequested(boolean value) { containersRequested = value; if (!value) { containers = List.of(); } }
    public LoadedBlockIndex(EventBus bus, Minecraft mc) {
        this.mc = mc; subscription = bus.subscribe(UpdateEvent.class, 50, false, ignored -> update());
    }
    public void loaded(LevelChunk chunk) {
        if (!(chunk.getLevel() instanceof ClientLevel client)) { return; }
        if (level != client) { clear(); level = client; }
        dirty = true;
        long key = chunk.getPos().pack(); chunks.put(key, chunk); matches.remove(key);
        pending.removeIf(scan -> scan.chunk().getPos().pack() == key);
        if (!selected.isEmpty()) { pending.add(new Scan(chunk, 0)); }
    }
    public void unloaded(long key) { dirty = true; chunks.remove(key); matches.remove(key); pending.removeIf(scan -> scan.chunk().getPos().pack() == key); }
    public void changed(LevelChunk chunk, BlockPos pos, BlockState state) {
        if (chunk.getLevel() != level) { return; }
        var values = matches.get(chunk.getPos().pack());
        if (values != null) { dirty = true; if (matches(state)) { values.add(pos.immutable()); } else { values.remove(pos); } }
    }
    public void select(List<String> ids) {
        Set<String> next = Set.copyOf(ids); if (selected.equals(next)) { return; }
        selected = next; dirty = true;
        var resolved = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<net.minecraft.world.level.block.Block, Boolean>());
        next.forEach(id -> BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.parse(id)).ifPresent(resolved::add));
        selectedBlocks = resolved;
        matches.clear(); blocks = List.of(); pending.clear();
        if (!next.isEmpty()) {
            var near = new ArrayList<>(chunks.values());
            if (mc.player != null) { near.sort(Comparator.comparingDouble(c -> mc.player.distanceToSqr(c.getPos().getMiddleBlockX(), mc.player.getY(), c.getPos().getMiddleBlockZ()))); }
            near.forEach(chunk -> pending.add(new Scan(chunk, 0)));
        }
    }
    private boolean matches(BlockState state) { return selectedBlocks.contains(state.getBlock()); }
    private void update() {
        if (mc.level != level) { clear(); level = mc.level; }
        if (selected.isEmpty() && !containersRequested) { return; }
        long deadline = System.nanoTime() + 1_000_000;
        for (int count = 0; count < 2 && !pending.isEmpty() && System.nanoTime() < deadline; count++) {
            Scan scan = pending.removeFirst(); LevelChunk chunk = scan.chunk();
            if (chunks.get(chunk.getPos().pack()) != chunk) { continue; }
            var sections = chunk.getSections();
            if (scan.section() >= sections.length) { continue; }
            var values = matches.computeIfAbsent(chunk.getPos().pack(), ignored -> new HashSet<>());
            var section = sections[scan.section()]; scannedSections++; dirty = true;
            if (!section.hasOnlyAir() && section.maybeHas(this::matches)) {
                int baseY = chunk.getSectionYFromSectionIndex(scan.section()) << 4;
                int baseX = chunk.getPos().x() << 4, baseZ = chunk.getPos().z() << 4;
                for (int y = 0; y < 16; y++) { for (int z = 0; z < 16; z++) { for (int x = 0; x < 16; x++) {
                    if (matches(section.getBlockState(x, y, z))) { values.add(new BlockPos(baseX + x, baseY + y, baseZ + z)); }
                } } }
            } else { rejectedSections++; }
            if (scan.section() + 1 < sections.length) { pending.addFirst(new Scan(chunk, scan.section() + 1)); }
        }
        BlockPos playerPos = mc.player == null ? BlockPos.ZERO : mc.player.blockPosition();
        if (dirty || !lastPlayerPos.equals(playerPos)) {
            // Bounded nearest-first snapshot; avoid sorting the complete ore index every frame.
            var center = net.minecraft.world.phys.Vec3.atCenterOf(playerPos);
            Comparator<BlockPos> order = Comparator.comparingDouble(pos -> pos.distToCenterSqr(center));
            var nearest = new java.util.PriorityQueue<BlockPos>(1024, order.reversed());
            for (var values : matches.values()) { for (BlockPos pos : values) {
                if (nearest.size() < 1024) { nearest.add(pos); }
                else if (order.compare(pos, nearest.peek()) < 0) { nearest.remove(); nearest.add(pos); }
            } }
            var found = new ArrayList<>(nearest); found.sort(order); blocks = List.copyOf(found);
            dirty = false; lastPlayerPos = playerPos;
        }
        if (!containersRequested) { return; }
        var storage = new ArrayList<BlockEntity>();
        for (LevelChunk chunk : chunks.values()) { for (BlockEntity entity : chunk.getBlockEntities().values()) {
            if (!entity.isRemoved() && (entity instanceof Container || entity instanceof EnderChestBlockEntity || entity instanceof net.minecraft.world.level.block.entity.LecternBlockEntity)) { storage.add(entity); }
        } }
        containers = List.copyOf(storage);
    }
    public List<BlockPos> blocks() { return blocks; }
    public List<BlockEntity> containers() { return containers; }
    public long scannedSections() { return scannedSections; }
    public long rejectedSections() { return rejectedSections; }
    public int pendingSections() { return pending.size(); }
    private void clear() { dirty = true; chunks.clear(); matches.clear(); pending.clear(); blocks = List.of(); containers = List.of(); }
    @Override public void close() { subscription.close(); clear(); }
}
