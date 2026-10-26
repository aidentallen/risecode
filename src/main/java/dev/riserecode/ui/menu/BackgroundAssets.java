package dev.riserecode.ui.menu;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;

public final class BackgroundAssets {
    private static final java.util.Map<String,Identifier> IMAGES=new java.util.LinkedHashMap<>();
    private static long residentBytes;
    private static final long MAX_BYTES=128L*1024*1024;
    private static final java.util.Set<String> FAILED=new java.util.HashSet<>();
    private BackgroundAssets() { }
    public static Identifier asset(ThemeManager.Background choice) {
        if (choice == ThemeManager.Background.LOCAL_IMAGE) {
            String requested = Theme.manager().text("Background File");
            if(IMAGES.containsKey(requested)){return IMAGES.get(requested);}
            if(!FAILED.contains(requested)&&requested.matches("[a-f0-9]{64}\\.png")){
                try{
                    var path=Theme.manager().getDirectory().resolve("branding").resolve(requested);
                    if(Files.size(path)>16_777_216){throw new java.io.IOException("Oversized background");}
                    var image=NativeImage.read(Files.readAllBytes(path));
                    if(image.getWidth()>4096||image.getHeight()>4096){image.close();throw new java.io.IOException("Oversized background");}
                    // Do not evict a view still referenced by another card's extracted draw.
                    long bytes=(long)image.getWidth()*image.getHeight()*4;
                    if(IMAGES.size()>=128||residentBytes+bytes>MAX_BYTES){image.close();throw new java.io.IOException("Background texture budget exceeded");}
                    var id=Identifier.fromNamespaceAndPath("riserecode","background/"+requested.substring(0,64));
                    Minecraft.getInstance().getTextureManager().register(id,new DynamicTexture(()->"risecode local background",image));IMAGES.put(requested,id);residentBytes+=bytes;return id;
                }catch(java.io.IOException|RuntimeException error){FAILED.add(requested);dev.riserecode.Client.INSTANCE.getLogger().warn("Background unavailable: {}",error.getMessage());}
            }
        }
        String name = switch (choice) { case SCENIC -> "scenic"; case SPACE -> "space"; case ABSTRACT -> "abstract"; default -> "night-lake"; };
        return Identifier.fromNamespaceAndPath("riserecode", "textures/gui/" + name + ".png");
    }
    public static void close() {var manager=Minecraft.getInstance().getTextureManager();IMAGES.values().forEach(manager::release);IMAGES.clear();FAILED.clear();residentBytes=0;}
}
