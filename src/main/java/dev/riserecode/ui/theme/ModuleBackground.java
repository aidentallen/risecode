package dev.riserecode.ui.theme;

import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.menu.AnimatedPreview;
import dev.riserecode.ui.menu.BackgroundAssets;
import dev.riserecode.ui.menu.BackgroundParticles;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.util.ColorUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

/** Immutable per-card draws: no shared off-screen image target can be overwritten by another module. */
public final class ModuleBackground {
    private static final class Pipelines {private static final com.mojang.blaze3d.pipeline.RenderPipeline IMAGE=UiMesh.pipeline("module_background","module_background");}
    private ModuleBackground(){}
    public static void draw(GuiGraphicsExtractor g,double x,double y,double width,double height,double alpha){
        var manager=Theme.manager();if(manager==null||width<=0||height<=0){return;}
        double particleAlpha=alpha;
        alpha*=Theme.number("Background Opacity",1)*Theme.number("Panel Opacity",.87);
        var kind=manager.choice("Background",ThemeManager.Background.class);double radius=Theme.radius(5);
        RenderUtil.scissorPush(g,x,y,width,height);
        try{
            if(kind==ThemeManager.Background.NONE){}
            else if(kind==ThemeManager.Background.SOLID){RenderUtil.drawPaintRect(g,x,y,width,height,5,Theme.paint("Background Color"),alpha);}
            else if(kind==ThemeManager.Background.GRADIENT){RenderUtil.drawPaintRect(g,x,y,width,height,5,new ColorSetting.Paint(manager.color("Gradient Start"),manager.color("Gradient End"),ColorSetting.Mode.GRADIENT,ColorUtil.GradientDirection.VERTICAL,0,height),alpha);}
            else if(AnimatedPreview.animated(kind)){AnimatedPreview.drawRounded(g,kind,x,y,width,height,Theme.opacity(manager.color("Background Tint"),alpha),radius);}
            else{
                var texture=Minecraft.getInstance().getTextureManager().getTexture(BackgroundAssets.asset(kind));
                var setup=TextureSetup.singleTexture(texture.getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                int fit=manager.choice("Image Fit",ThemeManager.ImageFit.class).ordinal(),motion=Theme.bool("Reduced Motion",false)?0:manager.choice("Image Motion",ThemeManager.ImageMotion.class).ordinal();
                int blur=dev.riserecode.performance.PerformancePolicy.frame().blur()?0:Math.clamp(Math.round(Theme.number("GUI Blur Radius",12)*Theme.number("GUI Blur Opacity",.8)),0,128);
                int time=(int)(Theme.gradientTime()*Theme.number("Background Motion Speed",1)*.07/(Math.PI*2)*65536)%65536;if(time>32767){time-=65536;}
                int color=Theme.opacity(manager.color("Background Tint"),alpha);
                UiMesh.quad(g,Pipelines.IMAGE,setup,x,y,width,height,(float)-width/2,(float)-height/2,(float)width/2,(float)height/2,color,color,(int)Math.min(4095,Math.round(width))+fit*4096+motion*16384,(int)Math.min(16383,Math.round(height)),(int)Math.min(127,Math.round(Math.min(radius,Math.min(width,height)/2)))+blur*128,time);
            }
            if(kind!=ThemeManager.Background.NONE){RenderUtil.drawPaintRect(g,x,y,width,height,5,Theme.paint("Background Color"),alpha*Theme.number("Background Darkness",.42));}
            BackgroundParticles.draw(g,particleAlpha,x,y,width,height);
        }finally{RenderUtil.scissorPop(g);}
    }
}
