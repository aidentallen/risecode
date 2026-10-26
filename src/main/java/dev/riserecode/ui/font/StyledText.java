package dev.riserecode.ui.font;

import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** One set of runs drives measurement, wrapping, drawing and interaction regions. */
public final class StyledText {
    public record Run(String text, Style style) { }
    public record Region(double x, double width, Style style) { }
    private final List<Run> runs;
    private StyledText(List<Run> runs) { this.runs = List.copyOf(runs); }
    public List<Run> runs() { return runs; }
    public static StyledText of(String text) { return of(Component.literal(text)); }
    public static StyledText of(net.minecraft.util.FormattedCharSequence sequence) {
        List<Run> out=new ArrayList<>();StringBuilder text=new StringBuilder();Style[] current={Style.EMPTY};
        sequence.accept((index,style,code)->{if(!style.equals(current[0])){if(!text.isEmpty()){out.add(new Run(text.toString(),current[0]));text.setLength(0);}current[0]=style;}text.appendCodePoint(code);return true;});
        if(!text.isEmpty()){out.add(new Run(text.toString(),current[0]));}return new StyledText(out);
    }
    public static StyledText of(net.minecraft.network.chat.FormattedText component) {
        List<Run> out = new ArrayList<>();
        component.visit((base, text) -> {
            Style style = base; StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < text.length();) {
                int code = text.codePointAt(i); i += Character.charCount(code);
                if (code == '§' && i < text.length()) {
                    ChatFormatting format = ChatFormatting.getByCode(text.charAt(i++));
                    if (format != null) {
                        if (!buffer.isEmpty()) { out.add(new Run(buffer.toString(), style)); buffer.setLength(0); }
                        style = format == ChatFormatting.RESET ? base : format.ordinal() <= ChatFormatting.WHITE.ordinal()
                                ? base.withColor(format).withBold(false).withItalic(false).withUnderlined(false).withStrikethrough(false).withObfuscated(false)
                                : style.applyFormat(format);
                    }
                } else { buffer.appendCodePoint(code); }
            }
            if (!buffer.isEmpty()) { out.add(new Run(buffer.toString(), style)); }
            return Optional.empty();
        }, Style.EMPTY);
        return new StyledText(out);
    }
    private static boolean nativeRun(Run run){return run.style().isObfuscated()||!run.style().getFont().equals(net.minecraft.network.chat.FontDescription.DEFAULT);}
    private static Component component(Run run){return Component.literal(run.text()).setStyle(run.style());}
    private static double advance(Font font, Run run, boolean title) {
        if(nativeRun(run)){return font.width(component(run))*dev.riserecode.ui.theme.Theme.number("Font Scale",1);}
        return CustomTextRenderer.width(font, run.text(), title) + (run.style().isBold() ? 0.35 : 0);
    }
    public double width(Font font) { return width(font, false); }
    public double width(Font font, boolean title) { return runs.stream().mapToDouble(run -> advance(font, run, title)).sum(); }
    public List<Region> regions(Font font) {
        List<Region> out = new ArrayList<>(); double x = 0;
        for (Run run : runs) { double w = advance(font, run, false); out.add(new Region(x, w, run.style())); x += w; }
        return List.copyOf(out);
    }
    public Style styleAt(Font font, double x) {
        for (Region region : regions(font)) { if (x >= region.x() && x < region.x() + region.width()) { return region.style(); } }
        return null;
    }
    public List<StyledText> wrap(Font font, double maximum) {
        List<StyledText> lines = new ArrayList<>(); List<Run> line = new ArrayList<>(); double used = 0;
        for (Run run : runs) {
            StringBuilder word = new StringBuilder();
            for (int code : run.text().codePoints().toArray()) {
                String candidate = word.toString() + new String(Character.toChars(code));
                double w = advance(font, new Run(candidate, run.style()), false);
                if (code == '\n' || used + w > Math.max(1, maximum) && (!line.isEmpty() || !word.isEmpty())) {
                    if (!word.isEmpty()) { line.add(new Run(word.toString(), run.style())); }
                    lines.add(new StyledText(line)); line = new ArrayList<>(); used = 0; word.setLength(0);
                    if (code == '\n') { continue; }
                }
                word.appendCodePoint(code);
            }
            if (!word.isEmpty()) { Run tail = new Run(word.toString(), run.style()); line.add(tail); used += advance(font, tail, false); }
        }
        if (!line.isEmpty() || lines.isEmpty()) { lines.add(new StyledText(line)); }
        return List.copyOf(lines);
    }
    public void draw(GuiGraphicsExtractor g, Font font, double x, double y, double scale, int color) {
        g.pose().pushMatrix(); g.pose().translate((float)x, (float)y); g.pose().scale((float)scale, (float)scale);
        double at = 0;
        try { for (Run run : runs) {
            int tint = run.style().getColor() == null ? color : color & 0xFF000000 | run.style().getColor().getValue();
            g.pose().pushMatrix(); g.pose().translate((float)at, 0);
            if(nativeRun(run)){
                float size=(float)dev.riserecode.ui.theme.Theme.number("Font Scale",1);g.pose().scale(size,size);g.text(font,component(run),0,0,tint,false);g.pose().popMatrix();at+=advance(font,run,false);continue;
            }
            if (run.style().isItalic()) { g.pose().shearX(-0.15f); }
            CustomTextRenderer.draw(g, font, run.text(), tint, false, true, false);
            if (run.style().isBold()) { g.pose().translate(0.35f, 0); CustomTextRenderer.draw(g, font, run.text(), tint, false, false, false); }
            g.pose().popMatrix();
            double w = advance(font, run, false);
            if (run.style().isUnderlined()) { RenderUtil.line(g, at, 9, at + w, 9, 0.6, tint); }
            if (run.style().isStrikethrough()) { RenderUtil.line(g, at, 4, at + w, 4, 0.6, tint); }
            at += w;
        } } finally { g.pose().popMatrix(); }
    }
}
