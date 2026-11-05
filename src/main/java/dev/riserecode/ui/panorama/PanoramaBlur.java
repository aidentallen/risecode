package dev.riserecode.ui.panorama;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.render.LiquidGlass;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

final class PanoramaBlur {
    private static final class Pipeline {static final com.mojang.blaze3d.pipeline.RenderPipeline BOX=UiMesh.pipeline("panorama_button_blur","blur_box");}
    static void draw(GuiGraphicsExtractor g,GpuTextureView view,double x,double y,double w,double h,double corner,double opacity){
        LiquidGlass.quad(g,LiquidGlass.backdrop(Pipeline.BOX),TextureSetup.singleTexture(view,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),x,y,w,h,corner,Theme.opacity(-1,opacity),LiquidGlass.backdropParameters());
        dev.riserecode.util.RenderUtil.drawRoundedRect(g,x,y,w,h,corner,Theme.opacity(0xFF09131C,opacity*.23));
    }
    private PanoramaBlur(){}
}
