package dev.riserecode.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.render.glass.*;
import dev.riserecode.render.glass.GlassPolicy.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(WingsLayer.class)
public abstract class GlassWingsLayerMixin {
    @WrapMethod(method="submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V")
    private void risecode$equipment(PoseStack poses,SubmitNodeCollector collector,int light,HumanoidRenderState state,float yaw,float pitch,Operation<Void> original){
        var s=GlassCapture.surface(state,Component.ARMOR,true,Slot.CHEST);
        original.call(poses,s==null?collector:new GlassCollector(collector,s),light,state,yaw,pitch);
    }
}
