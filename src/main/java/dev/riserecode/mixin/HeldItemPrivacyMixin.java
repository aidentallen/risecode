package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.riserecode.render.HotbarPrivacy;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Changes renderer inputs only; the inventory and item-use packets retain the real items. */
@Mixin(ItemInHandRenderer.class)
public abstract class HeldItemPrivacyMixin {
    @ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true)
    private ItemStack riserecode$emptyHand(ItemStack original) {
        return HotbarPrivacy.hideHeldItems() ? ItemStack.EMPTY : original;
    }

    @ModifyExpressionValue(method = "submitHandsWithItems", at = @At(value = "FIELD", target =
            "Lnet/minecraft/client/renderer/ItemInHandRenderer$HandRenderSelection;renderMainHand:Z"))
    private boolean riserecode$showFist(boolean original) {
        return HotbarPrivacy.hideHeldItems() || original;
    }

    @ModifyExpressionValue(method = "submitHandsWithItems", at = @At(value = "FIELD", target =
            "Lnet/minecraft/client/renderer/ItemInHandRenderer$HandRenderSelection;renderOffHand:Z"))
    private boolean riserecode$hideOffhand(boolean original) {
        return !HotbarPrivacy.hideHeldItems() && original;
    }
}
