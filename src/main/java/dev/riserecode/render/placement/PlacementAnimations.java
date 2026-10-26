package dev.riserecode.render.placement;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Client-thread animation ownership, immutable mesher policies and acknowledged mesh handoff. */
public final class PlacementAnimations {
    public record BuildView(long generation, Map<Long, BlockState> hidden) { }
    public record Draw(BlockPos pos, BlockState state, Matrix4f pose, MovingBlockRenderState model, double progress) { }
    private static final java.util.concurrent.atomic.AtomicLong bbeSuppressions=new java.util.concurrent.atomic.AtomicLong();
    public static long bbeSuppressions(){return bbeSuppressions.get();}
    public static void bbeSuppressed(){bbeSuppressions.incrementAndGet();}
    private static final Map<Long, Entry> entries=new LinkedHashMap<>();
    private static final Set<Long> dirty=new HashSet<>();
    private static final Map<Long, Long> uploaded=new ConcurrentHashMap<>();
    private static final ThreadLocal<BuildView> build=new ThreadLocal<>();
    private static volatile Set<Long> watched=Set.of();
    private static volatile BuildView policy=new BuildView(1,Map.of());
    private static List<Draw> frame=List.of();
    private static ClientLevel level;
    private static long generation=1, starts, modelDraws, shadowDraws, rebuilds;
    private static boolean active;
    private static java.util.function.BooleanSupplier reducedMotion=()->dev.riserecode.ui.theme.Theme.bool("Reduced Motion",false);
    private static java.util.function.LongSupplier clock=System::nanoTime;
    public static long time(){return clock.getAsLong();}
    /** Injectable monotonic clock for deterministic render checks. Call on the client thread. */
    public static void timeSource(java.util.function.LongSupplier source){clock=java.util.Objects.requireNonNull(source);}
    private static final class Entry {
        final BlockPos pos; final BlockState state; final PlacementMotion.Settings settings; final long started;
        final double yaw; final MovingBlockRenderState model;
        long hide,restore; boolean restoring; Group group;
        Entry(BlockPos pos,BlockState state,PlacementMotion.Settings settings,double yaw,long now) {
            this.pos=pos.immutable();this.state=state;this.settings=settings;this.yaw=yaw;started=now;
            model=new Snapshot(level,pos,state);
        }
    }
    private static final class Group {
        final List<Entry> members = new ArrayList<>();
        final BlockPos origin;
        long motionStart = -1;
        Group(BlockPos origin) { this.origin = origin; }
    }
    /** Real neighbor states retain AO/tints instead of treating every placed block as isolated. */
    private static final class Snapshot extends MovingBlockRenderState {
        private final Map<Long,BlockState> neighbors;
        Snapshot(ClientLevel world,BlockPos pos,BlockState state) {
            blockPos=randomSeedPos=pos.immutable();blockState=state;biome=world.getBiome(pos);
            cardinalLighting=world.cardinalLighting();lightEngine=world.getLightEngine();
            var states=new HashMap<Long,BlockState>();
            for(var p:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2))){states.put(p.asLong(),world.getBlockState(p));}
            neighbors=Map.copyOf(states);
        }
        @Override public BlockState getBlockState(BlockPos pos) { return neighbors.getOrDefault(pos.asLong(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()); }
    }
    public static boolean isPlacementModel(Object state){return state instanceof Snapshot;}
    private PlacementAnimations() { }
    public static void enable(java.util.function.BooleanSupplier preference) { syncLevel();reducedMotion=java.util.Objects.requireNonNull(preference);active=true; }
    public static boolean active() { return active; }
    public static long starts() { return starts; }
    public static long modelDraws() { return modelDraws; }
    public static long shadowDraws() { return shadowDraws; }
    public static long rebuilds() { return rebuilds; }
    public static int pending() { return entries.size(); }
    public static List<Draw> draws() { return frame; }
    public static void syncLevel() {
        ClientLevel next=Minecraft.getInstance().level;
        if(level!=next){ clear(false);level=next; }
    }
    public static boolean start(BlockPos pos,BlockState state,Vec3 placer,PlacementMotion.Settings settings,double range,int limit) {
        syncLevel();var mc=Minecraft.getInstance();
        if(!active||level==null||mc.player==null||settings.duration()<=0||reducedMotion.getAsBoolean())return false;
        if(!level.getChunkSource().hasChunk(pos.getX()>>4,pos.getZ()>>4)||pos.distToCenterSqr(mc.player.position())>range*range||entries.containsKey(pos.asLong())||entries.size()>=limit)return false;
        if(state.isAir()||!state.getFluidState().isEmpty()||state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                ||state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART)
                )return false;
        if(state.getRenderShape()!=RenderShape.MODEL&&!state.hasBlockEntity())return false;
        if(dev.riserecode.render.ShaderWorldOverlay.active()&&(state.getLightEmission()>0||!PlacementShaderBridge.supported()))return false;
        double yaw=Math.atan2(placer.x-pos.getX()-.5,placer.z-pos.getZ()-.5);
        var positions=new ArrayList<BlockPos>();positions.add(pos);
        if(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock && state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)!=net.minecraft.world.level.block.state.properties.ChestType.SINGLE){
            var other=pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(state));
            if(level.getBlockState(other).getBlock()==state.getBlock())positions.add(other);
        }
        if(entries.size()+positions.stream().filter(p->!entries.containsKey(p.asLong())).count()>limit)return false;
        // Retarget both halves together when a single chest becomes a double chest.
        for(var member:positions){var old=entries.remove(member.asLong());if(old!=null)dirty(member);}
        var origin=new BlockPos(positions.stream().mapToInt(BlockPos::getX).min().orElse(pos.getX()),pos.getY(),positions.stream().mapToInt(BlockPos::getZ).min().orElse(pos.getZ()));
        var group=new Group(origin);
        for(var member:positions){var entry=new Entry(member,level.getBlockState(member),settings,yaw,time());entry.group=group;group.members.add(entry);entries.put(member.asLong(),entry);}
        starts++;publish();for(var entry:group.members){entry.hide=policy.generation();dirty(entry.pos);}return true;
    }
    private static void publish() {
        var hidden=new HashMap<Long,BlockState>();
        for(var e:entries.values())if(!e.restoring&&(e.state.getRenderShape()==RenderShape.MODEL||e.state.hasBlockEntity()))hidden.put(e.pos.asLong(),e.state);
        policy=new BuildView(++generation,Map.copyOf(hidden));
        watched=entries.values().stream().map(e->SectionPos.asLong(e.pos)).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    private static void dirty(BlockPos pos) {
        // Include border neighbors; hiding a block also reveals their formerly culled faces.
        for(var p:List.of(pos,pos.north(),pos.south(),pos.east(),pos.west(),pos.above(),pos.below()))dirty.add(SectionPos.asLong(p));
    }
    private static void restore(Entry e) {
        if(e.restoring)return;
        e.restoring=true;publish();e.restore=policy.generation();dirty(e.pos);
    }
    public static void stop() { active=false;for(var e:entries.values())restore(e);flushDirty(); }
    public static void clear(boolean restoreWorld) {
        if(restoreWorld&&level!=null){for(var e:entries.values())dirty(e.pos);}
        entries.clear();policy=new BuildView(++generation,Map.of());frame=List.of();uploaded.clear();watched=Set.of();
        if(restoreWorld)flushDirty();else dirty.clear();
        PlacementShaderBridge.close();
    }
    private static void flushDirty() {
        if(level!=null)for(long key:dirty){var p=SectionPos.of(key).origin();var state=level.getBlockState(p);level.sendBlockUpdated(p,state,state,Block.UPDATE_CLIENTS|Block.UPDATE_IMMEDIATE);rebuilds++;}
        dirty.clear();
    }
    public static void frame(long now) {
        syncLevel();if(level==null)return;
        var draws=new ArrayList<Draw>(entries.size());
        var groups=entries.values().stream().map(e->e.group).distinct().toList();
        for(var group:groups){
            boolean valid=group.members.stream().allMatch(e->level.getChunkSource().hasChunk(e.pos.getX()>>4,e.pos.getZ()>>4)&&level.getBlockState(e.pos)==e.state);
            if(!valid){for(var e:group.members){entries.remove(e.pos.asLong());dirty(e.pos);}publish();continue;}
            boolean ready=group.members.stream().allMatch(e->!requiresMesh(e)||uploaded.getOrDefault(SectionPos.asLong(e.pos),0L)>=e.hide);
            if(group.motionStart<0&&ready)group.motionStart=now;
            boolean abandoned=group.motionStart<0&&now-group.members.getFirst().started>=1_000_000_000L;
            double progress=group.motionStart<0?0:Math.clamp((now-group.motionStart)*1e-9/group.members.getFirst().settings.duration(),0,1);
            if(reducedMotion.getAsBoolean()||!active||abandoned)progress=1;
            if(progress>=1)for(var e:group.members)restore(e);
            boolean restored=group.members.stream().allMatch(e->e.restoring&&(!requiresMesh(e)||uploaded.getOrDefault(SectionPos.asLong(e.pos),0L)>=e.restore));
            if(restored){for(var e:group.members)entries.remove(e.pos.asLong());continue;}
            // A failed handoff never starts moving. Let the original static owner finish restoring.
            if(group.motionStart<0)continue;
            var first=group.members.getFirst();
            double cx=group.members.stream().mapToDouble(e->e.pos.getX()+.5).average().orElse(.5)-group.origin.getX();
            double cz=group.members.stream().mapToDouble(e->e.pos.getZ()+.5).average().orElse(.5)-group.origin.getZ();
            var shared=new Matrix4f().translation((float)(cx-.5),0,(float)(cz-.5)).mul(PlacementMotion.pose(first.settings,first.restoring?1:progress,first.yaw)).translate((float)(.5-cx),0,(float)(.5-cz));
            for(var e:group.members){
                int dx=e.pos.getX()-group.origin.getX(),dz=e.pos.getZ()-group.origin.getZ();
                var pose=new Matrix4f().translation(-dx,0,-dz).mul(shared).translate(dx,0,dz);
                draws.add(new Draw(e.pos,e.state,pose,e.model,e.restoring?1:progress));
            }
        }
        frame=List.copyOf(draws);watched=entries.values().stream().map(e->SectionPos.asLong(e.pos)).collect(java.util.stream.Collectors.toUnmodifiableSet());uploaded.keySet().retainAll(watched);flushDirty();
    }
    private static boolean requiresMesh(Entry e){return e.state.getRenderShape()==RenderShape.MODEL||e.state.hasBlockEntity()&&net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("betterblockentities");}
    /** BBE stays the static owner until the hidden mesh upload is acknowledged. */
    public static boolean immediateBlockEntity(BlockPos pos){return at(pos)!=null;}
    /** True once the terrain upload acknowledges the current suppression policy. */
    public static boolean terrainReady(BlockPos pos){return uploaded.getOrDefault(SectionPos.asLong(pos),0L)>=policy.generation();}
    public static Draw at(BlockPos pos) { for(var d:frame)if(d.pos.equals(pos))return d;return null; }
    public static Vec3 transform(Draw d,Vec3 point) {
        var p=d.pose.transformPosition(new Vector3f((float)(point.x-d.pos.getX()),(float)(point.y-d.pos.getY()),(float)(point.z-d.pos.getZ())));
        return new Vec3(p.x+d.pos.getX(),p.y+d.pos.getY(),p.z+d.pos.getZ());
    }
    public static void submit(SubmitNodeCollector collector,Vec3 camera) { submit(collector,camera,false); }
    public static void submit(SubmitNodeCollector collector,Vec3 camera,boolean shadow) {
        for(var d:frame){if(d.state.getRenderShape()!=RenderShape.MODEL)continue;
            var pose=new PoseStack();pose.translate(d.pos.getX()-camera.x,d.pos.getY()-camera.y,d.pos.getZ()-camera.z);pose.mulPose(d.pose);
            collector.submitMovingBlock(pose,d.model,0);if(shadow)shadowDraws++;else modelDraws++;
        }
    }
    public static void blockEntity(BlockPos pos,PoseStack pose){var d=at(pos);if(d!=null)pose.mulPose(d.pose);}
    public static long beginBuild(){var snapshot=policy;build.set(snapshot);return snapshot.generation();}
    public static void endBuild(){build.remove();}
    public static boolean hidden(BlockPos pos,BlockState state){var b=build.get();return b!=null&&b.hidden().get(pos.asLong())==state;}
    public static boolean hiddenNeighbor(BlockPos pos){var b=build.get();return b!=null&&b.hidden().containsKey(pos.asLong());}
    public static void meshReady(long section,long stamp){if(stamp>0&&watched.contains(section))uploaded.merge(section,stamp,Math::max);}
}
