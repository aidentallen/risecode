package dev.riserecode.testmixin;

import dev.riserecode.LifetimeFaults;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemLifetimeProbeMixin {
    @Inject(method={"renderItem","renderMap"},at=@At("HEAD"),cancellable=true)
    private void risecode$fault(CallbackInfo ci){if(LifetimeFaults.mode==1){ci.cancel();}else if(LifetimeFaults.mode==2){throw LifetimeFaults.FAILURE;}}
}
