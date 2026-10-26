package dev.riserecode.cosmetics;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;

/** Client-thread outfit ownership; workers decode assets, extraction exports immutable values. */
public final class CosmeticsManager implements AutoCloseable {
    public record Asset(CosmeticModel model,List<Identifier> textures,String sha256,long nativeImageBytes,long ownedGpuBytes){public Asset{textures=List.copyOf(textures);}public Asset(CosmeticModel model,List<Identifier> textures,String sha256){this(model,textures,sha256,0,0);}public long decodedCpuBytes(){return model==null?0:model.decodedBytes();}}
    private record Decoded(CosmeticModel model,List<NativeImage> images,long pixels){ }
    private final Set<Decoded> decodedPending=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<String> pinned=new HashSet<>();
    private final List<Asset> retired=new ArrayList<>();
    private volatile boolean closed;
    private final java.util.concurrent.atomic.AtomicInteger inFlight=new java.util.concurrent.atomic.AtomicInteger();
    private long textureSerial;
    private static final Asset PROCEDURAL=new Asset(null,List.of(),"-");
    private static final class Requested {long tick;}
    private final Map<String,Requested> requested=new HashMap<>();
    private void requested(String key){
        var value=requested.get(key);if(value==null){if(requested.size()>=256){requested.remove(requested.keySet().iterator().next());}value=new Requested();requested.put(key,value);}value.tick=dev.riserecode.performance.PerformancePolicy.ticks();
    }
    private boolean active(String key){for(var item:outfit.items().values()){if(item.id().equals(key)){return true;}}var preview=requested.get(key);return preview!=null&&dev.riserecode.performance.PerformancePolicy.ticks()-preview.tick<=2;}

    public void beginFrame(){retired.forEach(this::destroy);retired.clear();pinned.clear();trim();}
    private long footprint(Asset a){return a.decodedCpuBytes()+a.nativeImageBytes()+a.ownedGpuBytes();}
    private void trim(){
        var policy=dev.riserecode.performance.PerformancePolicy.current();int entries=assets.size()+retired.size();long bytes=0;
        for(var asset:assets.values()){bytes+=footprint(asset);}for(var asset:retired){bytes+=footprint(asset);}
        var it=assets.entrySet().iterator();
        while((entries>policy.unusedCosmeticEntries()||bytes>policy.unusedCosmeticBytes())&&it.hasNext()){
            var e=it.next();if(active(e.getKey())||pinned.contains(e.getKey())){continue;}entries--;bytes-=footprint(e.getValue());destroy(e.getValue());it.remove();
        }
    }
    public record Statistics(int entries,int unusedEntries,int pinnedEntries,int pending,long decodedCpuBytes,long nativeImageBytes,long ownedGpuBytes,int entryOverage,long byteOverage,int activeEntries,long workingSetFloorBytes) { }
    public Statistics statistics(){
        long cpu=0,nativeBytes=0,gpu=0,unusedBytes=0,floor=0;int unused=0,activeEntries=0;
        for(var e:assets.entrySet()){var a=e.getValue();cpu+=a.decodedCpuBytes();nativeBytes+=a.nativeImageBytes();gpu+=a.ownedGpuBytes();if(!active(e.getKey())){unused++;unusedBytes+=footprint(a);}else{activeEntries++;}if(active(e.getKey())||pinned.contains(e.getKey())){floor+=footprint(a);}}
        for(var a:retired){cpu+=a.decodedCpuBytes();nativeBytes+=a.nativeImageBytes();gpu+=a.ownedGpuBytes();floor+=footprint(a);unusedBytes+=footprint(a);}
        for(var d:decodedPending){cpu+=d.model()==null?0:d.model().decodedBytes();nativeBytes+=d.pixels();}
        var p=dev.riserecode.performance.PerformancePolicy.current();return new Statistics(assets.size()+retired.size(),unused,pinned.size()+retired.size(),inFlight.get(),cpu,nativeBytes,gpu,Math.max(0,assets.size()+retired.size()-p.unusedCosmeticEntries()),Math.max(0,cpu+nativeBytes+gpu-p.unusedCosmeticBytes()),activeEntries,floor);
    }
    private final CosmeticsLibrary library;
    private final Map<String,Asset> assets=new LinkedHashMap<>(16,.75f,true);
    private final Set<String> loading=new HashSet<>(),failed=new HashSet<>();
    private final List<EventBus.Subscription> subscriptions=new ArrayList<>();
    private final CosmeticsMotion motion=new CosmeticsMotion();
    private CosmeticLoadout outfit=CosmeticLoadout.empty();
    private Runnable changed=()->{};
    private volatile long generation;
    private Object world;
    public CosmeticsManager(Path directory){library=new CosmeticsLibrary(directory.resolve("cosmetics"));}
    public CosmeticsLibrary library(){return library;}
    public CosmeticLoadout outfit(){return outfit;}
    public CosmeticsMotion motion(){return motion;}
    public JsonObject snapshot(){return outfit.json();}
    public void onChange(Runnable action){changed=action;}
    public void apply(CosmeticLoadout value){
        for(var entry:value.items().entrySet()){var d=library.find(entry.getValue().id());if(d!=null&&d.category()!=entry.getKey()){throw new IllegalArgumentException("Cosmetic belongs to another category");}}
        if(!outfit.equals(value)){generation++;loading.clear();}
        outfit=value;motion.clear();changed.run();
    }
    public void restore(JsonObject json){apply(CosmeticLoadout.read(json));}
    public void validate(JsonObject json){var parsed=CosmeticLoadout.read(json);for(var e:parsed.items().entrySet()){var d=library.find(e.getValue().id());if(d!=null&&d.category()!=e.getKey()){throw new IllegalArgumentException("Cosmetic category mismatch");}}}
    public void bind(EventBus bus){
        subscriptions.add(bus.subscribe(UpdateEvent.class,e->{var mc=e.getMinecraft();if(mc.level!=world){world=mc.level;motion.clear();}
            trim();if(mc.player==null||!outfit.visible()){motion.clear();return;}motion.tick(mc.player.position());for(var item:outfit.items().values()){request(item.id());}}));
        subscriptions.add(bus.subscribe(CosmeticExtractEvent.class,e->{
            var state=(CosmeticRenderState)e.state;state.risecode$outfit(null);state.risecode$preview(false);var mc=Minecraft.getInstance();
            if(e.player!=mc.player||e.state.isInvisible||e.state.isSpectator||!outfit.visible()){return;}
            state.risecode$outfit(outfit);patchCape(e.state,outfit);
        }));
        subscriptions.add(bus.subscribe(CosmeticSubmitEvent.class,e->CosmeticsRenderer.submit(this,e)));
        subscriptions.add(bus.subscribe(Render3DEvent.class,e->CosmeticsRenderer.trail(this,e)));
    }
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath("riserecode","cosmetics/"+path);}
    public void patchCape(net.minecraft.client.renderer.entity.state.AvatarRenderState state,CosmeticLoadout value){
        var item=value.items().get(CosmeticCategory.CAPE);if(item==null||!value.visible()||!state.showCape){return;}var asset=request(item.id());
        if(asset==null||asset.textures.isEmpty()||state.skin==null){return;}
        Identifier texture=asset.textures.getFirst();var original=state.skin;
        state.skin=new PlayerSkin(original.body(),new ClientAsset.ResourceTexture(texture,texture),original.elytra(),original.model(),original.secure());
    }
    public Asset request(String item){
        Asset cached=assets.get(item);if(cached!=null){pinned.add(item);}var d=library.find(item);if(d!=null||cached!=null){requested(item);}if(cached!=null&&(d==null||cached.sha256().equals(d.sha256()))){pinned.add(item);return cached;}
        if(d==null||!library.available(d)||loading.contains(item)||failed.contains(item)){return cached;}
        if(d.asset().equals("none")){return PROCEDURAL;}
        if(closed||inFlight.get()>=16){return cached;}
        loading.add(item);inFlight.incrementAndGet();long token=generation;
        library.work(()->{
            if(token!=generation){throw new java.util.concurrent.CancellationException();}
            byte[] bytes=library.read(d);var images=new ArrayList<NativeImage>();CosmeticModel model=null;
            try{if(d.category()==CosmeticCategory.COMPANION){model=CosmeticModel.read(bytes);for(byte[] png:model.images()){images.add(NativeImage.read(png));}}
                else{CosmeticModel.png(bytes,2048);images.add(NativeImage.read(bytes));}
                long pixels=0;for(var image:images){pixels+=image.getWidth()*(long)image.getHeight()*4;}
                var decoded=new Decoded(model,List.copyOf(images),pixels);decodedPending.add(decoded);if(closed){if(decodedPending.remove(decoded)){images.forEach(NativeImage::close);}throw new java.util.concurrent.CancellationException();}return decoded;
            }catch(Exception e){images.forEach(NativeImage::close);throw e;}
        }).whenComplete((decoded,error)->{
            if(closed){inFlight.decrementAndGet();if(decoded!=null&&decodedPending.remove(decoded)){decoded.images.forEach(NativeImage::close);}return;}
            Minecraft.getInstance().execute(()->{
                inFlight.decrementAndGet();
                if(decoded!=null&&!decodedPending.remove(decoded)){return;}
                if(token!=generation||closed){if(decoded!=null){decoded.images.forEach(NativeImage::close);}return;}loading.remove(item);
                if(error!=null){failed.add(item);return;}
                var textures=new ArrayList<Identifier>();int index=0;
                try{
                    for(var image:decoded.images){var identifier=id("dynamic/"+d.id()+"_"+d.sha256()+"_"+textureSerial++ +"_"+index++);Minecraft.getInstance().getTextureManager().register(identifier,new DynamicTexture(()->"risecode cosmetic "+item,image));textures.add(identifier);}
                    release(item);assets.put(item,new Asset(decoded.model,textures,d.sha256(),decoded.pixels,decoded.pixels));trim();
                }catch(RuntimeException failure){textures.forEach(t->Minecraft.getInstance().getTextureManager().release(t));for(int i=textures.size();i<decoded.images.size();i++){decoded.images.get(i).close();}failed.add(item);}
            });
        });return cached;
    }
    public boolean failed(String id){return failed.contains(id);}
    public void retry(){failed.clear();}
    public int loadedAssets(){return assets.size();}
    private void destroy(Asset asset){asset.textures.forEach(t->Minecraft.getInstance().getTextureManager().release(t));}
    private void release(String key){var asset=assets.remove(key);if(asset!=null){if(pinned.remove(key)){retired.add(asset);}else{destroy(asset);}}}
    public void resourceReload(){requested.clear();generation++;loading.clear();failed.clear();new ArrayList<>(assets.keySet()).forEach(this::release);motion.clear();}
    @Override public void close(){closed=true;subscriptions.forEach(EventBus.Subscription::close);subscriptions.clear();resourceReload();library.close();beginFrame();for(var d:decodedPending){if(decodedPending.remove(d)){d.images.forEach(NativeImage::close);}}}
}
