package dev.riserecode;

import dev.riserecode.cosmetics.*;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.config.ConfigManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.LoggerFactory;

/** Asset, profile and cancellation contracts exercised without graphics or network. */
public final class CosmeticsChecks {
    private static int assertions;
    private static void check(boolean b,String message){assertions++;if(!b){throw new AssertionError(message);}}
    private static void reject(Runnable r,String message){boolean failed=false;try{r.run();}catch(RuntimeException e){failed=true;}check(failed,message);}
    private static void await(CosmeticsLibrary library)throws Exception{for(int i=0;i<500&&library.busy();i++){Thread.sleep(10);}check(!library.busy(),"bounded operation completed");}
    public static void main(String[] args)throws Exception{
        var empty=CosmeticLoadout.empty();check(empty.items().isEmpty(),"nothing equipped by default");
        for(var category:CosmeticCategory.values()){
            var next=empty.with(category,CosmeticLoadout.Item.defaults("test"));check(CosmeticLoadout.read(next.json()).equals(next),"category round trip: "+category);
            check(next.with(category,null).equals(empty),"category removal");
        }
        reject(()->new CosmeticLoadout.Item("../escape",-1,-1,1,1,0,0,0,1,1,1),"unsafe cosmetic identity");
        reject(()->new CosmeticLoadout.Item("valid",-1,-1,Double.NaN,1,0,0,0,1,1,1),"finite settings");
        var directory=Files.createTempDirectory("risecode-cosmetics-check-");
        try(var library=new CosmeticsLibrary(directory,(relative,limit)->{throw new AssertionError("No implicit network");})){
            check(library.definitions().size()==44,"complete catalog");
            check(library.definitions().stream().filter(library::available).count()==26,"offline starter collection");
            check(library.definitions().stream().filter(d->d.category()==CosmeticCategory.COMPANION).count()==21,"open companion catalog");
            for(var def:library.definitions()){
                check(!def.creator().isBlank()&&!def.source().isBlank()&&!def.license().isBlank(),"asset provenance: "+def.id());
                if(library.available(def)&&!def.asset().equals("none")){
                    byte[] bytes=library.read(def);check(CosmeticsLibrary.hash(bytes).equals(def.sha256()),"bundled digest: "+def.id());
                    if(def.category()==CosmeticCategory.COMPANION){
                        var model=CosmeticModel.read(bytes);var first=model.sample(.2F,false);var repeat=model.sample(.2F,false);var next=model.sample(.7F,false);
                        check(!first.isEmpty()&&!model.images().isEmpty(),"real animated textured companion");boolean changed=false;
                        for(int p=0;p<first.size();p++){float[] a=first.get(p).vertices(),b=repeat.get(p).vertices(),c=next.get(p).vertices();check(Arrays.equals(a,b),"render sampling is repeatable");
                            changed|=!Arrays.equals(a,c);for(int i=0;i<a.length;i++){check(Float.isFinite(a[i]),"finite skinned vertex");}for(int index:first.get(p).indices()){check(index>=0&&index<a.length/8,"valid model index");}}
                        check(changed,"source skeletal animation advances");
                    }
                }
            }
            byte[] png=library.read(library.find("cape-gradient"));Path source=directory.resolve("cape.png");Files.write(source,png);
            var imported=library.importCape(source).get(5,TimeUnit.SECONDS);check(imported.id().startsWith("import-")&&library.available(imported),"cape import stored by content");
            check(library.importCape(source).get(5,TimeUnit.SECONDS).id().equals(imported.id()),"imports deduplicate");
            Files.write(source,new byte[]{1,2,3});boolean invalid=false;try{library.importCape(source).get(5,TimeUnit.SECONDS);}catch(java.util.concurrent.ExecutionException e){invalid=true;}check(invalid,"invalid cape rejected");
            reject(()->CosmeticsLibrary.parse("{\"version\":9,\"entries\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"unsupported catalog");
            reject(()->new CosmeticDefinition("bad","Bad",CosmeticCategory.CAPE,"cape","../bad","-",0,"a","s","CC0"),"path traversal");
            byte[] glb=library.read(library.find("pet-bee"));String hash=CosmeticsLibrary.hash(glb);var d=new CosmeticDefinition("test-pet","Test",CosmeticCategory.COMPANION,"companion","test/"+hash+".glb",hash,glb.length,"Quaternius","test","CC0");
            try(var downloads=new CosmeticsLibrary(directory.resolve("downloads"),(path,limit)->glb)){
                downloads.download(d);await(downloads);check(Arrays.equals(downloads.read(d),glb),"verified download cached offline");
                Files.write(downloads.directory().resolve(d.asset()),new byte[]{1});boolean corrupt=false;try{downloads.read(d);}catch(java.io.IOException e){corrupt=true;}check(corrupt,"corrupt cache never decoded");
            }
            byte[] replacement=library.read(library.find("cape-chevron"));var updated=new CosmeticDefinition("cape-gradient","Updated",CosmeticCategory.CAPE,"cape","capes/gradient.png",CosmeticsLibrary.hash(replacement),replacement.length,"risecode","test","MIT");
            try(var updates=new CosmeticsLibrary(directory.resolve("updates"),(path,limit)->replacement)){
                check(!updates.available(updated),"new digest does not use stale bundled asset");updates.download(updated);await(updates);check(updates.available(updated)&&Arrays.equals(updates.read(updated),replacement),"validated update replaces old bundle version");
            }
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            try(var cancelled=new CosmeticsLibrary(directory.resolve("cancel"),(path,limit)->{entered.countDown();release.await();return glb;})){
                cancelled.download(d);check(entered.await(2,TimeUnit.SECONDS),"download started");cancelled.cancel();release.countDown();Thread.sleep(40);check(!Files.exists(cancelled.directory().resolve(d.asset())),"cancelled operation never commits");
            }
        }
        Files.writeString(directory.resolve("catalog.json"),"broken");try(var fallback=new CosmeticsLibrary(directory)){check(fallback.definitions().size()==44,"bad metadata retains bundled collection");}
        var motion=new CosmeticsMotion();motion.tick(new net.minecraft.world.phys.Vec3(0,64,0));for(int i=1;i<190;i++){motion.tick(new net.minecraft.world.phys.Vec3(i*.1,64,0));}check(motion.trail().size()==160,"bounded trail");
        float first=motion.sample(.5F);check(first==motion.sample(.5F),"render does not advance spring");motion.tick(new net.minecraft.world.phys.Vec3(1000,64,0));check(motion.trail().size()==1&&motion.sample(.5F)==0,"teleport resets history");
        var manager=new CosmeticsManager(directory.resolve("manager"));var modules=new ModuleManager(new EventBus(),LoggerFactory.getLogger("CosmeticsChecks"));
        try(manager;var config=new ConfigManager(directory.resolve("config"),modules,()->true,v->{},LoggerFactory.getLogger("CosmeticsChecks"))){
            config.bindCosmeticsStore(manager::snapshot,manager::restore,manager::validate);
            var outfit=empty.with(CosmeticCategory.WINGS,CosmeticLoadout.Item.defaults("wings-dragon"));manager.apply(outfit);var profile=config.profileSnapshot();manager.apply(empty);config.applyProfile(profile);check(manager.outfit().equals(outfit),"profiles restore outfit");
            var old=profile.deepCopy();old.remove("cosmetics");manager.apply(empty);config.applyProfile(old);check(manager.outfit().equals(empty),"older profile preserves current outfit");
            manager.apply(outfit);check(config.withCurrentHudState(old).get("cosmetics")==null,"HUD-only saves do not replace cosmetics");
            var missing=empty.with(CosmeticCategory.HEAD,CosmeticLoadout.Item.defaults("missing-asset"));manager.apply(missing);check(CosmeticLoadout.read(manager.snapshot()).equals(missing),"missing asset references preserved");
            var invalid=profile.deepCopy();invalid.getAsJsonObject("cosmetics").getAsJsonObject("items").getAsJsonObject("WINGS").addProperty("scale",99);
            reject(()->config.applyProfile(invalid),"profile validates cosmetics before commit");check(manager.outfit().equals(missing),"failed profile retains current outfit");
        }
        System.out.println("Cosmetics checks passed ("+assertions+" assertions).");
    }
}
