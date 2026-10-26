package dev.riserecode.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.riserecode.render.placement.PlacementAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class PlacementBlockEntityMixin {
    @WrapMethod(method="submit")
    private <S extends BlockEntityRenderState> void riserecode$pose(S state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,Operation<Void> original){
        pose.pushPose();
        try { PlacementAnimations.blockEntity(state.blockPos,pose); original.call(state,pose,collector,camera); }
        finally { pose.popPose(); }
    }
}
