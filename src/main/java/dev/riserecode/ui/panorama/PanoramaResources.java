package dev.riserecode.ui.panorama;

import dev.riserecode.ui.panorama.ForestScene.Kind;
import java.io.ByteArrayOutputStream;
import java.nio.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Only capture() touches loaded resources. Workers consume value-only model snapshots. */
public final class PanoramaResources {
    public enum Pass { OPAQUE, CUTOUT, WATER, TRANSLUCENT }
    private record Vertex(float x,float y,float z,float u,float v) { }
    private record Quad(List<Vertex> vertices,int direction,int color,float nx,float ny,float nz,float sway,Pass pass,Identifier atlas) { }
    public record Snapshot(Map<Kind,List<List<Quad>>> models) { }
    public record Mesh(int cellX,int cellZ,Pass pass,Identifier atlas,byte[] bytes,int vertices) { }
    public record Prepared(ForestScene.Description scene,List<Mesh> meshes) { }
    private record Batch(int x,int z,Pass pass,Identifier atlas) { }
    public static Snapshot capture(){
        Minecraft mc=Minecraft.getInstance();if(!mc.isSameThread())throw new IllegalStateException("Resource capture requires client thread");
        var result=new EnumMap<Kind,List<List<Quad>>>(Kind.class);
        for(Kind kind:Kind.values()){
            BlockState state=state(kind);
            if(kind==Kind.VINE)state=state.setValue(net.minecraft.world.level.block.VineBlock.SOUTH,true);var variants=new ArrayList<List<Quad>>();
            if(kind==Kind.WATER){
                var fluid=mc.getModelManager().getFluidStateModelSet().get(Blocks.WATER.defaultBlockState().getFluidState());var sprite=fluid.stillMaterial().sprite();
                variants.add(List.of(new Quad(List.of(new Vertex(0,.12f,0,sprite.getU0(),sprite.getV0()),new Vertex(0,.12f,1,sprite.getU0(),sprite.getV1()),new Vertex(1,.12f,1,sprite.getU1(),sprite.getV1()),new Vertex(1,.12f,0,sprite.getU1(),sprite.getV0())),Direction.UP.ordinal(),0xFFFFFFFF,0,1,0,2,Pass.WATER,sprite.atlasLocation())));
            }else for(int variant=0;variant<4;variant++){
                var parts=new ArrayList<BlockStateModelPart>();mc.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(variant*4171L),parts);
                var quads=new ArrayList<Quad>();
                for(var part:parts){for(Direction d:Direction.values())copy(mc,state,kind,part.getQuads(d),d.ordinal(),quads);copy(mc,state,kind,part.getQuads(null),-1,quads);}
                variants.add(List.copyOf(quads));
            }
            result.put(kind,List.copyOf(variants));
        }
        return new Snapshot(Collections.unmodifiableMap(result));
    }
    private static void copy(Minecraft mc,BlockState state,Kind kind,List<BakedQuad> from,int face,List<Quad> out){
        for(var q:from){
            var m=q.materialInfo();var v=new ArrayList<Vertex>(4);
            for(int i=0;i<4;i++){var p=q.position(i);long uv=q.packedUV(i);v.add(new Vertex(p.x(),p.y(),p.z(),UVPair.unpackU(uv),UVPair.unpackV(uv)));}
            int color=-1;
            if(m.isTinted()){var tint=mc.getBlockColors().getTintSource(state,m.tintIndex());if(tint!=null)color=0xFF000000|tint.color(state);}
            var d=q.direction();boolean leaves=kind==Kind.LEAVES||kind==Kind.BIRCH_LEAVES||kind==Kind.SPRUCE_LEAVES;
            float sway=leaves?.18f:kind==Kind.VINE?.28f:kind==Kind.FERN||kind==Kind.GRASS_PLANT||kind==Kind.FLOWER||kind==Kind.BLUE_FLOWER?.65f:0;
            var layer=m.layer();Pass pass=layer==ChunkSectionLayer.TRANSLUCENT?Pass.TRANSLUCENT:layer==ChunkSectionLayer.CUTOUT||sway>0||kind==Kind.LILY?Pass.CUTOUT:Pass.OPAQUE;
            out.add(new Quad(List.copyOf(v),face,color,d.getStepX(),d.getStepY(),d.getStepZ(),sway,pass,m.sprite().atlasLocation()));
        }
    }
    public static Prepared prepare(Snapshot resources,double trees,double plants,boolean low,BooleanSupplier cancelled){
        var scene=ForestScene.create(trees,plants,low,cancelled);var batches=new LinkedHashMap<Batch,ByteArrayOutputStream>();var occupancy=scene.occupancy();
        int[] order={0,1,2,0,2,3};Direction[] directions=Direction.values();ByteBuffer vertex=ByteBuffer.allocate(44).order(ByteOrder.nativeOrder());
        for(var block:scene.blocks()){
            if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            var variants=resources.models.get(block.kind());var quads=variants.get((int)(ForestScene.hash(block.x(),block.z(),block.y())*variants.size()));
            // Cache sunlight occlusion in geometry; never recompute it during a frame.
            float shadow=1;
            if(!low)for(int step=2;step<15;step+=2){Kind hit=occupancy.get(ForestScene.key(block.x()-step,block.y()+step,block.z()-step/2));if(hit!=null&&hit!=Kind.WATER&&hit!=Kind.FERN&&hit!=Kind.GRASS_PLANT){shadow=.66f;break;}}
            for(var q:quads){
                if(q.direction>=0){Direction d=directions[q.direction];Kind neighbor=occupancy.get(ForestScene.key(block.x()+d.getStepX(),block.y()+d.getStepY(),block.z()+d.getStepZ()));if(neighbor!=null&&(ForestScene.solid(neighbor)||neighbor==block.kind()&&q.sway>0&&q.sway<.2))continue;}
                var key=new Batch(Math.floorDiv(block.x(),24),Math.floorDiv(block.z(),24),q.pass,q.atlas);var bytes=batches.computeIfAbsent(key,ignored->new ByteArrayOutputStream());
                for(int index:order){var v=q.vertices.get(index);vertex.clear();vertex.putFloat(block.x()+v.x).putFloat(block.y()+v.y-(block.kind()==Kind.LILY?.89f:0)).putFloat(block.z()+v.z).putFloat(v.u).putFloat(v.v);
                    vertex.put((byte)(q.color>>16)).put((byte)(q.color>>8)).put((byte)q.color).put((byte)255);
                    vertex.putFloat(q.nx).putFloat(q.ny).putFloat(q.nz).putFloat(q.sway>=.4f&&q.sway<1?q.sway*v.y:q.sway).putFloat(shadow);bytes.writeBytes(vertex.array());}
            }
        }
        var meshes=new ArrayList<Mesh>();batches.forEach((k,b)->{byte[] bytes=b.toByteArray();meshes.add(new Mesh(k.x,k.z,k.pass,k.atlas,bytes,bytes.length/44));});
        return new Prepared(scene,List.copyOf(meshes));
    }
    private static BlockState state(Kind kind){return (switch(kind){
        case GRASS->Blocks.GRASS_BLOCK;case DIRT->Blocks.DIRT;case MOSS->Blocks.MOSS_BLOCK;case ROCK->Blocks.MOSSY_COBBLESTONE;
        case LOG->Blocks.OAK_LOG;case BIRCH->Blocks.BIRCH_LOG;case LEAVES->Blocks.OAK_LEAVES;case BIRCH_LEAVES->Blocks.BIRCH_LEAVES;case SPRUCE_LOG->Blocks.SPRUCE_LOG;case SPRUCE_LEAVES->Blocks.SPRUCE_LEAVES;
        case FERN->Blocks.FERN;case GRASS_PLANT->Blocks.SHORT_GRASS;case FLOWER->Blocks.OXEYE_DAISY;case BLUE_FLOWER->Blocks.CORNFLOWER;case VINE->Blocks.VINE;case LILY->Blocks.LILY_PAD;case WATER->Blocks.WATER;
    }).defaultBlockState();}
    private PanoramaResources(){}
}
