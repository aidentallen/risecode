package dev.riserecode.scanner;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import static dev.riserecode.scanner.ChunkHeuristics.*;

/** One bounded worker; no live levels/chunks/settings or render objects cross into it. */
public final class BaseScanner implements AutoCloseable {
    public record Discovery(long chunk,int minY,int height,Finding finding,long discoveredNanos) { }
    private static final class Ticket { volatile boolean active=true; boolean full; }
    private static final class State {
        final Ticket ticket;final int minY;final int[] cells;final byte[][] block,sky;
        State(Ticket ticket,int minY,int sections){this.ticket=ticket;this.minY=minY;cells=new int[sections*4096];block=new byte[sections+2][];sky=new byte[sections+2][];}
    }
    private final ScannerBlocks blocks;
    private final ThreadPoolExecutor worker;
    // Tickets and discoveries are protected by this; decoded states belong exclusively to worker.
    private final LinkedHashMap<Long,Ticket> tickets=new LinkedHashMap<>(256,.75f,true);
    private final LinkedHashMap<Long,Discovery> discoveries=new LinkedHashMap<>(256,.75f,true);
    private final LinkedHashMap<Long,State> states=new LinkedHashMap<>(32,.75f,true);
    private final AtomicLong rejected=new AtomicLong(), failures=new AtomicLong(), completed=new AtomicLong();
    private boolean closed;
    public BaseScanner(ScannerBlocks blocks){
        this.blocks=blocks;
        worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),r->{Thread t=new Thread(r,"risecode-base-scanner");t.setDaemon(true);t.setPriority(Thread.NORM_PRIORITY-1);return t;},new ThreadPoolExecutor.AbortPolicy());
    }
    public synchronized boolean hasBase(long key){Ticket t=tickets.get(key);return t!=null&&t.active&&t.full;}
    public synchronized void forget(long key){Ticket t=tickets.remove(key);if(t!=null)t.active=false;discoveries.remove(key);}
    public synchronized List<Discovery> discoveries(){return List.copyOf(discoveries.values());}
    public long rejected(){return rejected.get();} public long failures(){return failures.get();} public long completed(){return completed.get();}
    public int queued(){return worker.getQueue().size();}
    private synchronized Ticket ticket(long key,boolean full){
        if(closed)return null;
        Ticket t=tickets.computeIfAbsent(key,k->new Ticket());t.full|=full;
        while(tickets.size()>256){long first=tickets.keySet().iterator().next();forget(first);}return t;
    }
    private final class Work implements Runnable {
        final long key; final Ticket ticket; final Runnable task;
        Work(long key,Ticket ticket,Runnable task){this.key=key;this.ticket=ticket;this.task=task;}
        @Override public void run(){
            if(!ticket.active)return;
            try {states.entrySet().removeIf(e->!e.getValue().ticket.active);task.run();completed.incrementAndGet();}
            catch(RuntimeException e){failures.incrementAndGet();invalidate(key,ticket);}
        }
    }
    public synchronized int tracked(){return tickets.size();}
    private void submit(long key,Ticket t,Runnable task){
        if(t==null)return;
        try {worker.execute(new Work(key,t,task));}
        catch(RejectedExecutionException e){rejected.incrementAndGet();invalidate(key,t);}
    }
    private synchronized void invalidate(long key,Ticket t){if(tickets.get(key)==t)forget(key);}
    public void packet(int x,int z,Object packet,int minY,int sections,PalettedContainerFactory factory,Options options){
        long key=ChunkPos.pack(x,z);
        if(packet instanceof ClientboundForgetLevelChunkPacket){forget(key);return;}
        boolean full=packet instanceof ClientboundLevelChunkWithLightPacket;
        Ticket t=ticket(key,full);
        submit(key,t,()->{
            State s=states.get(key);
            if(full){
                var p=(ClientboundLevelChunkWithLightPacket)packet;s=new State(t,minY,sections);
                var buffer=p.getChunkData().getReadBuffer();
                try {for(int section=0;section<sections;section++){var decoded=new LevelChunkSection(factory);decoded.read(buffer);copy(decoded,s,section);}}
                finally{buffer.release();}
                final State snapshot=s;
                p.getChunkData().getBlockEntitiesTagsConsumer(x,z).accept((pos,type,tag)->{int i=cell(pos,snapshot);if(i>=0)snapshot.cells[i]|=ENTITY;});
                light(s,p.getLightData());states.put(key,s);trimStates();
            }else{
                if(s==null||s.ticket!=t)return;
                if(packet instanceof ClientboundLightUpdatePacket p)light(s,p.getLightData());
                else if(packet instanceof ClientboundBlockUpdatePacket p)update(s,p.getPos(),p.getBlockState());
                else if(packet instanceof ClientboundSectionBlocksUpdatePacket p){State target=s;p.runUpdates((pos,state)->update(target,pos,state));}
            }
            evaluate(key,t,s,options);
        });
    }
    /** Copies are captured by the client thread; the worker never reads a resident section. */
    public void snapshot(int x,int z,int minY,LevelChunkSection[] copies,ClientboundLightUpdatePacketData light,Options options){
        long key=ChunkPos.pack(x,z);Ticket t=ticket(key,true);
        submit(key,t,()->{var s=new State(t,minY,copies.length);for(int section=0;section<copies.length;section++)copy(copies[section],s,section);light(s,light);states.put(key,s);trimStates();evaluate(key,t,s,options);});
    }
    private void trimStates(){while(states.size()>32){var first=states.entrySet().iterator().next();states.remove(first.getKey());/* Keep immutable discoveries; a later delta needs a fresh snapshot. */ synchronized(this){if(tickets.get(first.getKey())==first.getValue().ticket)first.getValue().ticket.full=false;}}}
    private void copy(LevelChunkSection section,State s,int sectionIndex){
        for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)s.cells[index(x,(sectionIndex<<4)+y,z)]=blocks.flags(section.getBlockState(x,y,z));
    }
    private static int cell(BlockPos p,State s){int y=p.getY()-s.minY;return y>=0&&y<s.cells.length/256?index(p.getX()&15,y,p.getZ()&15):-1;}
    private void update(State s,BlockPos p,net.minecraft.world.level.block.state.BlockState state){int i=cell(p,s);if(i>=0)s.cells[i]=blocks.flags(state);}
    private static void light(State s,ClientboundLightUpdatePacketData p){
        merge(s.block,p.getBlockYMask(),p.getEmptyBlockYMask(),p.getBlockUpdates());merge(s.sky,p.getSkyYMask(),p.getEmptySkyYMask(),p.getSkyUpdates());
    }
    private static void merge(byte[][] target,BitSet mask,BitSet empty,List<byte[]> updates){
        for(int i=empty.nextSetBit(0);i>=0&&i<target.length;i=empty.nextSetBit(i+1))target[i]=new byte[2048];
        int cursor=0;for(int i=mask.nextSetBit(0);i>=0;i=mask.nextSetBit(i+1)){byte[] data=updates.get(cursor++);if(i<target.length){if(data.length!=2048)throw new IllegalArgumentException("Invalid light layer");target[i]=data.clone();}}
    }
    private void evaluate(long key,Ticket t,State s,Options options){
        // Collapse consecutive updates for one chunk into one analysis, preserving packet order.
        if(worker.getQueue().peek() instanceof Work next&&next.key==key&&next.ticket==t)return;
        // Remove derived growth before recomputing after delta packets.
        for(int i=0;i<s.cells.length;i++){if((s.cells[i]&(DERIVED_GROWTH))!=0)s.cells[i]&=~(GROWTH|(DERIVED_GROWTH));s.cells[i]&=~(CLOCK);}
        ScannerBlocks.growth(s.cells);
        Finding f=ChunkHeuristics.evaluate(s.cells,s.minY,s.block,s.sky,options);
        synchronized(this){
            if(!t.active||tickets.get(key)!=t)return;
            if(f==null){discoveries.remove(key);return;}
            Discovery previous=discoveries.get(key);
            discoveries.put(key,new Discovery(key,s.minY,s.cells.length/256,f,previous==null?System.nanoTime():previous.discoveredNanos()));
            while(discoveries.size()>256)discoveries.remove(discoveries.keySet().iterator().next());
        }
    }
    @Override public synchronized void close(){closed=true;tickets.values().forEach(t->t.active=false);tickets.clear();discoveries.clear();worker.shutdownNow();}
}
