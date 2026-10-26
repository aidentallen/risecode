package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.render.ItemVisualContext;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStackRenderState.class)
public abstract class ItemVisualStateMixin implements ItemVisualContext.State {
    @Unique private ItemVisualContext risecode$context;
    @Override public ItemVisualContext risecode$itemContext(){return risecode$context;}
    @Override public void risecode$itemContext(ItemVisualContext value){risecode$context=value;}
    @WrapMethod(method="submit")
    private void risecode$submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline, Operation<Void> original){
        ItemVisualContext before=ItemVisualContext.CURRENT.get();
        ItemVisualContext.CURRENT.set(risecode$context);
        try{original.call(pose,collector,light,overlay,outline);}finally{
            if(before==null){ItemVisualContext.CURRENT.remove();}else{ItemVisualContext.CURRENT.set(before);}
        }
    }
}
