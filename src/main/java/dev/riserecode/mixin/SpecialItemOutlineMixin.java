package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.event.EnchantmentRenderEvent;
import dev.riserecode.render.ItemVisualContext;
import dev.riserecode.render.ItemOutlineRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelFeatureRenderer.class)
public abstract class SpecialItemOutlineMixin extends RenderTypeFeatureRenderer<ModelFeatureRenderer.Submit<?>> {
    @org.spongepowered.asm.mixin.Unique private EnchantmentRenderEvent.Style risecode$glint;
    @Inject(method="prepareModel",at=@At("HEAD"))
    private <S> void risecode$policy(ModelFeatureRenderer.Submit<S> submit,CallbackInfo ci){
        risecode$glint=null;var context=((ItemVisualContext.State)(Object)submit).risecode$itemContext();
        if(context!=null&&context.foil()&&Client.INSTANCE.isInitialized()){risecode$glint=Client.INSTANCE.getEventBus().post(new EnchantmentRenderEvent(context.display())).style();}
    }
    @Redirect(method="prepareModel",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private com.mojang.blaze3d.vertex.VertexConsumer risecode$glint(ModelFeatureRenderer renderer,net.minecraft.client.renderer.rendertype.RenderType type){
        if(risecode$glint!=null&&risecode$glint.overlay()&&type.toString().contains("glint")){return dev.riserecode.render.EnchantmentRenderer.tint(getVertexBuilder(dev.riserecode.render.EnchantmentRenderer.glintType(risecode$glint,type)),risecode$glint.overlayPaint());}
        return getVertexBuilder(type);
    }
    @Inject(method="prepareModel",at=@At("TAIL"))
    private <S> void risecode$mask(ModelFeatureRenderer.Submit<S> submit,CallbackInfo ci){
        ItemVisualContext context=((ItemVisualContext.State)(Object)submit).risecode$itemContext();
        if(context==null||(!context.foil()&&!context.enchanted())||!Client.INSTANCE.isInitialized()||submit.renderType().toString().contains("glint")){return;}
        var event=Client.INSTANCE.getEventBus().post(new EnchantmentRenderEvent(context.display()));
        if(event.style()!=null){ItemOutlineRenderer.submitModel(submit,context,event.style(),this::getVertexBuilder);}
    }
}
