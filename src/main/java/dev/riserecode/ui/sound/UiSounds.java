package dev.riserecode.ui.sound;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.flag.FeatureFlagSet;

/** One dispatch path shared by native controls and risecode controls. */
public final class UiSounds implements RepositorySource {
    public static final UiSounds INSTANCE = new UiSounds();
    private static boolean nativeMinecraftia,nativeQraftys;
    public static boolean nativeQraftys(){return nativeQraftys;}
    public static boolean nativeMinecraftia(){return nativeMinecraftia;}
    private UiSounds() { }
    private static void prepareFonts(Path root){
        Path config=FabricLoader.getInstance().getConfigDir().resolve("riserecode");
        try{
            nativeQraftys=dev.riserecode.ui.font.CustomTextRenderer.prepareNativeBitmap(config,root);
            if(nativeQraftys&&!Files.exists(root.resolve("pack.mcmeta"))){Files.writeString(root.resolve("pack.mcmeta"),"{\"pack\":{\"description\":\"Local interface assets\",\"min_format\":[88,0],\"max_format\":[88,0]}}");}
        }catch(IOException error){dev.riserecode.Client.INSTANCE.getLogger().warn("Cannot prepare local bitmap label font",error);}
        Path fonts=config.resolve("fonts");
        for(String filename:List.of("minecraftia.ttf","Minecraftia.ttf","Minecraftia-Regular.ttf")){
            Path source=fonts.resolve(filename);if(!Files.isRegularFile(source)){continue;}
            try{
                if(Files.size(source)>16_777_216){return;}
                Path destination=root.resolve("assets/riserecode/font");Files.createDirectories(destination);
                Files.copy(source,destination.resolve("minecraftia.ttf"),StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(destination.resolve("minecraftia.json"),"{\"providers\":[{\"type\":\"ttf\",\"file\":\"riserecode:minecraftia.ttf\",\"size\":18,\"oversample\":2},{\"type\":\"reference\",\"id\":\"minecraft:default\"}]}");
                if(!Files.exists(root.resolve("pack.mcmeta"))){Files.writeString(root.resolve("pack.mcmeta"),"{\"pack\":{\"description\":\"Local interface assets\",\"min_format\":[88,0],\"max_format\":[88,0]}}");}
                nativeMinecraftia=true;return;
            }catch(IOException error){dev.riserecode.Client.INSTANCE.getLogger().warn("Cannot prepare local world-label font",error);return;}
        }
    }
    private static boolean dispatching;
    private static long dispatches;
    public static boolean dispatching(){return dispatching;}
    public static long dispatches(){return dispatches;}
    public static net.minecraft.client.sounds.SoundEngine.PlayResult play() {
        if(dev.riserecode.Client.INSTANCE.isInitialized()&&!dev.riserecode.Client.INSTANCE.getClientPreferences().sounds.getValue()){return net.minecraft.client.sounds.SoundEngine.PlayResult.NOT_STARTED;}
        dispatches++;
        if (Theme.manager() == null) { return net.minecraft.client.sounds.SoundEngine.PlayResult.NOT_STARTED; }
        var kind = Theme.manager().choice("Click Sound", ThemeManager.ClickSound.class);
        if (kind == ThemeManager.ClickSound.MUTED) { return net.minecraft.client.sounds.SoundEngine.PlayResult.NOT_STARTED; }
        String custom=Theme.manager().text("Sound File");
        SoundEvent sound = kind==ThemeManager.ClickSound.LOCAL_OGG&&custom.matches("[a-f0-9]{64}\\.ogg")?SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("riserecode","ui.import_"+custom.substring(0,64))):kind == ThemeManager.ClickSound.VANILLA ? SoundEvents.UI_BUTTON_CLICK.value()
                : SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("riserecode", "ui." + kind.name().toLowerCase(java.util.Locale.ROOT)));
        dispatching=true;
        try { return Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,
                (float)Theme.number("Click Pitch", 1), (float)Theme.number("Click Volume", 0.35))); } finally { dispatching=false; }
    }
    private static Path directory() { return FabricLoader.getInstance().getConfigDir().resolve("riserecode/ui-sounds"); }
    public static void importOgg(ThemeManager manager) throws IOException {importOgg(manager,manager::getSetting);}
    public static void importOgg(ThemeManager manager,java.util.function.Function<String,dev.riserecode.setting.Setting<?>> settings) throws IOException {
        Path source = Path.of(((dev.riserecode.setting.StringSetting)settings.apply("Sound File")).getValue());
        if (!Files.isRegularFile(source) || Files.size(source) > 4 * 1024 * 1024) { throw new IOException("Choose an OGG smaller than 4 MiB"); }
        byte[] data = Files.readAllBytes(source);
        if (data.length < 32 || data[0] != 'O' || data[1] != 'g' || data[2] != 'g' || data[3] != 'S') { throw new IOException("Expected an OGG audio file"); }
        String hash;try{hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(data));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
        Path root = directory(), folder = root.resolve("assets/riserecode/sounds/ui"); Files.createDirectories(folder);
        Path temporary = Files.createTempFile(folder, "sound-", ".tmp");
        try { Files.write(temporary, data); Files.move(temporary, folder.resolve(hash+".ogg"), StandardCopyOption.REPLACE_EXISTING); }
        finally { Files.deleteIfExists(temporary); }
        Path catalog=root.resolve("assets/riserecode/sounds.json");
        com.google.gson.JsonObject entries=Files.isRegularFile(catalog)?com.google.gson.JsonParser.parseString(Files.readString(catalog)).getAsJsonObject():new com.google.gson.JsonObject();
        entries.add("ui.import_"+hash,com.google.gson.JsonParser.parseString("{\"sounds\":[\"riserecode:ui/"+hash+"\"]}"));
        Files.writeString(catalog,entries.toString());
        ((dev.riserecode.setting.StringSetting)settings.apply("Sound File")).setValue(hash+".ogg");
        Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"Local interface sound\",\"min_format\":[88,0],\"max_format\":[88,0]}}");
        @SuppressWarnings("unchecked") var selection = (dev.riserecode.setting.EnumSetting<ThemeManager.ClickSound>)settings.apply("Click Sound");
        selection.setValue(ThemeManager.ClickSound.LOCAL_OGG); manager.changed(); Minecraft.getInstance().reloadResourcePacks();
    }
    @Override public void loadPacks(Consumer<Pack> consumer) {
        Path root = directory(); prepareFonts(root); if (!Files.isRegularFile(root.resolve("pack.mcmeta"))) { return; }
        var location = new PackLocationInfo("riserecode:ui-sounds", Component.literal("Local interface sound"), PackSource.BUILT_IN, Optional.empty());
        var supplier = new Pack.ResourcesSupplier() {
            @Override public PackResources openPrimary(PackLocationInfo info) { return new PathPackResources(info, root); }
            @Override public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) { return new PathPackResources(info, root); }
        };
        consumer.accept(new Pack(location, supplier, new Pack.Metadata(Component.literal("Local interface sound"), PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of()), new PackSelectionConfig(true, Pack.Position.TOP, true)));
    }
}
