package dev.riserecode.resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.flag.FeatureFlagSet;

/** One client-only repository source; no entries are written into Minecraft's options pack list. */
public final class ResourcesPackSource implements RepositorySource {
    public static final String ID="riserecode:resources";
    public static final ResourcesPackSource INSTANCE=new ResourcesPackSource();
    private static volatile Path selected;
    private static boolean initialized;
    private ResourcesPackSource(){}
    public static Path directory(){
        if(!initialized){initialized=true;try{Path root=FabricLoader.getInstance().getConfigDir().resolve("riserecode/resources/26.2");Path pointer=root.resolve("active.json");if(Files.exists(pointer)){String name=ResourceIo.read(pointer).get("generation").getAsString();if(name.matches("[a-f0-9-]{36}"))selected=root.resolve("generations").resolve(name);}}catch(java.io.IOException|RuntimeException ignored){selected=null;}}
        return selected;
    }
    public static void select(Path directory){initialized=true;selected=directory;}
    public static boolean available(){
        Path path=directory();if(path==null || !Files.isRegularFile(path.resolve("pack.mcmeta")))return false;
        try{return complete(path);}catch(java.io.IOException|RuntimeException unavailable){return false;}
    }
    public static boolean matches(ResourcesState state){
        Path path=directory();if(state.selected().isEmpty())return path==null;
        if(path==null || !Files.isRegularFile(path.resolve("pack.mcmeta")))return false;
        try{return ResourceIo.read(path.resolve("resources.json")).getAsJsonObject("state").equals(state.json()) && complete(path);}
        catch(java.io.IOException|RuntimeException failure){return false;}
    }
    private static boolean complete(Path directory) throws java.io.IOException {
        var metadata=ResourceIo.read(directory.resolve("resources.json"));
        if(!metadata.has("files"))return false;
        for(var file:metadata.getAsJsonArray("files"))if(!Files.isRegularFile(ResourceIo.child(directory,file.getAsString())))return false;
        return true;
    }
    @Override public void loadPacks(Consumer<Pack> consumer) {
        Path directory=directory();if(directory==null || !Files.exists(directory.resolve("pack.mcmeta")))return;
        try { if(!complete(directory))return; } catch(java.io.IOException|RuntimeException unavailable){return;}
        var location=new PackLocationInfo(ID,Component.literal("risecode Resources"),PackSource.BUILT_IN,Optional.empty());
        var resources=new Pack.ResourcesSupplier(){
            @Override public PackResources openPrimary(PackLocationInfo info){return new PathPackResources(info,directory);}
            @Override public PackResources openFull(PackLocationInfo info,Pack.Metadata metadata){return new PathPackResources(info,directory);}
        };
        consumer.accept(new Pack(location,resources,new Pack.Metadata(Component.literal("Vanilla Tweaks selections"),PackCompatibility.COMPATIBLE,FeatureFlagSet.of(),List.of()),new PackSelectionConfig(true,Pack.Position.TOP,true)));
    }
}
