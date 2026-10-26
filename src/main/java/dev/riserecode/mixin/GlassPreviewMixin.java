package dev.riserecode.mixin;
import dev.riserecode.render.glass.GlassPreview;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PictureInPictureRenderer.class)
public abstract class GlassPreviewMixin {
    @Inject(method="prepare",at=@At("HEAD"),cancellable=true)
    private void risecode$preview(PictureInPictureRenderState state,GuiRenderState gui,FeatureRenderDispatcher dispatcher,int scale,CallbackInfo ci){
        if(state instanceof GuiEntityRenderState entity&&GlassPreview.isPreview(entity)){
            try{GlassPreview.render(entity,gui,dispatcher,scale);ci.cancel();}
            catch(RuntimeException failure){GlassPreview.close();dev.riserecode.render.glass.GlassRenderer.previewFailure(failure);}
        }
    }
}
