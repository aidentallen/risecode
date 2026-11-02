package dev.riserecode.mixin;

import dev.riserecode.render.HotbarPrivacy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes only the local avatar empty-handed in F5 and inventory previews. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarItemPrivacyMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void riserecode$emptyLocalAvatar(Avatar avatar, AvatarRenderState state, float delta, CallbackInfo callback) {
        if (!HotbarPrivacy.hideHeldItems() || avatar != Minecraft.getInstance().player) return;
        state.rightHandItemState.clear();
        state.leftHandItemState.clear();
        state.heldOnHead.clear();
        state.rightHandItemStack = ItemStack.EMPTY;
        state.leftHandItemStack = ItemStack.EMPTY;
        state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        state.isUsingItem = false;
    }
}
