package dev.riserecode.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.riserecode.ui.theme.Theme;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Modern GUI extraction using cached, antialiased nine-slice distance masks. */
public final class RenderUtil {
    private static final int DENSITY = 4;
    private static final int SHADOW_PADDING = 16;
    private static final Map<MaskKey, Mask> MASKS = new HashMap<>();
    private enum Kind { FILL, BORDER, SHADOW }
    private record MaskKey(int radius, Kind kind) { }
    private record Mask(Identifier texture, int side, int edge) { }

    private RenderUtil() { }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        drawMask(graphics, x, y, width, height, radius, color, Kind.FILL);
    }

    public static void drawRoundedBorder(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        drawMask(graphics, x, y, width, height, radius, color, Kind.BORDER);
    }

    public static void drawDropShadow(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, double radius, int color) {
        drawMask(graphics, x, y + 3, width, height, radius, color, Kind.SHADOW);
    }

    public static void drawGradientRect(GuiGraphicsExtractor graphics, double x, double y,
            double width, double height, int top, int bottom) {
        graphics.fillGradient((int) x, (int) y, (int) Math.ceil(x + width),
                (int) Math.ceil(y + height), top, bottom);
    }

    public static void drawStringWithShadow(GuiGraphicsExtractor graphics, Font font, String text,
            double x, double y, int color) {
        drawText(graphics, font, text, x, y, color, false, true);
    }

    public static void drawText(GuiGraphicsExtractor graphics, Font font, String text,
            double x, double y, int color, boolean title, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.text(font, Theme.text(text, title), 0, 0, color, shadow);
        graphics.pose().popMatrix();
    }

    public static int textWidth(Font font, String text, boolean title) {
        return font.width(Theme.text(text, title));
    }

    public static String ellipsize(Font font, String text, double width) {
        if (textWidth(font, text, false) <= width) { return text; }
        String shortened = text;
        while (!shortened.isEmpty() && textWidth(font, shortened + "…", false) > width) {
            int end = shortened.offsetByCodePoints(shortened.length(), -1);
            shortened = shortened.substring(0, end);
        }
        return shortened + "…";
    }

    /** Coordinates are GUI units; Minecraft captures the current pose transform. */
    public static void scissorPush(GuiGraphicsExtractor graphics, double x, double y, double width, double height) {
        graphics.enableScissor((int) Math.floor(x), (int) Math.floor(y),
                (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    }

    public static void scissorPop(GuiGraphicsExtractor graphics) { graphics.disableScissor(); }

    private static void drawMask(GuiGraphicsExtractor graphics, double x, double y, double width,
            double height, double radius, int color, Kind kind) {
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) { return; }
        int r = Math.max(1, Math.min(24, (int) Math.floor(Math.min(radius, Math.min(width, height) / 2))));
        Mask mask = MASKS.computeIfAbsent(new MaskKey(r, kind), RenderUtil::createMask);
        int padding = kind == Kind.SHADOW ? SHADOW_PADDING : 0;
        int w = Math.max(2 * mask.edge, (int) Math.round(width) + padding * 2);
        int h = Math.max(2 * mask.edge, (int) Math.round(height) + padding * 2);
        int edge = mask.edge;
        int[] destinationX = {0, edge, w - edge, w};
        int[] destinationY = {0, edge, h - edge, h};
        int[] source = {0, edge * DENSITY, (mask.side - edge) * DENSITY, mask.side * DENSITY};
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (x - padding), (float) (y - padding));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int sliceWidth = destinationX[column + 1] - destinationX[column];
                int sliceHeight = destinationY[row + 1] - destinationY[row];
                if (sliceWidth == 0 || sliceHeight == 0) { continue; }
                graphics.blit(RenderPipelines.GUI_TEXTURED, mask.texture,
                        destinationX[column], destinationY[row], source[column], source[row], sliceWidth, sliceHeight,
                        source[column + 1] - source[column], source[row + 1] - source[row],
                        mask.side * DENSITY, mask.side * DENSITY, color);
            }
        }
        graphics.pose().popMatrix();
    }

    private static Mask createMask(MaskKey key) {
        int padding = key.kind == Kind.SHADOW ? SHADOW_PADDING : 0;
        int edge = key.radius + padding;
        int side = edge * 2 + 2;
        NativeImage image = new NativeImage(side * DENSITY, side * DENSITY, false);
        for (int py = 0; py < side * DENSITY; py++) {
            for (int px = 0; px < side * DENSITY; px++) {
                double x = (px + 0.5) / DENSITY;
                double y = (py + 0.5) / DENSITY;
                double qx = Math.abs(x - side / 2.0) - 1.0;
                double qy = Math.abs(y - side / 2.0) - 1.0;
                double distance = Math.hypot(Math.max(qx, 0), Math.max(qy, 0))
                        + Math.min(Math.max(qx, qy), 0) - key.radius;
                double coverage = switch (key.kind) {
                    case FILL -> MathUtil.clamp01(0.5 - distance);
                    case BORDER -> MathUtil.clamp01(0.5 - distance) * MathUtil.clamp01(distance + 1.5);
                    case SHADOW -> Math.exp(-Math.pow(Math.max(distance, 0), 2) / 40.0);
                };
                image.setPixel(px, py, ((int) Math.round(coverage * 255) << 24) | 0xFFFFFF);
            }
        }
        Identifier texture = Identifier.fromNamespaceAndPath("riserecode",
                "ui/mask_" + key.kind.name().toLowerCase(java.util.Locale.ROOT) + "_" + key.radius);
        DynamicTexture dynamic = new DynamicTexture(() -> texture.toString(), image) {
            @Override
            public GpuSampler getSampler() {
                return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            }
        };
        // TextureManager owns the GPU lifetime and releases these at shutdown.
        Minecraft.getInstance().getTextureManager().register(texture, dynamic);
        return new Mask(texture, side, edge);
    }
}
