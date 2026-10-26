package dev.riserecode.render.glass;

import net.fabricmc.loader.api.FabricLoader;

/** Pinned to the hand submission and buffer lifetime contract inspected in Iris 1.11.4.
 * Both hand paths retain the exact projection captured with their native draw. */
public final class GlassIrisAdapter {
    private GlassIrisAdapter() { }
    public static boolean supported(){
        var iris=FabricLoader.getInstance().getModContainer("iris");
        return iris.isEmpty()||iris.orElseThrow().getMetadata().getVersion().getFriendlyString().equals("1.11.4+mc26.2");
    }
    public static boolean active(){return dev.riserecode.render.ShaderWorldOverlay.active();}
}
