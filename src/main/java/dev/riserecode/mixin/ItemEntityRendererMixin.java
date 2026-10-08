package dev.riserecode.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.Client;
import dev.riserecode.event.ItemRenderEvent;
import dev.riserecode.render.ItemPhysicsState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V", at = @At("RETURN"))
    private void riserecode$physicsState(ItemEntity entity, ItemEntityRenderState state, float partial, CallbackInfo callback) {
        long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        float angle = (float) ((seed & 0xFFFF) / 65536.0 * Math.PI * 2);
        ((ItemPhysicsState) state).riserecode$physics(entity.onGround(), angle);
    }
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void riserecode$physicsSubmit(ItemEntityRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo callback) {
        if (!Client.INSTANCE.isInitialized()) { return; }
        ItemRenderEvent event = new ItemRenderEvent(state, poses, collector); Client.INSTANCE.getEventBus().post(event);
        if (event.isCancelled()) { callback.cancel(); }
    }
}
