package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.render.placement.PlacementAnimations;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets="betterblockentities.render.AltRenderDispatcher",remap=false)
public abstract class BbePlacementSubmitMixin {
    @WrapMethod(method="submit")
    private <S extends BlockEntityRenderState> void riserecode$pose(S state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,Operation<Void> original){
        pose.pushPose();
        try { PlacementAnimations.blockEntity(state.blockPos,pose); original.call(state,pose,collector,camera); }
        finally { pose.popPose(); }
    }
}
