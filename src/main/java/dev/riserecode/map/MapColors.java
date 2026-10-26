package dev.riserecode.map;

import dev.riserecode.mixin.SpritePixelsAccessor;
import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.Direction;

/** Top-face texture samples on the client thread, without backend-specific calls or GPU readbacks. */
public final class MapColors {
    private record Sample(int rgb,int tintIndex,int[] pixels) { }
    private final Map<String,BlockState> states=new LinkedHashMap<>();
    private final Map<BlockState,Sample> colors=new LinkedHashMap<>();
    public void clear(){states.clear();colors.clear();}
    public static String encode(BlockState state){return BlockState.CODEC.encodeStart(JsonOps.INSTANCE,state).getOrThrow().toString();}
    public int color(Minecraft mc,BlockState state){return sample(mc,state).rgb();}
    public int tintIndex(Minecraft mc,BlockState state){return sample(mc,state).tintIndex();}
    private Sample sample(Minecraft mc,BlockState state){
        Sample cached=colors.get(state);if(cached!=null){return cached;}
        TextureAtlasSprite sprite;int tintIndex=-1;
        if(!state.getFluidState().isEmpty()){
            sprite=mc.getModelManager().getFluidStateModelSet().get(state.getFluidState()).stillMaterial().sprite();tintIndex=0;
        }else{
            var model=mc.getModelManager().getBlockStateModelSet().get(state);sprite=model.particleMaterial().sprite();
            List<BlockStateModelPart> parts=new ArrayList<>();model.collectParts(net.minecraft.util.RandomSource.create(42),parts);
            double highest=-Double.MAX_VALUE;
            for(var part:parts){var quads=new ArrayList<>(part.getQuads(Direction.UP));for(var q:part.getQuads(null)){if(q.direction()==Direction.UP){quads.add(q);}}
                for(var q:quads){double y=(q.position0().y()+q.position1().y()+q.position2().y()+q.position3().y())/4.0;
                    if(y>highest+0.001||Math.abs(y-highest)<0.001&&q.materialInfo().isTinted()){
                        highest=y;sprite=q.materialInfo().sprite();tintIndex=q.materialInfo().tintIndex();
                    }
                }
            }
        }
        var image=((SpritePixelsAccessor)(Object)sprite.contents()).risecode$image();
        long r=0,g=0,b=0,n=0;int w=Math.min(image.getWidth(),sprite.contents().width()),h=Math.min(image.getHeight(),sprite.contents().height());
        for(int y=0;y<h;y+=Math.max(1,h/16)){for(int x=0;x<w;x+=Math.max(1,w/16)){int c=image.getPixel(x,y);if((c>>>24)<128){continue;}r+=c>>16&255;g+=c>>8&255;b+=c&255;n++;}}
        int rgb=n==0?0x777777:(int)(r/n)<<16|(int)(g/n)<<8|(int)(b/n);int[] detail=new int[16];
        for(int dz=0;dz<4;dz++){for(int dx=0;dx<4;dx++){
            long sr=0,sg=0,sb=0,sn=0;
            for(int iy=dz*h/4;iy<Math.max(dz*h/4+1,(dz+1)*h/4);iy++){for(int ix=dx*w/4;ix<Math.max(dx*w/4+1,(dx+1)*w/4);ix++){int c=image.getPixel(Math.min(ix,w-1),Math.min(iy,h-1));if((c>>>24)<128){continue;}sr+=c>>16&255;sg+=c>>8&255;sb+=c&255;sn++;}}
            detail[dz*4+dx]=sn==0?rgb:(int)(sr/sn)<<16|(int)(sg/sn)<<8|(int)(sb/sn);
        }}
        Sample result=new Sample(rgb,tintIndex,detail);
        if(colors.size()>=2048){colors.remove(colors.keySet().iterator().next());}colors.put(state,result);return result;
    }
    public int[] detail(Minecraft mc,BlockState state){return sample(mc,state).pixels().clone();}
    public int[] recolorDetail(Minecraft mc,SurfaceTile tile){
        int[][] palette=new int[tile.palette().size()][];
        for(int i=0;i<palette.length;i++){String key=tile.palette().get(i);BlockState state=states.computeIfAbsent(key,k->BlockState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(k)).getOrThrow());palette[i]=detail(mc,state);}
        int[] known=tile.colors(),result=new int[4096];
        for(int i=0;i<256;i++){if((known[i]>>>24)==0){continue;}for(int dz=0;dz<4;dz++){for(int dx=0;dx<4;dx++){
            result[((i>>4)*4+dz)*64+(i&15)*4+dx]=SurfaceTile.shade(palette[tile.block(i)][dz*4+dx],tile.tint(i),tile.slope(i),tile.water(i));
        }}}return result;
    }
    public int[] recolor(Minecraft mc,SurfaceTile tile){
        int[] palette=new int[tile.palette().size()];
        for(int i=0;i<palette.length;i++){
            String key=tile.palette().get(i);BlockState state=states.computeIfAbsent(key,k->BlockState.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(k)).getOrThrow());palette[i]=color(mc,state);
        }
        if(states.size()>4096){states.clear();}
        int[] known=tile.colors(),result=new int[256];for(int i=0;i<256;i++){if((known[i]>>>24)==0){continue;}result[i]=SurfaceTile.shade(palette[tile.block(i)],tile.tint(i),tile.slope(i),tile.water(i));}return result;
    }
}
