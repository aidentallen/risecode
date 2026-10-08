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
    private static TextureTarget scene, ping, blurred;
    private static GpuTextureView source;
    private static double radius = -1;
    private static final GpuBuffer[] PARAMS = new GpuBuffer[3];
    private static long builds;
    private static final class Pipelines {
        private static final RenderPipeline PHOTO = post("menu_photo_copy", "menu_photo_copy");
        private static final RenderPipeline BLUR = post("menu_photo_blur", "blur");
        private static final RenderPipeline DRAW = UiMesh.pipeline("menu_photo", "menu_photo");
        private static final RenderPipeline BOX = UiMesh.pipeline("menu_photo_box", "blur_box");
    }
    private MenuBackdrop() { }
    private static RenderPipeline post(String name, String shader) {
        return RenderPipelines.register(RenderPipeline.builder().withLocation(UiMesh.id("pipeline/" + name))
                .withVertexShader("core/screenquad").withFragmentShader(UiMesh.id("core/" + shader))
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0).withBindGroupLayout(BindGroupLayout.builder().withUniform("BlurParams", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(ColorTargetState.DEFAULT).withDepthStencilState(Optional.empty()).withCull(false).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
    }
    private static void ensure() {
        Minecraft mc = Minecraft.getInstance(); var texture = mc.getTextureManager().getTexture(PHOTO);
        int width = Math.max(1, (mc.getWindow().getScreenWidth() + 1) / 2), height = Math.max(1, (mc.getWindow().getScreenHeight() + 1) / 2);
        if (scene == null || scene.width != width || scene.height != height) {
            destroy(); scene = new TextureTarget("risecode menu photo", width, height, false, GpuFormat.RGBA8_UNORM);
            ping = new TextureTarget("risecode photo blur ping", width, height, false, GpuFormat.RGBA8_UNORM);
            blurred = new TextureTarget("risecode photo blur", width, height, false, GpuFormat.RGBA8_UNORM); radius = -1;
        }
        double next = Theme.number("GUI Blur Radius", 12);
        if (source == texture.getTextureView() && radius == next) { return; }
        source = texture.getTextureView(); radius = next;
        double photoAspect = (double) texture.getTexture().getWidth(0) / texture.getTexture().getHeight(0), screenAspect = (double) width / height;
        float sx = (float) Math.min(1, screenAspect / photoAspect), sy = (float) Math.min(1, photoAspect / screenAspect);
        pass(source, scene, Pipelines.PHOTO, 0, sx, sy, (1 - sx) / 2, (1 - sy) / 2);
        float spread = (float) (next / (2 * 3.230769));
        pass(scene.getColorTextureView(), ping, Pipelines.BLUR, 1, spread / width, 0, next > 0 ? 1 : 2, 1);
        pass(ping.getColorTextureView(), blurred, Pipelines.BLUR, 2, 0, spread / height, next > 0 ? 1 : 2, 1); builds++;
    }
    private static void pass(GpuTextureView source, RenderTarget target, RenderPipeline pipeline, int slot, float x, float y, float z, float w) {
        if (PARAMS[slot] == null) { PARAMS[slot] = RenderSystem.getDevice().createBuffer(() -> "risecode photo parameters", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16); }
        try (MemoryStack stack = MemoryStack.stackPush()) { RenderSystem.getDevice().createCommandEncoder().writeToBuffer(PARAMS[slot].slice(), Std140Builder.onStack(stack, 16).putVec4(x, y, z, w).get()); }
        try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "risecode cached photo blur", target.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(pipeline); pass.setUniform("BlurParams", PARAMS[slot]); pass.bindTexture("Sampler0", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)); pass.draw(3, 1, 0, 0);
        }
    }
    public static void draw(GuiGraphicsExtractor graphics) { draw(graphics, 1); }
    public static void draw(GuiGraphicsExtractor graphics, double opacity) {
        ensure(); var setup = TextureSetup.singleTexture(scene.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        UiMesh.quad(graphics, Pipelines.DRAW, setup, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0, 0, 1, 1, Theme.opacity(-1,opacity), Theme.opacity(-1,opacity), 0, 0, 0, 0);
        dev.riserecode.util.RenderUtil.drawPaintRect(graphics, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0, Theme.paint("Background Color"), Theme.number("Background Darkness", 0.42) * opacity);
    }
    public static void drawBlur(GuiGraphicsExtractor graphics, double x, double y, double width, double height, double corner, double opacity) {
        ensure();
        UiMesh.quad(graphics, Pipelines.BOX, TextureSetup.singleTexture(blurred.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                x - 1, y - 1, width + 2, height + 2, (float) (-width / 2 - 1), (float) (-height / 2 - 1), (float) (width / 2 + 1), (float) (height / 2 + 1),
                Theme.opacity(-1, opacity), Theme.opacity(-1, opacity), (int) Math.round(width * 16), (int) Math.round(height * 16), (int) Math.round(Theme.radius(corner) * 256), 0);
        dev.riserecode.util.RenderUtil.drawPaintRect(graphics, x, y, width, height, corner, Theme.paint("Background Color"), Theme.number("Background Darkness", 0.42) * opacity);
    }
    public static long builds() { return builds; }
    private static void destroy() { if (scene != null) { scene.destroyBuffers(); ping.destroyBuffers(); blurred.destroyBuffers(); scene = ping = blurred = null; } source = null; }
    public static void close() { destroy(); for (int i = 0; i < PARAMS.length; i++) { if (PARAMS[i] != null) { PARAMS[i].close(); PARAMS[i] = null; } } }
}
