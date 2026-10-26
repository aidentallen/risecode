package dev.riserecode.ui.theme;

import dev.riserecode.util.MathUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/** Packed ARGB design tokens. UI colors and font choices never enter modules. */
public final class Theme {
    public static final int BACKGROUND = 0xFF0F1115;
    public static final int PANEL = 0xBF181A20;
    public static final int SURFACE = 0xE022252E;
    public static final int HOVER = 0xEC2D3240;
    public static final int TEXT = 0xFFF4F6FC;
    public static final int SECONDARY = 0xFF989FAF;
    public static final int MUTED = 0xFF616A7D;
    public static final int BLUE = 0xFF3B82F6;
    public static final int FUCHSIA = 0xFFD946EF;
    public static final int BORDER = 0x14FFFFFF;
    public static final int SHADOW = 0x70000000;
    public static final int PANEL_RADIUS = 12;
    public static final int BUTTON_RADIUS = 8;
    public static final int PADDING = 16;
    private static final Style BODY = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "inter_regular")));
    private static final Style TITLE = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("riserecode", "inter_medium")));

    private Theme() { }

    public static Component text(String text, boolean title) {
        return Component.literal(text).withStyle(title ? TITLE : BODY);
    }

    public static int lerp(int from, int to, double progress) {
        double t = MathUtil.clamp01(progress);
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int channel = (int) Math.round(MathUtil.lerp((from >>> shift) & 255, (to >>> shift) & 255, t));
            result |= channel << shift;
        }
        return result;
    }

    /** Multiplies existing alpha; translucent panel tokens stay translucent. */
    public static int opacity(int color, double opacity) {
        int alpha = (int) Math.round((color >>> 24) * MathUtil.clamp01(opacity));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
