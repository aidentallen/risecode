package dev.riserecode.mixin;
import dev.riserecode.Client;
import dev.riserecode.event.LabelRenderEvent;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityRenderer.class)
public abstract class LabelRenderMixin {
    @Inject(method="extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",at=@At("HEAD"),cancellable=true)
    private void risecode$label(Entity entity,EntityRenderState state,float partial,double a,double b,CallbackInfo ci){
        if(Client.INSTANCE.isInitialized()&&Client.INSTANCE.getEventBus().post(new LabelRenderEvent(entity)).isCancelled()){state.nameTag=null;state.scoreText=null;ci.cancel();}
    }
}
