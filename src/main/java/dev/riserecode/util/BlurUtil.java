package dev.riserecode.util;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
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
import com.mojang.blaze3d.buffers.Std140Builder;
import org.lwjgl.system.MemoryStack;

/** Scene-only half-resolution blur. Extraction requests effects; the render hook executes them
 * after the world render and before all GUI/HUD draws, avoiding recursive menu blur and readbacks.
 */
public final class BlurUtil {
    private static final class Pipelines {
        private static final RenderPipeline BLUR = RenderPipelines.register(RenderPipeline.builder()
                .withLocation(UiMesh.id("pipeline/blur")).withVertexShader("core/screenquad")
                .withFragmentShader(UiMesh.id("core/blur")).withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("BlurParams", UniformType.UNIFORM_BUFFER).build())
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(Optional.empty()).withCull(false)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).build());
        private static final RenderPipeline BOX = UiMesh.pipeline("blur_box", "blur_box");
    }
    private static TextureTarget snapshot, ping, gui, world;
    private static final GpuBuffer[] PARAMETERS = new GpuBuffer[6];
    private record Region(int left,int top,int right,int bottom) {
        Region union(Region r) { return new Region(Math.min(left,r.left),Math.min(top,r.top),Math.max(right,r.right),Math.max(bottom,r.bottom)); }
        Region expand(int pixels) { return new Region(Math.max(0,left-pixels),Math.max(0,top-pixels),Math.min(snapshot.width,right+pixels),Math.min(snapshot.height,bottom+pixels)); }
    }
    private static Region guiRegion, passRegion;
    private static boolean guiRequested, worldRequested;
    private static double requestedGuiRadius, requestedWorldRadius, worldOpacity;
    private static int parameterIndex, allocations;
    private static long frames;
    private static double lastSubmitMilliseconds;
    private BlurUtil() { }
    public static int getAllocationCount() { return allocations; }
    public static long getRenderedFrames() { return frames; }
    public static double getLastSubmitMilliseconds() { return lastSubmitMilliseconds; }
    public static int getTargetWidth() { return snapshot == null ? 0 : snapshot.width; }
    public static int getTargetHeight() { return snapshot == null ? 0 : snapshot.height; }
    private static void ensureTargets() {
        Minecraft minecraft = Minecraft.getInstance();
        int width = Math.max(1, (minecraft.getWindow().getScreenWidth() + 1) / 2);
        int height = Math.max(1, (minecraft.getWindow().getScreenHeight() + 1) / 2);
        if (snapshot != null && snapshot.width == width && snapshot.height == height) { return; }
        destroyTargets();
        snapshot = new TextureTarget("risecode scene / half", width, height, false, GpuFormat.RGBA8_UNORM);
        ping = new TextureTarget("risecode blur / ping", width, height, false, GpuFormat.RGBA8_UNORM);
        gui = new TextureTarget("risecode blur / GUI", width, height, false, GpuFormat.RGBA8_UNORM);
        world = new TextureTarget("risecode blur / world", width, height, false, GpuFormat.RGBA8_UNORM);
        allocations++;
    }
    public static void drawGui(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, double openingOpacity) {
        if (Theme.bool("World Blur", false) && Theme.number("World Blur Radius", 8) > 0) {
            ensureTargets(); worldRequested = true;
            requestedWorldRadius = Theme.number("World Blur Radius", 8); worldOpacity = MathUtil.clamp01(openingOpacity);
        }
        drawBox(graphics, x, y, width, height, radius,
                openingOpacity * Theme.number("GUI Blur Opacity", 0.8));
    }
    public static void drawBox(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, double opacity) {
        double blur = Theme.number("GUI Blur Radius", 12);
        if (blur <= 0 || opacity <= 0 || width <= 0 || height <= 0) { return; }
        ensureTargets(); guiRequested = true; requestedGuiRadius = blur;
        double pad = 1;
        var bounds = new net.minecraft.client.gui.navigation.ScreenRectangle((int)Math.floor(x-pad),(int)Math.floor(y-pad),(int)Math.ceil(width+pad*2+1),(int)Math.ceil(height+pad*2+1)).transformMaxBounds(graphics.pose());
        var clip = graphics.scissorStack.peek(); if (clip != null) { bounds = bounds.intersection(clip); }
        if (bounds != null) {
            double sx=snapshot.width/(double)graphics.guiWidth(),sy=snapshot.height/(double)graphics.guiHeight();
            var region = new Region((int)Math.floor(bounds.left()*sx),(int)Math.floor(bounds.top()*sy),(int)Math.ceil(bounds.right()*sx),(int)Math.ceil(bounds.bottom()*sy)).expand(2);
            guiRegion = guiRegion == null ? region : guiRegion.union(region);
        }
        int color = Theme.opacity(0xFFFFFFFF, opacity);
        UiMesh.quad(graphics, Pipelines.BOX, TextureSetup.singleTexture(gui.getColorTextureView(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)), x - pad, y - pad, width + pad * 2,
                height + pad * 2, (float) (-width / 2 - pad), (float) (-height / 2 - pad),
                (float) (width / 2 + pad), (float) (height / 2 + pad), color, color,
                (int) Math.round(width * 16), (int) Math.round(height * 16),
                (int) Math.round(Math.min(Theme.radius(radius), Math.min(width, height) / 2) * 256), 0);
    }
    /** Invoked on the render thread by GuiRendererMixin after panorama and before GUI preparation. */
    public static void renderBeforeGui(RenderTarget main) {
        if ((!guiRequested && !worldRequested) || snapshot == null) { return; }
        long start = System.nanoTime(); parameterIndex = 0;
        try {
            passRegion = worldRequested || guiRegion == null ? null : guiRegion.expand((int)Math.ceil(requestedGuiRadius/2)+2);
            pass(main.getColorTextureView(), snapshot, 0, 0, 0, 1);
            if (guiRequested) { gaussian(snapshot, gui, requestedGuiRadius, worldRequested && requestedWorldRadius == requestedGuiRadius ? null : guiRegion); }
            passRegion = null;
            if (worldRequested) {
                if (guiRequested && requestedWorldRadius == requestedGuiRadius) {
                    pass(gui.getColorTextureView(), main, 0, 0, 2, worldOpacity);
                } else {
                    gaussian(snapshot, world, requestedWorldRadius, null);
                    pass(world.getColorTextureView(), main, 0, 0, 2, worldOpacity);
                }
            }
            frames++;
        } finally {
            guiRequested = worldRequested = false; guiRegion = passRegion = null; lastSubmitMilliseconds = (System.nanoTime() - start) / 1e6;
        }
    }
    private static void gaussian(RenderTarget source, RenderTarget destination, double radius, Region region) {
        // The farthest bilinear tap lies at 3.230769 source texels; radius is in full framebuffer pixels.
        float spread = (float) (radius / (2 * 3.230769));
        passRegion = region == null ? null : region.expand((int)Math.ceil(radius/2)+2);
        pass(source.getColorTextureView(), ping, spread / snapshot.width, 0, 1, 1);
        passRegion = region;
        pass(ping.getColorTextureView(), destination, 0, spread / snapshot.height, 1, 1);
    }
    private static void pass(GpuTextureView source, RenderTarget destination, float dx, float dy, int mode, double opacity) {
        int slot = parameterIndex++;
        if (PARAMETERS[slot] == null) {
            PARAMETERS[slot] = RenderSystem.getDevice().createBuffer(() -> "risecode blur parameters",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 16);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var bytes = Std140Builder.onStack(stack, 16).putVec4(dx, dy, mode, (float) opacity).get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(PARAMETERS[slot].slice(), bytes);
        }
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "risecode half-resolution blur", destination.getColorTextureView(), Optional.empty())) {
            if (passRegion != null) {
                double sx=destination.width/(double)snapshot.width,sy=destination.height/(double)snapshot.height;
                int left=(int)Math.floor(passRegion.left*sx),right=(int)Math.ceil(passRegion.right*sx),top=(int)Math.floor(passRegion.top*sy),bottom=(int)Math.ceil(passRegion.bottom*sy);
                pass.enableScissor(left,destination.height-bottom,Math.max(0,right-left),Math.max(0,bottom-top));
            }
            pass.setPipeline(Pipelines.BLUR); pass.setUniform("BlurParams", PARAMETERS[slot]);
            pass.bindTexture("Sampler0", source, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.draw(3, 1, 0, 0);
        }
    }
    private static void destroyTargets() {
        if (snapshot != null) {
            snapshot.destroyBuffers(); ping.destroyBuffers(); gui.destroyBuffers(); world.destroyBuffers();
            snapshot = ping = gui = world = null;
        }
    }
    public static void close() {
        destroyTargets();
        for (int i = 0; i < PARAMETERS.length; i++) { if (PARAMETERS[i] != null) { PARAMETERS[i].close(); PARAMETERS[i] = null; } }
        guiRequested = worldRequested = false; guiRegion = passRegion = null;
    }
}
