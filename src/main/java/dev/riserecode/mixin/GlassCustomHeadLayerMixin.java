package dev.riserecode.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(CustomHeadLayer.class)
public abstract class GlassCustomHeadLayerMixin {
    @WrapMethod(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V")
    private void risecode$equipment(PoseStack poses,SubmitNodeCollector collector,int light,LivingEntityRenderState state,float yaw,float pitch,Operation<Void> original){
        var s=GlassCapture.surface(state,Component.ARMOR,true,Slot.HEAD);
        original.call(poses,s==null?collector:new GlassCollector(collector,s),light,state,yaw,pitch);
    }
}
