package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.riserecode.render.WorldResolution;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.TextureFilteringMethod;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The world owns a scaled target only between renderLevel and the final world post effects. */
@Mixin(GameRenderer.class)
public abstract class WorldResolutionMixin {
    @Shadow @Final @Mutable private RenderTarget mainRenderTarget;
    @Shadow @Final private GlobalSettingsUniform globalSettingsUniform;
    @Shadow @Final private GameRenderState gameRenderState;
    @Shadow @Final private Minecraft minecraft;
    @Unique private RenderTarget riserecode$output;
    @Unique private void riserecode$globals(DeltaTracker delta,int width,int height){
        var options=gameRenderState.optionsRenderState;
        globalSettingsUniform.update(width,height,options.glintStrength,minecraft.level==null?0:minecraft.level.getGameTime(),delta,
                options.menuBackgroundBlurriness,gameRenderState.levelRenderState.cameraRenderState.pos,options.textureFiltering==TextureFilteringMethod.RGSS);
    }
    @WrapMethod(method="render")
    private void riserecode$scope(DeltaTracker delta,boolean advance,Operation<Void> original){
        var color=RenderSystem.outputColorTextureOverride;var depth=RenderSystem.outputDepthTextureOverride;
        try{original.call(delta,advance);}
        finally{
            if(riserecode$output!=null){mainRenderTarget=riserecode$output;riserecode$output=null;riserecode$globals(delta,mainRenderTarget.width,mainRenderTarget.height);}
            WorldResolution.abort();RenderSystem.outputColorTextureOverride=color;RenderSystem.outputDepthTextureOverride=depth;
        }
    }
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"))
    private void riserecode$world(DeltaTracker delta,boolean advance,CallbackInfo ci){
        var output=mainRenderTarget;var world=WorldResolution.begin(output);
        if(world!=output){riserecode$output=output;mainRenderTarget=world;riserecode$globals(delta,world.width,world.height);}
    }
    @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
    private void riserecode$present(DeltaTracker delta,boolean advance,CallbackInfo ci){
        WorldResolution.compositeOverlays(mainRenderTarget);
        if(riserecode$output!=null){
            var output=riserecode$output;
            try{WorldResolution.finish(output);}
            finally{mainRenderTarget=output;riserecode$output=null;riserecode$globals(delta,output.width,output.height);}
        }
    }
    @Inject(method={"resize","close"},at=@At("HEAD"))
    private void riserecode$release(CallbackInfo ci){WorldResolution.close();}
}
