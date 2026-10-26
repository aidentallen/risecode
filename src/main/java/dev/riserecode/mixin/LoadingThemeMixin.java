package dev.riserecode.mixin;

import dev.riserecode.Client;
import dev.riserecode.ui.menu.MenuBackdrop;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native extraction/tick still owns progress, exceptions, callbacks and overlay removal. */
@Mixin(LoadingOverlay.class)
public abstract class LoadingThemeMixin {
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    private static boolean riserecode$themed() { return Client.INSTANCE.resourcesReady() && Theme.bool("Loading Theme", true); }
    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
    private void riserecode$logo(GuiGraphicsExtractor g, com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
            net.minecraft.resources.Identifier texture, int x, int y, float u, float v, int width, int height,
            int regionWidth, int regionHeight, int textureWidth, int textureHeight, int color) {
        if (!riserecode$themed()) { g.blit(pipeline,texture,x,y,u,v,width,height,regionWidth,regionHeight,textureWidth,textureHeight,color); }
    }
    @Inject(method = "extractProgressBar", at = @At("HEAD"), cancellable = true)
    private void riserecode$nativeBar(GuiGraphicsExtractor g,int x0,int y0,int x1,int y1,float fade,CallbackInfo ci) {
        if (riserecode$themed()) { ci.cancel(); }
    }
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void riserecode$loading(GuiGraphicsExtractor g, int mx, int my, float partial, CallbackInfo ci) {
        if (!riserecode$themed()) { return; }
        double opacity = fadeOutStart < 0 ? 1 : Math.clamp(2 - (Util.getMillis() - fadeOutStart)/1000.0, 0, 1);
        if (opacity <= 0) { return; } g.nextStratum();
        if (Theme.bool("Loading Background", true)) { MenuBackdrop.draw(g, opacity); }
        dev.riserecode.ui.menu.BackgroundParticles.draw(g,opacity);
        var font = net.minecraft.client.Minecraft.getInstance().font; String title = Client.INSTANCE.getName();
        double w = RenderUtil.textWidth(font,title,false)*3, x=(g.guiWidth()-w)/2, y=g.guiHeight()*0.42;
        RenderUtil.accentText(g,font,title,x,y,3,opacity);
        dev.riserecode.ui.theme.BrandingLogo.drawBeside(g,x,y,w,opacity);
        String progress="Loading resources · "+Math.round(currentProgress*100)+"%";
        RenderUtil.text(g,font,progress,(g.guiWidth()-RenderUtil.textWidth(font,progress,false))/2,y+39,1,Theme.opacity(Theme.SECONDARY,opacity));
        double bw=Math.min(300,g.guiWidth()*0.6),by=g.guiHeight()*0.75;
        RenderUtil.drawRoundedRect(g,(g.guiWidth()-bw)/2,by,bw,3,1.5,Theme.opacity(Theme.MUTED,opacity*0.4));
        RenderUtil.drawPaintRect(g,(g.guiWidth()-bw)/2,by,bw*currentProgress,3,1.5,Theme.paint("Primary Accent"),opacity);
    }
}
