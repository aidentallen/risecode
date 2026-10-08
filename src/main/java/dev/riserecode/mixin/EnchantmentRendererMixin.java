package dev.riserecode.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import dev.riserecode.Client;
import dev.riserecode.event.EnchantmentRenderEvent;
import dev.riserecode.render.EnchantmentRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFeatureRenderer.class)
public abstract class EnchantmentRendererMixin extends RenderTypeFeatureRenderer<ItemFeatureRenderer.Submit> {
    @Unique private ItemDisplayContext riserecode$context = ItemDisplayContext.NONE;
    @Inject(method = "prepareMainSubmit", at = @At("TAIL"))
    private void riserecode$outline(ItemFeatureRenderer.Submit submit, CallbackInfo callback) {
        if (submit.foilType() == ItemStackRenderState.FoilType.NONE || !Client.INSTANCE.isInitialized()) { return; }
        EnchantmentRenderEvent event = new EnchantmentRenderEvent(submit.displayContext()); Client.INSTANCE.getEventBus().post(event);
        if (event.style() != null) { EnchantmentRenderer.outline(submit, event.style(), this::getVertexBuilder); }
    }
    @Inject(method = "prepareFoilSubmit", at = @At("HEAD"))
    private void riserecode$foilContext(ItemFeatureRenderer.Submit submit, CallbackInfo callback) { riserecode$context = submit.displayContext(); }
    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true)
    private void riserecode$foil(RenderType type, PoseStack.Pose pose, CallbackInfoReturnable<VertexConsumer> callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        EnchantmentRenderEvent event = new EnchantmentRenderEvent(riserecode$context); Client.INSTANCE.getEventBus().post(event);
        if (event.style() != null && event.style().overlay()) {
            VertexConsumer buffer = EnchantmentRenderer.tint(getVertexBuilder(EnchantmentRenderer.glintType(event.style(), type)), event.style().overlayPaint());
            if (pose != null) { buffer = new SheetedDecalTextureGenerator(buffer, pose, 0.0078125F); }
            callback.setReturnValue(buffer);
        }
    }
}
