package dev.riserecode.testmixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Avoid resolving the optional Iris test probe on vanilla or Vulkan-only fixtures. */
public final class ReviewMixinPlugin implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target,String mixin){if(mixin.endsWith("UxPreviewProbeMixin")){return Boolean.getBoolean("riserecode.uxRefinementOnly")||Boolean.getBoolean("riserecode.uxJourneyOnly");}return !mixin.endsWith("ReviewShadowMixin")||FabricLoader.getInstance().isModLoaded("iris");}
    @Override public void onLoad(String name){ }
    @Override public String getRefMapperConfig(){return null;}
    @Override public void acceptTargets(Set<String> mine,Set<String> others){ }
    @Override public List<String> getMixins(){return null;}
    @Override public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){ }
    @Override public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){ }
}
