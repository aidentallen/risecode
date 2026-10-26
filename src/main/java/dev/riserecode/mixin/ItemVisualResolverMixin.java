package dev.riserecode.mixin;

import dev.riserecode.render.ItemVisualContext;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ItemOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ItemVisualResolverMixin {
    @Inject(method="updateForTopItem",at=@At("TAIL"))
    private void risecode$capture(ItemStackRenderState state,ItemStack stack,ItemDisplayContext display,Level level,ItemOwner owner,int seed,CallbackInfo ci){
        ((ItemVisualContext.State)state).risecode$itemContext(new ItemVisualContext(display,stack.isEnchanted(),stack.hasFoil()));
    }
}
