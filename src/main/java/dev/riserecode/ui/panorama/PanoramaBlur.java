package dev.riserecode.ui.panorama;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

final class PanoramaBlur {
    private static final class Pipeline {static final com.mojang.blaze3d.pipeline.RenderPipeline BOX=UiMesh.pipeline("panorama_button_blur","blur_box");}
    static void draw(GuiGraphicsExtractor g,GpuTextureView view,double x,double y,double w,double h,double corner,double opacity){
        UiMesh.quad(g,Pipeline.BOX,TextureSetup.singleTexture(view,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),x-1,y-1,w+2,h+2,(float)(-w/2-1),(float)(-h/2-1),(float)(w/2+1),(float)(h/2+1),Theme.opacity(-1,opacity),Theme.opacity(-1,opacity),(int)Math.round(w*16),(int)Math.round(h*16),(int)Math.round(Theme.radius(corner)*256),0);
        dev.riserecode.util.RenderUtil.drawRoundedRect(g,x,y,w,h,corner,Theme.opacity(0xFF09131C,opacity*.23));
    }
    private PanoramaBlur(){}
}
