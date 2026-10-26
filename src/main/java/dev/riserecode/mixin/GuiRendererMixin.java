package dev.riserecode.mixin;

import dev.riserecode.util.BlurUtil;
import dev.riserecode.render.ItemOutlineRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capture the finished world or panorama before any HUD/menu draw. */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;prepare()V"))
    private void riserecode$blurBeforeGui(CallbackInfo callback) {
        ItemOutlineRenderer.composite(Minecraft.getInstance().gameRenderer.mainRenderTarget());
        dev.riserecode.render.SilhouetteOutlineRenderer.composite(Minecraft.getInstance().gameRenderer.mainRenderTarget());
        BlurUtil.renderBeforeGui(Minecraft.getInstance().gameRenderer.mainRenderTarget());
    }
}
