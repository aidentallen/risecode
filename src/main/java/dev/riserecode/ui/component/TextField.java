package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** Borderless centered input with Unicode editing, selection and standard clipboard shortcuts. */
public final class TextField extends UIComponent {
    private final Supplier<String> placeholder;
    private String value = "";
    private int caret;
    private int anchor;
    private boolean focused;
    private double time;
    private Font renderedFont;
    private java.util.function.Predicate<KeyEvent> keyHandler = ignored -> false;
    public void onKey(java.util.function.Predicate<KeyEvent> handler) { keyHandler = handler; }
    private int maximum = 512;
    private String validation = "";
    private Consumer<String> changed = ignored -> { };
    private Runnable committed = () -> { }, cancelled = () -> { };
    public void setMaximum(int value) { maximum = value; }
    public void onChange(Consumer<String> value) { changed = value; }
    public void onCommit(Runnable value) { committed = value; }
    public void onCancel(Runnable value) { cancelled = value; }
    public void validation(String value) { validation = value; }
    public String validation() { return validation; }
    public void selectAll() { anchor = 0; caret = value.length(); }
    public void commit() { committed.run(); setFocused(false); }
    public void cancel() { cancelled.run(); setFocused(false); }

    public TextField(Supplier<String> placeholder) { this.placeholder = placeholder; }
    public String getValue() { return value; }
    public void setValue(String next) { value = next; caret = anchor = value.length(); time = 0; }
    public void setFocused(boolean next) { if (next) { FocusedInput.focus(this); } else { FocusedInput.release(this); } focused = next; time = 0; }
    public boolean isFocused() { return focused; }
    private double left(Font font) {
        double span = RenderUtil.textWidth(font, value, false);
        if (span <= width - 16) { return x + (width - span) / 2; }
        double prefix = RenderUtil.textWidth(font, value.substring(0, caret), false);
        return x + 8 - Math.max(0, prefix - width + 16);
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        renderedFont = font; time += seconds;
        double hovered = updateHover(mouseX, mouseY, seconds);
        String text = value.isEmpty() ? placeholder.get() : value;
        double left = value.isEmpty() ? x + (width - RenderUtil.textWidth(font, text, false)) / 2 : left(font);
        double top = y + (height - 11) / 2;
        RenderUtil.scissorPush(graphics, x + 4, y, width - 8, height);
        try {
            if (focused && caret != anchor) {
                double start = left(font) + RenderUtil.textWidth(font, value.substring(0, Math.min(caret, anchor)), false);
                double end = left(font) + RenderUtil.textWidth(font, value.substring(0, Math.max(caret, anchor)), false);
                RenderUtil.drawRoundedRect(graphics, start, top - 1, end - start, 12, 1, Theme.opacity(Theme.accent(), opacity * 0.25));
            }
            RenderUtil.text(graphics, font, text, left, top, 1,
                    Theme.opacity(value.isEmpty() ? Theme.blendPalette(Theme.MUTED, Theme.SECONDARY, hovered) : Theme.TEXT, opacity));
            if (focused && (!value.isEmpty() || time % 1.1 < 0.6)) {
                double cursor = value.isEmpty() ? left - 2 : left(font) + RenderUtil.textWidth(font, value.substring(0, caret), false);
                double alpha = 0.3 + 0.7 * (0.5 + 0.5 * Math.cos(time * Math.PI * 2 / 1.1));
                RenderUtil.drawRoundedRect(graphics, cursor, top, 0.6, 10, 0.3, Theme.opacity(Theme.TEXT, opacity * alpha));
            }
        } finally { RenderUtil.scissorPop(graphics); }
        if (!validation.isEmpty() && focused) {
            RenderUtil.drawRoundedBorder(graphics, x, y, width, height, 2, Theme.opacity(0xFFFF6677, opacity), 0.7);
            RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, validation, width / 0.55), x, y + height - 5, 0.55, Theme.opacity(0xFFFF6677, opacity));
        }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { setFocused(false); return false; }
        setFocused(true); time = 0;
        if (renderedFont != null) {
            int best = 0; double distance = Double.POSITIVE_INFINITY;
            for (int index = 0; index <= value.length();) {
                double next = Math.abs(mouseX - left(renderedFont) - RenderUtil.textWidth(renderedFont, value.substring(0, index), false));
                if (next < distance) { best = index; distance = next; }
                if (index == value.length()) { break; }
                index = value.offsetByCodePoints(index, 1);
            }
            caret = anchor = best;
        }
        return true;
    }
    public boolean charTyped(CharacterEvent event) {
        if (!focused || !event.isAllowedChatCharacter()) { return false; }
        insert(event.codepointAsString()); return true;
    }
    private void insert(String text) {
        int start = Math.min(caret, anchor); int end = Math.max(caret, anchor);
        String clean = text.codePoints().filter(cp -> !Character.isISOControl(cp) && cp != 0xA7)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        if (value.length() - (end - start) + clean.length() > maximum) { return; }
        value = value.substring(0, start) + clean + value.substring(end);
        caret = anchor = start + clean.length(); time = 0; changed.accept(value);
    }
    public boolean keyPressed(KeyEvent event, Supplier<String> clipboard, Consumer<String> setClipboard) {
        if (!focused) { return false; }
        if (keyHandler.test(event)) { return true; }
        if (event.isSelectAll()) { anchor = 0; caret = value.length(); return true; }
        if (event.isCopy() || event.isCut()) {
            setClipboard.accept(value.substring(Math.min(caret, anchor), Math.max(caret, anchor)));
            if (event.isCut()) { insert(""); } return true;
        }
        if (event.isPaste()) { insert(clipboard.get()); return true; }
        if (event.key() == 259 || event.key() == 261) {
            if (caret == anchor) {
                if (event.key() == 259 && caret > 0) { anchor = value.offsetByCodePoints(caret, -1); }
                if (event.key() == 261 && caret < value.length()) { anchor = value.offsetByCodePoints(caret, 1); }
            }
            insert(""); return true;
        }
        int next = switch (event.key()) {
            case 263 -> caret == 0 ? 0 : value.offsetByCodePoints(caret, -1);
            case 262 -> caret == value.length() ? caret : value.offsetByCodePoints(caret, 1);
            case 268 -> 0;
            case 269 -> value.length();
            default -> -1;
        };
        if (next >= 0) { caret = next; if (!event.hasShiftDown()) { anchor = caret; } time = 0; return true; }
        return false;
    }
}
