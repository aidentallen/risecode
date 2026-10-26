package dev.riserecode.map;

import com.google.gson.JsonObject;
import dev.riserecode.Client;
import dev.riserecode.config.JsonStorage;
import dev.riserecode.event.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

/** Client-only surface recorder. 1.5 ms/32 columns per tick; 16 pending jobs, 512 CPU tiles, 512 GPU tiles. */
public final class MapService implements AutoCloseable {
    public record Key(String dimension,int x,int z,int level,MapLayerPolicy.Layer layer,int altitude) {
        public Key(String dimension,int x,int z){this(dimension,x,z,0);}
        public Key(String dimension,int x,int z,int level){this(dimension,x,z,level,MapLayerPolicy.Layer.SURFACE,0);}
        public Key { Objects.requireNonNull(dimension);Objects.requireNonNull(layer);if(level<0||level>8||level%2!=0||altitude < -2048||altitude>2048){throw new IllegalArgumentException("Invalid tile key");}if(layer==MapLayerPolicy.Layer.SURFACE){altitude=0;} }
        public Key parent(){return new Key(dimension,Math.floorDiv(x,4),Math.floorDiv(z,4),level+2,layer,altitude);}
    }
    private record Completion(long generation,Runnable apply) { }
    public record Statistics(int cpuTiles,int pending,int queued,int gpuTiles,long captured,long stale) { }
    private final Minecraft mc;
    private final Path root;
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),r->{Thread t=new Thread(r,"risecode surface IO");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private final ArrayBlockingQueue<Completion> completed=new ArrayBlockingQueue<>(32);
    private final Set<Key> pending=new HashSet<>();
    private final LinkedHashMap<Key,SurfaceTile> tiles=new LinkedHashMap<>(512,0.75f,true);
    private final LinkedHashMap<Key,Long> scanned=new LinkedHashMap<>(512,0.75f,true);
    private final LinkedHashSet<Key> missing=new LinkedHashSet<>();
    private final MapColors colors=new MapColors();
    private final MapTextures textures=new MapTextures(this);
    private final EventBus.Subscription updates, terrainChanges;
    private final LinkedHashSet<Key> dirty=new LinkedHashSet<>();
    private WaypointStore waypoints=new WaypointStore();
    private ClientLevel level;
    private Path worldDirectory;
    private String world="",dimension="minecraft:overworld";
    private boolean recording=true,waypointsReady,wasDead;
    private JsonObject preferences=new JsonObject();
    public JsonObject preferences(){return preferences.deepCopy();}
    public void preferences(JsonObject value){preferences=value.deepCopy();saveSettings();}
    private void saveSettings(){JsonObject json=preferences.deepCopy();json.addProperty("recording",recording);submit(()->write(root.resolve("settings.json"),json));}
    private long generation,captured,stale,savedRevision=-1,tick;
    private final java.util.concurrent.atomic.AtomicBoolean savingWaypoints=new java.util.concurrent.atomic.AtomicBoolean();
    private Capture capture;
    private int scanCursor;
    private long cpuBytes;
    public static final long CPU_LIMIT=64L*1024*1024;
    private final MapLayerPolicy layers=new MapLayerPolicy();
    private MapLayerPolicy.Mode mode=MapLayerPolicy.Mode.AUTO;
    private final LinkedHashSet<Key> visible=new LinkedHashSet<>();
    public MapLayerPolicy.Mode mode(){return mode;}
    public void cycleMode(){mode=MapLayerPolicy.Mode.values()[(mode.ordinal()+1)%MapLayerPolicy.Mode.values().length];preferences.addProperty("layerMode",mode.name());saveSettings();}
    public MapLayerPolicy.Layer layer(MapLayerPolicy.Mode selection){return layers.layer(selection);}
    public int altitude(){return layers.altitude();}
    public long cpuBytes(){return cpuBytes;}
    public Key key(String dim,int x,int z,int lod,MapLayerPolicy.Mode selection,int manualAltitude){var layer=layers.layer(selection);return new Key(dim,x,z,lod,layer,layer==MapLayerPolicy.Layer.CAVE?(manualAltitude==Integer.MIN_VALUE?altitude():manualAltitude):0);}
    private final ArrayDeque<PendingDeath> bufferedDeaths=new ArrayDeque<>();
    private record PendingDeath(int x,int y,int z,String dimension,long time) { }
    private boolean enclosed(){
        int roofs=0;var base=mc.player.blockPosition();
        for(int[] off:new int[][]{{0,0},{3,0},{-3,0},{0,3},{0,-3}}){
            BlockPos column=base.offset(off[0],2,off[1]);boolean roof=false;
            for(int dy=0;dy<48&&column.getY()+dy<level.getMaxY();dy++){
                var state=level.getBlockState(column.above(dy));
                if(!state.isAir()&&!state.is(net.minecraft.tags.BlockTags.LEAVES)&&!state.is(net.minecraft.tags.BlockTags.LOGS)&&state.isSolidRender()) {roof=true;break;}
            }
            if(roof&&!level.canSeeSky(column)){roofs++;}
        }return roofs>=4;
    }
    private static final List<int[]> OFFSETS=offsets();
    private static List<int[]> offsets(){List<int[]> out=new ArrayList<>();for(int z=-8;z<=8;z++){for(int x=-8;x<=8;x++){out.add(new int[]{x,z});}}out.sort(Comparator.comparingInt(a->a[0]*a[0]+a[1]*a[1]));return List.copyOf(out);}
    public MapService(Path directory,EventBus bus,Minecraft mc){
        this.mc=mc;root=directory.resolve("maps");
        try{Path settings=root.resolve("settings.json");if(Files.isRegularFile(settings)){preferences=JsonStorage.read(settings);if(preferences.has("recording")){recording=preferences.get("recording").getAsBoolean();}if(preferences.has("layerMode")){mode=MapLayerPolicy.Mode.valueOf(preferences.get("layerMode").getAsString());}}}catch(Exception e){Client.INSTANCE.getLogger().warn("Map settings could not be loaded",e);}
        updates=bus.subscribe(UpdateEvent.class,this::update);
        terrainChanges=bus.subscribe(TerrainChangedEvent.class,e->{if(recording&&e.level()==level){var key=new Key(dimension,e.position().getX()>>4,e.position().getZ()>>4);scanned.remove(key);if(dirty.size()<512){dirty.add(key);}}});
    }
    public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public boolean connected(){return level!=null;}
    public boolean ready(){return connected()&&waypointsReady;}
    public boolean recording(){return recording;}
    public void recording(boolean value){recording=value;if(!value){capture=null;}saveSettings();}
    public WaypointStore waypoints(){return waypoints;}
    public String dimension(){return dimension;}
    public List<String> dimensions(){LinkedHashSet<String> out=new LinkedHashSet<>(List.of(dimension,"minecraft:overworld","minecraft:the_nether","minecraft:the_end"));for(var p:waypoints.all()){out.add(p.dimension());}for(var key:tiles.keySet()){out.add(key.dimension());}return List.copyOf(out);}
    public Statistics statistics(){return new Statistics(tiles.size(),pending.size(),worker.getQueue().size(),textures.size(),captured,stale);}
    public MapTextures textures(){return textures;}
    public Minecraft minecraft(){return mc;}
    private String identity(){
        if(mc.getSingleplayerServer()!=null){return "local:"+mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();}
        if(mc.getCurrentServer()!=null){return "server:"+mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);}
        return mc.getConnection()==null?"":("server:"+mc.getConnection().getConnection().getRemoteAddress());
    }
    private void update(UpdateEvent event){
        tick++;ClientLevel current=mc.level;String nextWorld=current==null?"":identity();
        if(current!=level){
            boolean sameWorld=nextWorld.equals(world)&&!nextWorld.isEmpty();
            if(!sameWorld){flushWaypointsSnapshot();bufferedDeaths.clear();waypoints=new WaypointStore();waypointsReady=false;savedRevision=-1;}
            generation++;capture=null;dirty.clear();visible.clear();cpuBytes=0;layers.reset();pending.clear();missing.clear();tiles.clear();scanned.clear();textures.clear();colors.clear();scanCursor=0;level=current;world=nextWorld;if(!sameWorld){wasDead=false;}
            if(level!=null){worldDirectory=root.resolve(hash(world));dimension=level.dimension().identifier().toString();if(!sameWorld){loadWaypoints();}}
        }
        var policy=dev.riserecode.performance.PerformancePolicy.current();
        long completionDeadline=System.nanoTime()+(policy.backgroundWork()?250_000:Long.MAX_VALUE/2);int completions=0;
        Completion completion;while((!policy.backgroundWork()||completions<8&&System.nanoTime()<completionDeadline)&&(completion=completed.poll())!=null){completions++;if(completion.generation()==generation){completion.apply().run();}else{stale++;}}
        trimCpu();
        if(level==null||mc.player==null){return;}
        if(!waypointsReady&&tick%40==0){loadWaypoints();}
        boolean dead=!mc.player.isAlive();
        if(tick%4==0&&!dead){layers.update(enclosed(),mc.player.blockPosition().getY());}
        if(dead&&!wasDead){var p=mc.player.blockPosition();bufferedDeaths.addLast(new PendingDeath(p.getX(),p.getY(),p.getZ(),dimension,System.currentTimeMillis()));}
        wasDead=dead;
        if(ready()){while(!bufferedDeaths.isEmpty()){var death=bufferedDeaths.removeFirst();waypoints.death(death.x(),death.y(),death.z(),death.dimension(),death.time());}}
        if(!dead&&ready()){waypoints.reached(mc.player.getX(),mc.player.getY(),mc.player.getZ(),dimension);}
        if(tick%20==0){saveWaypoints();}
        if(recording&&pending.size()<16&&worker.getQueue().remainingCapacity()>2){record();}
    }
    private boolean submit(Runnable job){try{worker.execute(job);return true;}catch(RejectedExecutionException e){return false;}}
    private void complete(long stamp,Runnable action){
        // Back-pressure belongs on the IO worker; never drop write acknowledgements or strand pending keys.
        try{completed.put(new Completion(stamp,action));}catch(InterruptedException stopped){Thread.currentThread().interrupt();}
    }
    private void loadWaypoints(){
        if(worldDirectory==null){return;}long stamp=generation;Path path=worldDirectory.resolve("waypoints.json");
        submit(()->{JsonObject json=new JsonObject();try{if(Files.isRegularFile(path)){if(Files.size(path)>8*1024*1024){throw new java.io.IOException("Waypoint file exceeds 8 MiB");}json=JsonStorage.read(path);}}catch(Exception e){Client.INSTANCE.getLogger().error("Cannot read waypoints; preserving file",e);return;}JsonObject loaded=json;complete(stamp,()->{if(!waypointsReady){try{waypoints.load(loaded);waypointsReady=true;savedRevision=waypoints.revision();}catch(RuntimeException e){Client.INSTANCE.getLogger().error("Invalid waypoint store; preserving file",e);}}});});
    }
    public void saveWaypoints(){
        if(!waypointsReady||worldDirectory==null||savedRevision==waypoints.revision()||!savingWaypoints.compareAndSet(false,true)){return;}
        var archives=waypoints.pendingArchives();JsonObject snapshot=waypoints.snapshot();Path path=worldDirectory.resolve("waypoints.json");long revision=waypoints.revision(),stamp=generation;
        if(!submit(()->{
            try{persistWaypoints(path,snapshot,archives);complete(stamp,()->{savedRevision=revision;waypoints.acknowledgeArchives(archives.keySet());});}
            catch(Exception e){Client.INSTANCE.getLogger().error("Cannot save waypoints; will retry",e);}
            finally{savingWaypoints.set(false);}
        })){savingWaypoints.set(false);}
    }
    private void flushWaypointsSnapshot(){
        if(worldDirectory==null){return;}
        if(!waypointsReady){
            if(bufferedDeaths.isEmpty()){return;}var deaths=List.copyOf(bufferedDeaths);Path path=worldDirectory.resolve("waypoints.json");
            submit(()->{try{WaypointStore store=new WaypointStore();if(Files.isRegularFile(path)){store.load(JsonStorage.read(path));}for(var death:deaths){store.death(death.x(),death.y(),death.z(),death.dimension(),death.time());}for(var archive:store.pendingArchives().entrySet()){JsonStorage.write(path.getParent().resolve("deaths").resolve(archive.getKey()+".json"),archive.getValue());}JsonStorage.write(path,store.snapshot());}catch(Exception error){Client.INSTANCE.getLogger().error("Cannot flush deaths recorded while loading; preserving waypoint file",error);}});return;
        }
        var archives=waypoints.pendingArchives();JsonObject snapshot=waypoints.snapshot();Path path=worldDirectory.resolve("waypoints.json");
        if(!submit(()->{try{persistWaypoints(path,snapshot,archives);}catch(java.io.IOException error){Client.INSTANCE.getLogger().error("Cannot persist waypoint snapshot; preserving prior index",error);}} )){Client.INSTANCE.getLogger().error("Waypoint IO queue full while leaving world {}",worldDirectory);}
    }
    private static void persistWaypoints(Path path,JsonObject json,Map<Integer,JsonObject> archives)throws java.io.IOException{
        for(var archive:archives.entrySet()){JsonStorage.write(path.getParent().resolve("deaths").resolve(archive.getKey()+".json"),archive.getValue());}
        JsonStorage.write(path,json);
    }
    private static void write(Path path,JsonObject json){try{JsonStorage.write(path,json);}catch(Exception e){Client.INSTANCE.getLogger().error("Cannot write map data {}",path,e);}}
    public void loadDeathPage(int page,java.util.function.Consumer<List<WaypointStore.Waypoint>> receive){
        if(!ready()||page<0||page>=waypoints.archivePages()){receive.accept(List.of());return;}
        long stamp=generation;Path path=worldDirectory.resolve("deaths").resolve(page+".json");
        submit(()->{try{WaypointStore history=new WaypointStore();if(Files.size(path)>2*1024*1024){throw new java.io.IOException("Oversized death page");}history.load(JsonStorage.read(path));complete(stamp,()->receive.accept(history.all()));}catch(Exception error){Client.INSTANCE.getLogger().warn("Cannot load death page",error);complete(stamp,()->receive.accept(List.of()));}});
    }
    public void clearDeaths(){
        if(!ready()){return;}int count=waypoints.archivePages();Path archive=worldDirectory.resolve("deaths");waypoints.clearDeaths();
        JsonObject cleared=waypoints.snapshot();Path index=worldDirectory.resolve("waypoints.json");
        submit(()->{try{persistWaypoints(index,cleared,Map.of());for(int i=0;i<count;i++){Files.deleteIfExists(archive.resolve(i+".json"));}}catch(java.io.IOException error){Client.INSTANCE.getLogger().warn("Cannot finish clearing death archive; preserving remaining files",error);}});saveWaypoints();
    }
    public WaypointStore.Waypoint addAtPlayer(){if(!ready()||mc.player==null){return null;}var p=mc.player.blockPosition();var result=waypoints.add("Waypoint "+(waypoints.all().size()+1),p.getX(),p.getY(),p.getZ(),dimension);saveWaypoints();return result;}
    private Path file(Key key){return fileAt(worldDirectory,key);}
    private static Path fileAt(Path directory,Key key){int pageX=MapPageLayout.globalPageFromBlock(key.x()*16),pageZ=MapPageLayout.globalPageFromBlock(key.z()*16);return directory.resolve("terrain").resolve(hash(key.dimension())).resolve(key.layer()==MapLayerPolicy.Layer.SURFACE?".":"cave_"+key.altitude()).resolve("lod"+key.level()).resolve(MapPageLayout.regionFromGlobalPage(pageX)+"_"+MapPageLayout.regionFromGlobalPage(pageZ)).resolve(key.x()+"_"+key.z()+".rsm");}
    public SurfaceTile tile(Key key){
        if(key.level()==0&&key.dimension().equals(dimension)&&!scanned.containsKey(key)&&visible.size()<512){visible.add(key);}
        SurfaceTile found=tiles.get(key);if(found!=null){return found;}
        if(!connected()||missing.contains(key)||pending.contains(key)||pending.size()>=16||worker.getQueue().remainingCapacity()<=2){return null;}
        long stamp=generation;Path path=file(key);pending.add(key);
        if(!submit(()->{SurfaceTile loaded=null;try{if(Files.isRegularFile(path)){loaded=SurfaceTile.read(path);if(loaded.x()!=key.x()||loaded.z()!=key.z()){throw new java.io.IOException("Wrong tile coordinates");}}}catch(Exception e){Client.INSTANCE.getLogger().warn("Cannot read surface tile {}",path,e);}SurfaceTile result=loaded;complete(stamp,()->{pending.remove(key);if(result!=null){publish(key,result);}else{missing.add(key);if(missing.size()>2048){missing.remove(missing.iterator().next());}}});})){pending.remove(key);}
        return null;
    }
    private void publish(Key key,SurfaceTile tile){SurfaceTile previous=tiles.put(key,tile);cpuBytes+=tile.bytes()-(previous==null?0:previous.bytes());textures.remove(key);missing.remove(key);trimCpu();}
    private void trimCpu(){while(tiles.size()>dev.riserecode.performance.PerformancePolicy.current().mapCpuEntries()||cpuBytes>dev.riserecode.performance.PerformancePolicy.current().mapCpuBytes()){var first=tiles.keySet().iterator().next();cpuBytes-=tiles.remove(first).bytes();textures.remove(first);}}
    public record MemoryStatistics(int cpuEntries,long decodedCpuBytes,int gpuEntries,long nativeImageBytes,long ownedGpuBytes,int gpuOverage,int completions) { }
    public MemoryStatistics memoryStatistics(){return new MemoryStatistics(tiles.size(),cpuBytes,textures.ownedSize(),textures.bytes(),textures.bytes(),textures.overage(),completed.size());}
    private void record(){
        var policy=dev.riserecode.performance.PerformancePolicy.current();long end=System.nanoTime()+policy.mapCaptureNanos();
        if(capture==null&&!visible.isEmpty()){
            for(Key key:List.copyOf(visible)){visible.remove(key);if(!pending.contains(key)&&level.getChunkSource().getChunk(key.x(),key.z(),ChunkStatus.FULL,false)!=null){capture=new Capture(key);break;}}
        }
        if(capture==null&&!dirty.isEmpty()){
            for(var key:List.copyOf(dirty)){if(pending.contains(key)){continue;}dirty.remove(key);if(level.getChunkSource().getChunk(key.x(),key.z(),ChunkStatus.FULL,false)!=null){capture=new Capture(key);break;}}
        }
        if(capture==null){
            for(int tries=0;tries<8;tries++){
                int[] off=OFFSETS.get(scanCursor++%OFFSETS.size());int cx=(mc.player.blockPosition().getX()>>4)+off[0],cz=(mc.player.blockPosition().getZ()>>4)+off[1];Key key=key(dimension,cx,cz,0,mode,Integer.MIN_VALUE);
                if(tick-scanned.getOrDefault(key,-1000L)<600||pending.contains(key)){continue;}
                if(level.getChunkSource().getChunk(cx,cz,ChunkStatus.FULL,false)==null){continue;}
                capture=new Capture(key);break;
            }
        }
        if(capture==null){return;}
        if (level.getChunkSource().getChunk(capture.key.x(),capture.key.z(),ChunkStatus.FULL,false)==null
                || Math.abs((mc.player.blockPosition().getX()>>4)-capture.key.x())>8 || Math.abs((mc.player.blockPosition().getZ()>>4)-capture.key.z())>8) { capture=null; return; }
        int count=0;while(capture.index<256&&count++<policy.mapColumns()&&System.nanoTime()<end){capture.column();}
        if(capture.index==256){
            Capture done=capture;capture=null;SurfaceTile capturedData=done.finish();long stamp=generation;Path path=file(done.key);Path worldPath=worldDirectory;pending.add(done.key);
            if(submit(()->{SurfaceTile snapshot=capturedData.shaded();try{snapshot.write(path);
                SurfaceTile child=snapshot;
                for(int lod=2;lod<=8;lod+=2){int px=Math.floorDiv(child.x(),4),pz=Math.floorDiv(child.z(),4);Key parentKey=new Key(done.key.dimension(),px,pz,lod,done.key.layer(),done.key.altitude());Path parentPath=fileAt(worldPath,parentKey);
                    SurfaceTile prior=Files.isRegularFile(parentPath)?SurfaceTile.read(parentPath):null;
                    child=SurfaceOverview.merge(prior,child,px,pz);child.write(parentPath);
                }
            }catch(Exception e){Client.INSTANCE.getLogger().warn("Cannot persist surface tile",e);}complete(stamp,()->{pending.remove(done.key);publish(done.key,snapshot);
                    int px=done.key.x(),pz=done.key.z();for(int lod=2;lod<=8;lod+=2){px=Math.floorDiv(px,4);pz=Math.floorDiv(pz,4);Key parent=new Key(done.key.dimension(),px,pz,lod,done.key.layer(),done.key.altitude());SurfaceTile removed=tiles.remove(parent);if(removed!=null){cpuBytes-=removed.bytes();}textures.remove(parent);missing.remove(parent);}});})){scanned.put(done.key,tick);while(scanned.size()>1024){scanned.remove(scanned.keySet().iterator().next());}captured++;}else{pending.remove(done.key);}
        }
    }
    private final class Capture {
        private final Key key;private int index;private final List<String> palette=new ArrayList<>();private final Map<net.minecraft.world.level.block.state.BlockState,Short> indices=new HashMap<>();
        private final short[] blocks=new short[256],heights=new short[256];private final int[] tints=new int[256],rgb=new int[256];private final byte[] water=new byte[256];private final int[] detail=new int[4096];private final short[] edges=new short[32];
        private Capture(Key key){this.key=key;}
        private int height(int x,int z){
            if(key.layer()==MapLayerPolicy.Layer.SURFACE){return level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1;}
            int top=Math.min(level.getMaxY()-3,key.altitude()),bottom=Math.max(level.getMinY(),key.altitude()-28);
            for(int y=top;y>=bottom;y--){BlockPos p=new BlockPos(x,y,z);var state=level.getBlockState(p);
                if((!state.getFluidState().isEmpty()||!state.isAir()&&state.isSolidRender())&&level.getBlockState(p.above()).isAir()&&level.getBlockState(p.above(2)).isAir()){return y;}
            }return level.getMinY()-1;
        }
        private void column(){
            int x=key.x()*16+(index&15),z=key.z()*16+(index>>4);
            int y=height(x,z);BlockPos pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
            int depth=0;if(state.getFluidState().is(FluidTags.WATER)){for(int d=1;d<=32&&y-d>=level.getMinY();d++){if(!level.getFluidState(pos.below(d)).is(FluidTags.WATER)){break;}depth++;}}
            Short id=indices.get(state);if(id==null){id=(short)palette.size();indices.put(state,id);palette.add(MapColors.encode(state));}
            blocks[index]=id;heights[index]=(short)y;int tintIndex=colors.tintIndex(mc,state);
            var tintSource=state.getFluidState().isEmpty()?(tintIndex<0?null:mc.getBlockColors().getTintSource(state,tintIndex)):mc.getModelManager().getFluidStateModelSet().get(state.getFluidState()).tintSource();
            int tint=tintSource==null?-1:tintSource.colorInWorld(state,level,pos);
            if(index<16){edges[index]=(short)(level.getChunkSource().getChunk(key.x(),key.z()-1,ChunkStatus.FULL,false)==null?y:height(x,z-1));}
            if((index&15)==0){edges[16+(index>>4)]=(short)(level.getChunkSource().getChunk(key.x()-1,key.z(),ChunkStatus.FULL,false)==null?y:height(x-1,z));}
            int[] texture=colors.detail(mc,state);for(int dz=0;dz<4;dz++){for(int dx=0;dx<4;dx++){detail[((index>>4)*4+dz)*64+(index&15)*4+dx]=texture[dz*4+dx];}}
            tints[index]=tint==-1?0xFFFFFF:tint&0xFFFFFF;water[index]=(byte)depth;rgb[index]=y<level.getMinY()?0:0xFF000000|colors.color(mc,state);index++;
        }
        private SurfaceTile finish(){return new SurfaceTile(key.x(),key.z(),palette,blocks,heights,tints,water,rgb,detail,edges);}
    }
    public void resourceReload(){generation++;pending.clear();capture=null;colors.clear();textures.clear();}
    int[] recolorDetail(SurfaceTile tile){return colors.recolorDetail(mc,tile);}
    int[] recolor(SurfaceTile tile){return colors.recolor(mc,tile);}
    @Override public void close(){updates.close();terrainChanges.close();textures.shutdown();if(!waypointsReady&&!bufferedDeaths.isEmpty()){flushWaypointsSnapshot();}
        if(waypointsReady&&worldDirectory!=null){
            // Place the final snapshot behind pending writes, preserving their order at shutdown.
            var archives=waypoints.pendingArchives();JsonObject snapshot=waypoints.snapshot();Path path=worldDirectory.resolve("waypoints.json");
            long deadline=System.nanoTime()+2_000_000_000L;
            while(!submit(()->{try{persistWaypoints(path,snapshot,archives);}catch(java.io.IOException error){Client.INSTANCE.getLogger().error("Cannot persist waypoint snapshot; preserving prior index",error);}})&&System.nanoTime()<deadline){java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);}
        }
        worker.shutdown();try{long deadline=System.nanoTime()+5_000_000_000L;while(!worker.awaitTermination(10,TimeUnit.MILLISECONDS)&&System.nanoTime()<deadline){Completion completion;while((completion=completed.poll())!=null){if(completion.generation()==generation){completion.apply().run();}}}if(!worker.isTerminated()){worker.shutdownNow();}}catch(InterruptedException e){Thread.currentThread().interrupt();worker.shutdownNow();}}
}
