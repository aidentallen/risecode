package dev.riserecode.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.riserecode.render.glass.*;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ItemFeatureRenderer.class)
public abstract class GlassItemFeatureMixin {
    @WrapOperation(method="prepareMainSubmit",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType risecode$type(BakedQuad.MaterialInfo material,Operation<RenderType> original,@Local(argsOnly=true)ItemFeatureRenderer.Submit submit){
        return GlassRenderer.type(original.call(material),((GlassCapture.ItemState)(Object)submit).risecode$glassSurface());
    }
}
