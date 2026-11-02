package dev.riserecode.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.ui.theme.PageAppearance;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;

/** Shared, immutable per-quad glass parameters. Text is submitted above the material. */
public final class LiquidGlass {
    private static final class Pipelines {
        static final RenderPipeline BACKDROP = UiMesh.pipeline("liquid_glass", "liquid_glass");
        static final RenderPipeline SHEEN = UiMesh.pipeline("glass_sheen", "glass_sheen");
    }
    public static final double MAX_DISPLACEMENT = 8;
    private LiquidGlass() { }

    public static boolean enabled() {
        return Theme.manager() != null && !PageAppearance.plain()
                && Theme.manager().choice("Interface Material", ThemeManager.InterfaceMaterial.class)
                == ThemeManager.InterfaceMaterial.LIQUID_GLASS;
    }

    /** Kept inside signed 16-bit attributes; all parameters survive deferred GUI submission. */
    public static int parameters() {
        int refraction = (int) Math.round(Math.clamp(Theme.number("Glass Refraction", 1), 0, 2) * 50);
        int highlight = (int) Math.round(Math.clamp(Theme.number("Glass Highlights", .65), 0, 1) * 100);
        return 1 + refraction + 101 * highlight;
    }

    public static RenderPipeline backdrop(RenderPipeline fallback) { return enabled() ? Pipelines.BACKDROP : fallback; }
    public static int backdropParameters() { return enabled() ? parameters() : 0; }

    public static void sheen(GuiGraphicsExtractor g, double x, double y, double w, double h,
            double radius, double opacity) {
        if (!enabled() || opacity <= 0) { return; }
        quad(g, Pipelines.SHEEN, TextureSetup.noTexture(), x, y, w, h, radius,
                Theme.opacity(0xFFFFFFFF, opacity), parameters());
    }

    public static void quad(GuiGraphicsExtractor g, RenderPipeline pipeline, TextureSetup texture,
            double x, double y, double w, double h, double radius, int color, int parameters) {
        UiMesh.quad(g, pipeline, texture, x - 1, y - 1, w + 2, h + 2,
                (float) (-w / 2 - 1), (float) (-h / 2 - 1), (float) (w / 2 + 1), (float) (h / 2 + 1),
                color, color, (int) Math.round(w * 16), (int) Math.round(h * 16),
                (int) Math.round(Math.min(Theme.radius(radius), Math.min(w, h) / 2) * 256), parameters);
    }
}
