package dev.riserecode.setting;

/** Packed 0xAARRGGBB color, including transparency. */
public final class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultArgb) {
        super(name, description, defaultArgb);
    }

    public int getAlpha() {
        return (getValue() >>> 24) & 0xFF;
    }

    public int getRed() {
        return (getValue() >>> 16) & 0xFF;
    }

    public int getGreen() {
        return (getValue() >>> 8) & 0xFF;
    }

    public int getBlue() {
        return getValue() & 0xFF;
    }

    public static int argb(int alpha, int red, int green, int blue) {
        for (int component : new int[] {alpha, red, green, blue}) {
            if (component < 0 || component > 255) {
                throw new IllegalArgumentException("Color components must be within [0, 255]");
            }
        }
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
