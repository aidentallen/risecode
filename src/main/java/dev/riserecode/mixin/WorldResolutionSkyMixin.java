package dev.riserecode.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** SkyRenderer retains its constructor target across frames and resource reloads. Resolve per draw. */
@Mixin(SkyRenderer.class)
public abstract class WorldResolutionSkyMixin {
    @ModifyExpressionValue(method="*",at=@At(value="FIELD",target="Lnet/minecraft/client/renderer/SkyRenderer;renderTarget:Lcom/mojang/blaze3d/pipeline/RenderTarget;",opcode=org.objectweb.asm.Opcodes.GETFIELD))
    private RenderTarget riserecode$frameTarget(RenderTarget ignored){return Minecraft.getInstance().gameRenderer.mainRenderTarget();}
}
