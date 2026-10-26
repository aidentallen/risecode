package dev.riserecode.render;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.UpdateEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
 * Scans, nearest-result construction and containers share a 1 ms tick deadline with explicit work caps.
 * Match storage costs at most 512 bytes of bits per selected section; no chunks are requested or generated. */
public final class LoadedBlockIndex implements AutoCloseable {
    private record Scan(LevelChunk chunk, int section, int offset) { }
    private final Minecraft mc;
    private final EventBus.Subscription subscription;
    private ClientLevel level;
    private final Map<Long, LevelChunk> chunks = new HashMap<>();
    private final SectionBlockIndex matches = new SectionBlockIndex();
    private SectionBlockIndex.Search search;
    private List<SectionBlockIndex.Position> searchResult;
    private ArrayList<BlockPos> resultPositions;
    private int resultCursor;
    private long topology, storageTopology = -1;
    private java.util.Iterator<LevelChunk> storageChunks;
    private java.util.Iterator<BlockEntity> storageEntities;
    private ArrayList<BlockEntity> storage;
    private final ArrayDeque<Scan> pending = new ArrayDeque<>();
    private Set<String> selected = Set.of();
    private List<String> selection=List.of();
    private Set<net.minecraft.world.level.block.Block> selectedBlocks = Set.of();
    private int snapshotLimit=1024;
    public void maximum(int value){int next=Math.clamp(value,1024,4096);if(next!=snapshotLimit){snapshotLimit=next;dirty=true;}}
    private List<BlockPos> blocks = List.of();
    private List<BlockEntity> containers = List.of();
    private long scannedSections, rejectedSections;
    private boolean containersRequested, dirty = true;
    private BlockPos lastPlayerPos = BlockPos.ZERO;
    public void containersRequested(boolean value) { if(containersRequested==value){return;} containersRequested = value;
        storageChunks=null;storageEntities=null;storage=null;
        if (!value) { containers = List.of(); } }
    public LoadedBlockIndex(EventBus bus, Minecraft mc) {
        this.mc = mc; subscription = bus.subscribe(UpdateEvent.class, 50, false, ignored -> update());
    }
    public void loaded(LevelChunk chunk) {
        if (!(chunk.getLevel() instanceof ClientLevel client)) { return; }
        if (level != client) { clear(); level = client; }
        dirty = true;
        long key = chunk.getPos().pack(); LevelChunk previous=chunks.put(key, chunk); topology++;
        if(previous!=null){matches.removeChunk(previous.getPos().x(),previous.getPos().z(),previous.getMinSectionY(),previous.getSections().length);}
        pending.removeIf(scan -> scan.chunk().getPos().pack() == key);
        if (!selected.isEmpty()) { pending.add(new Scan(chunk, 0, 0)); }
    }
    public void unloaded(long key) {
        dirty=true;topology++;LevelChunk chunk=chunks.remove(key);
        if(chunk!=null){matches.removeChunk(chunk.getPos().x(),chunk.getPos().z(),chunk.getMinSectionY(),chunk.getSections().length);}
        blocks=blocks.stream().filter(pos->ChunkMotionTracker.key(pos.getX()>>4,pos.getZ()>>4)!=key).toList();
        search=null;searchResult=null;resultPositions=null;
        pending.removeIf(scan -> scan.chunk().getPos().pack() == key);
    }
    public void changed(LevelChunk chunk, BlockPos pos, BlockState state) {
        if (chunk.getLevel() != level) { return; }
        if (!selected.isEmpty() && chunks.get(chunk.getPos().pack())==chunk) {
            dirty=true;matches.set(pos.getX(),pos.getY(),pos.getZ(),matches(state));
        }
    }
    public void select(List<String> ids) {
        if(selection.equals(ids)){return;}selection=List.copyOf(ids);
        Set<String> next = Set.copyOf(ids); if (selected.equals(next)) { return; }
        selected = next; dirty = true;
        var resolved = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<net.minecraft.world.level.block.Block, Boolean>());
        next.forEach(id -> BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.parse(id)).ifPresent(resolved::add));
        selectedBlocks = resolved;
        matches.clear(); search=null; searchResult=null;resultPositions=null; blocks = List.of(); pending.clear();
        if (!next.isEmpty()) {
            var near = new ArrayList<>(chunks.values());
            if (mc.player != null) { near.sort(Comparator.comparingDouble(c -> mc.player.distanceToSqr(c.getPos().getMiddleBlockX(), mc.player.getY(), c.getPos().getMiddleBlockZ()))); }
            near.forEach(chunk -> pending.add(new Scan(chunk, 0, 0)));
        }
    }
    private boolean matches(BlockState state) { return selectedBlocks.contains(state.getBlock()); }
    private void update() {
        if (mc.level != level) { clear(); level = mc.level; }
        if (selected.isEmpty() && !containersRequested) { return; }
        long deadline = System.nanoTime() + 1_000_000;
        // Scanning gets only part of the tick budget; queries and containers must also advance.
        long scanDeadline=Math.min(deadline,System.nanoTime()+350_000);
        int budget=2048;
        while (budget>0 && !pending.isEmpty() && System.nanoTime()<scanDeadline) {
            Scan scan=pending.removeFirst();LevelChunk chunk=scan.chunk();
            if(chunks.get(chunk.getPos().pack())!=chunk){continue;}
            var sections=chunk.getSections();if(scan.section()>=sections.length){continue;}
            var section=sections[scan.section()];int offset=scan.offset();
            if(offset==0){scannedSections++;}
            if(offset==0&&(section.hasOnlyAir()||!section.maybeHas(this::matches))){rejectedSections++;offset=4096;budget--;}
            int baseY=chunk.getSectionYFromSectionIndex(scan.section())<<4,baseX=chunk.getPos().x()<<4,baseZ=chunk.getPos().z()<<4;
            while(offset<4096 && budget-->0 && System.nanoTime()<scanDeadline){
                int x=offset&15,y=offset>>8,z=(offset>>4)&15;
                matches.set(baseX+x,baseY+y,baseZ+z,matches(section.getBlockState(x,y,z)));offset++;dirty=true;
            }
            if(offset<4096){pending.addFirst(new Scan(chunk,scan.section(),offset));}
            else if(scan.section()+1<sections.length){pending.addLast(new Scan(chunk,scan.section()+1,0));}
        }
        BlockPos playerPos=mc.player==null?BlockPos.ZERO:mc.player.blockPosition();
        if(search==null&&(dirty||!lastPlayerPos.equals(playerPos))){
            search=matches.search(new SectionBlockIndex.Position(playerPos.getX(),playerPos.getY(),playerPos.getZ()),snapshotLimit);
            lastPlayerPos=playerPos;dirty=false;
        }
        long queryDeadline=Math.min(deadline,System.nanoTime()+350_000);
        if(search!=null && searchResult==null && search.step(2048,queryDeadline)){
            searchResult=search.result();resultPositions=new ArrayList<>(searchResult.size());resultCursor=searchResult.size()-1;
        }
        if(searchResult!=null){
            int remaining=256;
            while(resultCursor>=0 && remaining-->0 && System.nanoTime()<queryDeadline){
                var pos=searchResult.get(resultCursor--);
                if(chunks.containsKey(ChunkMotionTracker.key(pos.x()>>4,pos.z()>>4))){resultPositions.add(new BlockPos(pos.x(),pos.y(),pos.z()));}
            }
            if(resultCursor<0){blocks=java.util.Collections.unmodifiableList(resultPositions);dirty|=!search.current();search=null;searchResult=null;}
        }
        updateContainers(deadline);
    }
    private void updateContainers(long deadline) {
        if(!containersRequested){return;}
        if(storageChunks==null || storageTopology!=topology){
            storageTopology=topology;storageChunks=chunks.values().iterator();storageEntities=null;storage=new ArrayList<>();
        }
        int budget=256;
        try {
            while(budget-->0 && System.nanoTime()<deadline){
                if(storageEntities!=null && storageEntities.hasNext()){
                    BlockEntity entity=storageEntities.next();
                    if(!entity.isRemoved()&&(entity instanceof Container||entity instanceof EnderChestBlockEntity||entity instanceof net.minecraft.world.level.block.entity.LecternBlockEntity)){storage.add(entity);}
                }else if(storageChunks.hasNext()){storageEntities=storageChunks.next().getBlockEntities().values().iterator();}
                else {containers=java.util.Collections.unmodifiableList(storage);storageChunks=null;storageEntities=null;return;}
            }
        }catch(java.util.ConcurrentModificationException changed){storageChunks=null;storageEntities=null;}
    }

    public List<BlockPos> blocks() { return blocks; }
    public List<BlockEntity> containers() { return containers; }
    public long scannedSections() { return scannedSections; }
    public long rejectedSections() { return rejectedSections; }
    public int pendingSections() { return pending.size(); }
    private void clear() { dirty = true; chunks.clear(); matches.clear(); search=null;searchResult=null;resultPositions=null;lastPlayerPos=BlockPos.ZERO;storageChunks=null;storageEntities=null;storage=null;topology++; pending.clear(); blocks = List.of(); containers = List.of(); }
    @Override public void close() { subscription.close(); clear(); }
}
