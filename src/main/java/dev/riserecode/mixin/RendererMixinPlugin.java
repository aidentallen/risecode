package dev.riserecode.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Sodium overwrites prepareChunkRenders; its own adapter replaces the vanilla hook. */
public final class RendererMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        boolean sodium = FabricLoader.getInstance().isModLoaded("sodium");
        if (mixin.endsWith(".ChunkTerrainMixin")) { return !sodium; }
        if (mixin.endsWith(".SodiumExtraShaderMixin")) { return FabricLoader.getInstance().isModLoaded("sodium-extra"); }
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Sodium")) { return sodium; }
        if (mixin.substring(mixin.lastIndexOf('.') + 1).startsWith("Iris")) { return FabricLoader.getInstance().isModLoaded("iris"); }
        return true;
    }
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
