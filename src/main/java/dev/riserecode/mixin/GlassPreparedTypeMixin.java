package dev.riserecode.mixin;
import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import dev.riserecode.render.glass.GlassRenderer;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PreparedRenderType.class)
public abstract class GlassPreparedTypeMixin {
    @Inject(method="drawFromBuffer(Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/buffers/GpuBuffer;Lcom/mojang/blaze3d/IndexType;III)V",at=@At("HEAD"),cancellable=true)
    private void risecode$draw(GpuBuffer vertices,GpuBuffer indices,IndexType type,int base,int first,int count,CallbackInfo ci){
        var prepared=(PreparedRenderType)(Object)this;
        if(prepared.outputTarget() instanceof GlassRenderer.Target){GlassRenderer.draw(prepared,vertices,indices,type,base,first,count);ci.cancel();}
    }
}
