package dev.riserecode.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.ui.menu.MenuTheme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
public abstract class MenuEditBoxMixin {
    @Redirect(method = "extractWidgetRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void riserecode$field(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
        if (MenuTheme.enabled()) { RenderUtil.drawSurfaceTransition(graphics, x, y, width, height, 8, ((EditBox) (Object) this).isFocused() ? 1 : 0, 1); }
        else { graphics.blitSprite(pipeline, sprite, x, y, width, height); }
    }
}
