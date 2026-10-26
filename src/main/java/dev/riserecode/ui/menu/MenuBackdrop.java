package dev.riserecode.ui.menu;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;

/** Static photo blur cached at half resolution. The source is always the photo, never the world. */
public final class MenuBackdrop {
    private static final Identifier PHOTO = UiMesh.id("textures/gui/night-lake.png");
    private static TextureTarget scene, blurred;
    private static GpuTextureView source;
    private static double radius = -1;
    private static final GpuBuffer[] PARAMS = new GpuBuffer[3];
    private static long builds;
    private static long lastAnimationFrame;
    private static String cachedPolicy="";
    private static final class Pipelines {
        private static final RenderPipeline ANIMATED = post("animated_scene", "animated_scene");
        private static final RenderPipeline PHOTO = post("menu_photo_copy", "menu_photo_copy");
        private static final RenderPipeline BLUR = post("menu_photo_blur", "blur");
        private static final RenderPipeline DRAW = UiMesh.pipeline("menu_photo", "menu_photo");
        private static final RenderPipeline BOX = UiMesh.pipeline("menu_photo_box", "blur_box");
    }
    private MenuBackdrop() { }
    private static RenderPipeline post(String name, String shader) {
        var builder=RenderPipeline.builder().withLocation(UiMesh.id("pipeline/"+name)).withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/"+shader));
        if(!shader.equals("animated_scene")){builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);}
        return RenderPipelines.register(builder.withBindGroupLayout(BindGroupLayout.builder().withUniform("BlurParams",UniformType.UNIFORM_BUFFER).build()).withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
    }
    private static void ensure() {
        Minecraft mc = Minecraft.getInstance(); var texture = mc.getTextureManager().getTexture(BackgroundAssets.asset(Theme.manager().choice("Background", dev.riserecode.ui.theme.ThemeManager.Background.class)));
        int width = Math.max(1, (mc.getWindow().getScreenWidth() + 1) / 2), height = Math.max(1, (mc.getWindow().getScreenHeight() + 1) / 2);
        if (scene == null || scene.width != width || scene.height != height) {
            destroy(); scene = new TextureTarget("risecode menu photo", width, height, false, GpuFormat.RGBA8_UNORM);
            blurred = new TextureTarget("risecode photo blur", width, height, false, GpuFormat.RGBA8_UNORM); radius = -1;
        }
        double next = Theme.number("GUI Blur Radius", 12);
        var kind=Theme.manager().choice("Background",dev.riserecode.ui.theme.ThemeManager.Background.class);
        boolean animated=kind==dev.riserecode.ui.theme.ThemeManager.Background.ANIMATED_GRADIENT||kind==dev.riserecode.ui.theme.ThemeManager.Background.AURORA||kind==dev.riserecode.ui.theme.ThemeManager.Background.STARFIELD;
        var motion=Theme.manager().choice("Image Motion",dev.riserecode.ui.theme.ThemeManager.ImageMotion.class);
        boolean moving=motion!=dev.riserecode.ui.theme.ThemeManager.ImageMotion.NONE&&!Theme.bool("Reduced Motion",false);
        long frame=Double.doubleToLongBits(Theme.gradientTime());
        String policy=dev.riserecode.ui.theme.PageAppearance.context()+":"+Theme.manager().getRevision();
        if(source==texture.getTextureView()&&radius==next&&cachedPolicy.equals(policy)&&(!(animated||moving)||Theme.bool("Reduced Motion",false)||lastAnimationFrame==frame)){return;}
        lastAnimationFrame=frame;cachedPolicy=policy;
        source = texture.getTextureView(); radius = next;
        double photoAspect = (double) texture.getTexture().getWidth(0) / texture.getTexture().getHeight(0), screenAspect = (double) width / height;
        float sx = (float) Math.min(1, screenAspect / photoAspect), sy = (float) Math.min(1, photoAspect / screenAspect);
        var fit=Theme.manager().choice("Image Fit",dev.riserecode.ui.theme.ThemeManager.ImageFit.class);
        if(fit==dev.riserecode.ui.theme.ThemeManager.ImageFit.STRETCH){sx=sy=1;}
        else if(fit==dev.riserecode.ui.theme.ThemeManager.ImageFit.CONTAIN){sx=(float)Math.max(1,screenAspect/photoAspect);sy=(float)Math.max(1,photoAspect/screenAspect);}
        double time=Theme.bool("Reduced Motion",false)?0:Theme.gradientTime()*Theme.number("Background Motion Speed",1);
        float ox=(1-sx)/2,oy=(1-sy)/2;
        if(moving){float zoom=motion==dev.riserecode.ui.theme.ThemeManager.ImageMotion.PAN?1.1f:(float)(1.08+.06*Math.sin(time*.09));sx/=zoom;sy/=zoom;ox=(1-sx)/2;oy=(1-sy)/2;if(motion!=dev.riserecode.ui.theme.ThemeManager.ImageMotion.ZOOM){ox+=(float)((1-sx)*.35*Math.sin(time*.07));oy+=(float)((1-sy)*.35*Math.cos(time*.05));}}
        if(animated){pass(source,scene,Pipelines.ANIMATED,0,(float)time,kind==dev.riserecode.ui.theme.ThemeManager.Background.AURORA?1:kind==dev.riserecode.ui.theme.ThemeManager.Background.STARFIELD?2:0,0,0);}
        else {pass(source, scene, Pipelines.PHOTO, 0, sx, sy, ox, oy);}
        if(next>0){dev.riserecode.util.DualBlur.apply(scene,blurred,next);}
        else{pass(scene.getColorTextureView(),blurred,Pipelines.BLUR,1,0,0,2,1);}builds++;
    }
    private static void pass(GpuTextureView source, RenderTarget target, RenderPipeline pipeline, int slot, float x, float y, float z, float w) {
        if (PARAMS[slot] == null) { PARAMS[slot] = RenderSystem.getDevice().createBuffer(() -> "risecode photo parameters", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16); }
        try (MemoryStack stack = MemoryStack.stackPush()) { RenderSystem.getDevice().createCommandEncoder().writeToBuffer(PARAMS[slot].slice(), Std140Builder.onStack(stack, 16).putVec4(x, y, z, w).get()); }
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "risecode cached photo blur", target.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(pipeline); pass.setUniform("BlurParams", PARAMS[slot]); if(pipeline!=Pipelines.ANIMATED){pass.bindTexture("Sampler0", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));} pass.draw(3, 1, 0, 0);
        }
    }
    public static void draw(GuiGraphicsExtractor graphics) { draw(graphics, 1); }
    public static void draw(GuiGraphicsExtractor graphics, double opacity) {
        if(dev.riserecode.ui.panorama.LivingPanorama.active()){
            boolean saved=!dev.riserecode.ui.panorama.LivingPanorama.settled();if(saved)drawSaved(graphics,opacity);
            if(dev.riserecode.ui.panorama.LivingPanorama.draw(graphics)||saved)return;
        }
        drawSaved(graphics,opacity);
    }
    private static void drawSaved(GuiGraphicsExtractor graphics,double opacity){
        if (Theme.manager().choice("Background", dev.riserecode.ui.theme.ThemeManager.Background.class) == dev.riserecode.ui.theme.ThemeManager.Background.NONE) { return; }
        opacity *= Theme.number("Background Opacity", 1);
        var kind = Theme.manager().choice("Background", dev.riserecode.ui.theme.ThemeManager.Background.class);
        if (kind == dev.riserecode.ui.theme.ThemeManager.Background.SOLID || kind == dev.riserecode.ui.theme.ThemeManager.Background.GRADIENT) {
            dev.riserecode.util.RenderUtil.drawPaintRect(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0,
                    kind == dev.riserecode.ui.theme.ThemeManager.Background.SOLID ? Theme.paint("Background Color") :
                    new dev.riserecode.setting.ColorSetting.Paint(Theme.manager().color("Gradient Start"), Theme.manager().color("Gradient End"), dev.riserecode.setting.ColorSetting.Mode.GRADIENT,
                    dev.riserecode.util.ColorUtil.GradientDirection.VERTICAL, 0, graphics.guiHeight()), opacity);
            return;
        }
        ensure(); var setup = TextureSetup.singleTexture(scene.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        UiMesh.quad(graphics, Pipelines.DRAW, setup, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0, 0, 1, 1, Theme.opacity(Theme.manager().color("Background Tint"),opacity), Theme.opacity(Theme.manager().color("Background Tint"),opacity), 0, 0, 0, 0);
        dev.riserecode.util.RenderUtil.drawPaintRect(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0, Theme.paint("Background Color"), Theme.number("Background Darkness", 0.42) * opacity);

    }
    public static void drawBlur(GuiGraphicsExtractor graphics, double x, double y, double width, double height, double corner, double opacity) {
        if(dev.riserecode.ui.panorama.LivingPanorama.blur(graphics,x,y,width,height,corner,opacity)){return;}
        var kind = Theme.manager().choice("Background", dev.riserecode.ui.theme.ThemeManager.Background.class);
        if(kind == dev.riserecode.ui.theme.ThemeManager.Background.NONE){return;}
        if (kind == dev.riserecode.ui.theme.ThemeManager.Background.SOLID || kind == dev.riserecode.ui.theme.ThemeManager.Background.GRADIENT) {
            dev.riserecode.util.RenderUtil.scissorPush(graphics,x,y,width,height);
            try { draw(graphics,opacity); } finally { dev.riserecode.util.RenderUtil.scissorPop(graphics); } return;
        }
        opacity *= Theme.number("Background Opacity",1); ensure();
        UiMesh.quad(graphics, Pipelines.BOX, TextureSetup.singleTexture(blurred.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                x - 1, y - 1, width + 2, height + 2, (float) (-width / 2 - 1), (float) (-height / 2 - 1), (float) (width / 2 + 1), (float) (height / 2 + 1),
                Theme.opacity(Theme.manager().color("Background Tint"), opacity), Theme.opacity(Theme.manager().color("Background Tint"), opacity), (int) Math.round(width * 16), (int) Math.round(height * 16), (int) Math.round(Theme.radius(corner) * 256), 0);
        dev.riserecode.util.RenderUtil.drawPaintRect(graphics, x, y, width, height, corner, Theme.paint("Background Color"), Theme.number("Background Darkness", 0.42) * opacity);
    }
    public static long builds() { return builds; }
    private static void destroy() { if (scene != null) { scene.destroyBuffers(); blurred.destroyBuffers(); scene = blurred = null; } source = null; }
    public static void close() { BackgroundAssets.close(); destroy(); for (int i = 0; i < PARAMS.length; i++) { if (PARAMS[i] != null) { PARAMS[i].close(); PARAMS[i] = null; } } }
}
