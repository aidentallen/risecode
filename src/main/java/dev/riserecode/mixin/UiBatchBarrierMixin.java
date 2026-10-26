package dev.riserecode.mixin;

import dev.riserecode.ui.render.UiBatch;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native text/items or other mods' GUI elements are explicit ordering barriers. */
@Mixin(GuiRenderState.class)
public abstract class UiBatchBarrierMixin {
    @Inject(method={"addGuiElement","addText","addItem","addPicturesInPictureState","addBlitToCurrentLayer","addGlyphToCurrentLayer"},at=@At("HEAD"))
    private void riserecode$nativeBarrier(CallbackInfo ci){UiBatch.nativeBarrier((GuiRenderState)(Object)this);}
}
