package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.riserecode.render.placement.PlacementAnimations;
import net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings;
import net.irisshaders.iris.uniforms.CapturedRenderingState;
import net.irisshaders.iris.vertices.BlockSensitiveBufferBuilder;
import net.irisshaders.iris.vertices.ExtendedDataHelper;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Moving models need the same shader-pack block identity as their terrain representation. */
@Mixin(MovingBlockFeatureRenderer.class)
public abstract class IrisPlacementMaterialMixin {
    @Unique private BlockState riserecode$state;
    @WrapOperation(method="buildGroup",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
    private void riserecode$model(ModelBlockRenderer renderer,BlockQuadOutput output,float x,float y,float z,BlockAndTintGetter level,BlockPos pos,BlockState state,BlockStateModel model,long seed,Operation<Void> original){
        BlockState previous=riserecode$state;riserecode$state=PlacementAnimations.isPlacementModel(level)?state:null;
        try{original.call(renderer,output,x,y,z,level,pos,state,model,seed);}finally{riserecode$state=previous;}
    }
    @WrapOperation(method="putBakedQuad",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void riserecode$material(VertexConsumer buffer,PoseStack.Pose pose,BakedQuad quad,QuadInstance instance,Operation<Void> original){
        if(riserecode$state==null){original.call(buffer,pose,quad,instance);return;}
        var ids=WorldRenderingSettings.INSTANCE.getBlockStateIds();int id=ids==null?-1:ids.getInt(riserecode$state);
        var captured=CapturedRenderingState.INSTANCE;int previous=captured.getCurrentRenderedBlockEntity();captured.setCurrentBlockEntity(id);
        var sensitive=buffer instanceof BlockSensitiveBufferBuilder b?b:null;
        if(sensitive!=null)sensitive.beginBlock(id,(byte)ExtendedDataHelper.BLOCK_RENDER_TYPE,(byte)riserecode$state.getLightEmission(),0,0,0);
        try{original.call(buffer,pose,quad,instance);}finally{if(sensitive!=null)sensitive.endBlock();captured.setCurrentBlockEntity(previous);}
    }
}
