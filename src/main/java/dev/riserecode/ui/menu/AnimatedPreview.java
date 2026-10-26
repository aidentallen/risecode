package dev.riserecode.ui.menu;

import dev.riserecode.ui.theme.*;
import dev.riserecode.ui.render.UiMesh;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

/** Same procedural scenes as the full backdrop, directly rendered into gallery/map rectangles. */
public final class AnimatedPreview {
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET).withLocation(UiMesh.id("pipeline/animated_preview")).withVertexShader(UiMesh.id("core/ui")).withFragmentShader(UiMesh.id("core/animated_preview")).withVertexBinding(0,UiMesh.FORMAT).withPrimitiveTopology(PrimitiveTopology.QUADS).build());
    private AnimatedPreview(){}
    public static boolean animated(ThemeManager.Background kind){return kind==ThemeManager.Background.ANIMATED_GRADIENT||kind==ThemeManager.Background.AURORA||kind==ThemeManager.Background.STARFIELD;}
    public static void draw(GuiGraphicsExtractor g,ThemeManager.Background kind,double x,double y,double w,double h,int tint,boolean circle){drawMasked(g,kind,x,y,w,h,tint,circle?1:0);}
    public static void drawRounded(GuiGraphicsExtractor g,ThemeManager.Background kind,double x,double y,double w,double h,int tint,double radius){drawMasked(g,kind,x,y,w,h,tint,2+(int)Math.round(Math.clamp(radius/Math.max(1,Math.min(w,h)),0,.5)*32765));}
    private static void drawMasked(GuiGraphicsExtractor g,ThemeManager.Background kind,double x,double y,double w,double h,int tint,int mask){
        double time=Theme.bool("Reduced Motion",false)?0:Theme.gradientTime()*Theme.number("Background Motion Speed",1);
        int seconds=(int)(time%1024),fraction=(int)((time-Math.floor(time))*32000),scene=kind==ThemeManager.Background.AURORA?1:kind==ThemeManager.Background.STARFIELD?2:0;
        UiMesh.quad(g,PIPELINE,TextureSetup.noTexture(),x,y,w,h,0,0,1,1,tint,tint,seconds,fraction,scene,mask);
    }
}
