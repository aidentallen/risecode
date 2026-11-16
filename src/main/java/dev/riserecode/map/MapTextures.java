package dev.riserecode.map;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Backend-neutral textures, four uploads/frame, bounded by count and actual pixel bytes. */
public final class MapTextures {
    public static final long GPU_LIMIT=128L*1024*1024;
    private record Entry(Identifier id,int size,long bytes) { }
    private final MapService maps;
    private final dev.riserecode.ui.render.FramePinnedCache<MapService.Key,Entry> cache=new dev.riserecode.ui.render.FramePinnedCache<>(512,e->mapsRelease(e));
    private void mapsRelease(Entry e){bytes-=e.bytes();maps.minecraft().getTextureManager().release(e.id());}
    private int uploads;
    private final Set<MapService.Key> stale=new HashSet<>();
    private long serial,bytes;
    MapTextures(MapService maps){this.maps=maps;}
    public int size(){return cache.size();}
    public long bytes(){return bytes;}
    public void beginFrame(){uploads=0;cache.maximum(dev.riserecode.performance.PerformancePolicy.frame().mapGpuEntries());cache.beginFrame();}
    public int overage(){return cache.overage();}
    public int ownedSize(){return cache.ownedSize();}
    public Identifier texture(MapService.Key key,SurfaceTile tile,float pixelsPerBlock){
        Entry cached=cache.get(key);
        boolean overview=key.level()>0&&tile.detailed(),detailed=tile.detailed()&&pixelsPerBlock>(cached!=null&&cached.size()==64?1.4:1.8);
        int stride=detailed?1:1<<MapLodPolicy.leafMipLevel(pixelsPerBlock,4),size=detailed?64:16/stride;
        if(cached!=null&&cached.size()==size&&!stale.contains(key)){return cached.id();}if(uploads>=8){return cached==null?null:cached.id();}uploads++;remove(key);
        int[] colors=overview&&detailed?tile.detail():detailed?maps.recolorDetail(tile):maps.recolor(tile);NativeImage pixels=new NativeImage(size,size,false);
        for(int z=0;z<size;z++){for(int x=0;x<size;x++){
            if(detailed){pixels.setPixel(x,z,colors[z*64+x]);continue;}
            long r=0,g=0,b=0;int known=0;
            for(int dz=0;dz<stride;dz++){for(int dx=0;dx<stride;dx++){int color=colors[(z*stride+dz)*16+x*stride+dx];if((color>>>24)==0){continue;}known++;r+=color>>16&255;g+=color>>8&255;b+=color&255;}}
            int n=Math.max(1,known);pixels.setPixel(x,z,(known==0?0:0xFF000000)|(int)(r/n)<<16|(int)(g/n)<<8|(int)(b/n));
        }}
        Identifier id=Identifier.fromNamespaceAndPath("riserecode","map/tile_"+(serial++));maps.minecraft().getTextureManager().register(id,new DynamicTexture(()->"risecode terrain",pixels));
        long resident=size*(long)size*4;bytes+=resident;cache.putPinned(key,new Entry(id,size,resident));return id;
    }
    public void remove(MapService.Key key){stale.remove(key);cache.remove(key);}
    /** The old picture stays on screen until the replacement is uploaded, so a recaptured tile never blinks. */
    public void invalidate(MapService.Key key){if(stale.size()>=4096){stale.clear();}stale.add(key);}
    public void clear(){stale.clear();cache.discard();}
    public void shutdown(){clear();cache.beginFrame();}
}
