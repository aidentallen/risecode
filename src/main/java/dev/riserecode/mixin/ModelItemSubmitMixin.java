package dev.riserecode.mixin;

import dev.riserecode.render.ItemVisualContext;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelFeatureRenderer.Submit.class)
public abstract class ModelItemSubmitMixin implements ItemVisualContext.State {
    @Unique private ItemVisualContext risecode$context;
    @Inject(method="<init>",at=@At("TAIL"))
    private void risecode$capture(CallbackInfo ci){risecode$context=ItemVisualContext.CURRENT.get();}
    @Override public ItemVisualContext risecode$itemContext(){return risecode$context;}
    @Override public void risecode$itemContext(ItemVisualContext value){risecode$context=value;}
}
