package dev.riserecode.util;

/** Stateless easing functions for normalized animation time. */
public final class MathUtil {
    private MathUtil() {
    }

    public enum Easing {
        OUT_QUART,
        IN_OUT_CUBIC,
        OUT_ELASTIC;

        public double apply(double progress) {
            return switch (this) {
                case OUT_QUART -> easeOutQuart(progress);
                case IN_OUT_CUBIC -> easeInOutCubic(progress);
                case OUT_ELASTIC -> easeOutElastic(progress);
            };
        }
    }

    /** Clamps finite input to [0, 1]; invalid time values fail early. */
    public static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Progress must be finite");
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static double easeOutQuart(double progress) {
        double inverse = 1.0 - clamp01(progress);
        double squared = inverse * inverse;
        return 1.0 - squared * squared;
    }

    public static double easeInOutCubic(double progress) {
        double t = clamp01(progress);
        if (t < 0.5) {
            return 4.0 * t * t * t;
        }
        double inverse = 1.0 - t;
        return 1.0 - 4.0 * inverse * inverse * inverse;
    }

    /** Output deliberately overshoots 1 to provide the requested bounce. */
    public static double easeOutElastic(double progress) {
        double t = clamp01(progress);
        if (t == 0.0 || t == 1.0) {
            return t;
        }
        double period = (2.0 * Math.PI) / 3.0;
        return Math.pow(2.0, -10.0 * t) * Math.sin((10.0 * t - 0.75) * period) + 1.0;
    }

    /** Unclamped interpolation: elastic easing needs values beyond [0, 1]. */
    public static double lerp(double start, double end, double amount) {
        return Math.fma(amount, end - start, start);
    }
}
