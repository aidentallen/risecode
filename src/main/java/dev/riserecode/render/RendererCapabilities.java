package dev.riserecode.render;

/** Set by the mixin plugin using class bytes, before game classes are initialized. */
public final class RendererCapabilities {
    public static volatile boolean chunkAdapters=true;
    public static boolean supportedVersions(String sodium,String iris){
        return "0.9.2+mc26.2".equals(sodium)&&(iris==null||"1.11.4+mc26.2".equals(iris));
    }
    private RendererCapabilities(){ }
}
