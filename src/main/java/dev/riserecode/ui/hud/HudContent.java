package dev.riserecode.ui.hud;

import java.util.List;
import dev.riserecode.setting.ColorSetting;
import net.minecraft.resources.Identifier;

/** Presentation data, without Minecraft widgets or drawing calls in feature modules. */
public final class HudContent {
    public record Span(String text, int color, boolean gradient, ColorSetting.Paint paint) {
        public Span(String text, int color, boolean gradient) { this(text, color, gradient, null); }
        public static Span paint(String text, ColorSetting.Paint paint) { return new Span(text, paint.primary(), false, paint); }
    }
    public record Line(List<Span> spans, Identifier icon, double bar, int barColor, ColorSetting.Paint barPaint) {
        public Line(List<Span> spans, Identifier icon, double bar, int barColor) { this(spans, icon, bar, barColor, null); }
        public Line { spans = List.copyOf(spans); }
        public static Line text(String text, ColorSetting.Paint paint) { return new Line(List.of(Span.paint(text, paint)), null, -1, paint.primary()); }
        public static Line text(String text, int color, boolean gradient) { return new Line(List.of(new Span(text, color, gradient)), null, -1, color); }
    }
    private HudContent() { }
}
