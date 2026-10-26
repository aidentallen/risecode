package dev.riserecode.render.glass;

import dev.riserecode.setting.ColorSetting.Paint;
import java.util.Objects;

/** Immutable extraction-time appearance; no mutable settings or entities cross into drawing. */
public record GlassMaterial(Paint color, double opacity, double frosted, double refraction,
        double highlights, Paint edges, double edgeStrength, double edgeThickness,
        double glow, double spread, boolean shimmer, double shimmerStrength,
        double shimmerSpeed, double originalTexture, boolean glint) {
    public GlassMaterial {
        Objects.requireNonNull(color); Objects.requireNonNull(edges);
        unit(opacity); unit(frosted); unit(refraction); unit(highlights); unit(edgeStrength);
        unit(glow); unit(shimmerStrength); unit(originalTexture);
        range(edgeThickness, .25, 12); range(spread, 0, 32); range(shimmerSpeed, 0, 5);
    }
    private static void unit(double v) { range(v, 0, 1); }
    private static void range(double v, double min, double max) {
        if (!Double.isFinite(v) || v < min || v > max) throw new IllegalArgumentException("Invalid glass material value");
    }
}
