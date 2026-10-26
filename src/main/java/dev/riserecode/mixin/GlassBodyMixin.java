package dev.riserecode.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.*;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(LivingEntityRenderer.class)
public abstract class GlassBodyMixin {
    @WrapOperation(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private <S> void risecode$body(SubmitNodeCollector collector,Model<? super S> model,S state,PoseStack poses,RenderType type,int light,int overlay,int tint,TextureAtlasSprite sprite,int outline,CrumblingOverlay crumbling,Operation<Void> original){
        var s=GlassCapture.surface(state,Component.BODY,true,Slot.NONE);
        original.call(s==null?collector:new GlassCollector(collector,s),model,state,poses,type,light,overlay,tint,sprite,outline,crumbling);
    }
}
