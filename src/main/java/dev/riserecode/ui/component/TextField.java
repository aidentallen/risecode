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
public final class TextField extends UIComponent implements FocusedInput.Owner {
    private final Supplier<String> placeholder;
    private final TextEditModel edit = new TextEditModel(512,false);
    private boolean focused;
    private boolean masked;
    public void masked(boolean value) { masked=value; }
    private String displayed() { return masked ? "•".repeat(edit.value().codePointCount(0,edit.value().length())) : edit.value(); }
    private String prefix(int position) { return masked ? "•".repeat(edit.value().codePointCount(0,position)) : edit.value().substring(0,position); }
    private double time;
    private Font renderedFont;
    private java.util.function.Predicate<KeyEvent> keyHandler = ignored -> false;
    public void onKey(java.util.function.Predicate<KeyEvent> handler) { keyHandler = handler; }

    private String validation = "";
    private Runnable committed = () -> { }, cancelled = () -> { };
    public void setMaximum(int value) { edit.maximum(value); }
    public void onChange(Consumer<String> value) { edit.onChange(value); }
    public void onCommit(Runnable value) { committed = value; }
    public void onCancel(Runnable value) { cancelled = value; }
    public void validation(String value) { validation = value; }
    public String validation() { return validation; }
    public void selectAll() { edit.selectAll(); }
    public void commit() { committed.run(); setFocused(false); }
    public void cancel() { cancelled.run(); setFocused(false); }

    public TextField(Supplier<String> placeholder) { this.placeholder = placeholder; }
    public String getValue() { return edit.value(); }
    public void setValue(String next) { edit.value(next); time = 0; }
    public void setFocused(boolean next) { if (next) { FocusedInput.focus(this); } else { FocusedInput.release(this); } focused = next; time = 0; }
    public boolean isFocused() { return focused; }
    private double left(Font font) {
        double span = RenderUtil.textWidth(font, displayed(), false);
        if (span <= width - 16) { return x + (width - span) / 2; }
        double prefix = RenderUtil.textWidth(font, prefix(edit.caret()), false);
        return x + 8 - Math.max(0, prefix - width + 16);
    }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        renderedFont = font; time += seconds;
        double hovered = updateHover(mouseX, mouseY, seconds);
        String text = edit.value().isEmpty() ? placeholder.get() : displayed();
        double left = edit.value().isEmpty() ? x + (width - RenderUtil.textWidth(font, text, false)) / 2 : left(font);
        double textHeight = 9 * Theme.number("Font Scale", 1);
        double top = y + (height - textHeight) / 2;
        RenderUtil.scissorPush(graphics, x + 4, y, width - 8, height);
        try {
            if (focused && edit.caret() != edit.anchor()) {
                double start = left(font) + RenderUtil.textWidth(font, prefix(Math.min(edit.caret(), edit.anchor())), false);
                double end = left(font) + RenderUtil.textWidth(font, prefix(Math.max(edit.caret(), edit.anchor())), false);
                RenderUtil.drawRoundedRect(graphics, start, top - 1, end - start, textHeight + 2, 1, Theme.opacity(Theme.accent(), opacity * 0.25));
            }
            RenderUtil.text(graphics, font, text, left, top, 1,
                    Theme.opacity(edit.value().isEmpty() ? Theme.blendPalette(Theme.MUTED, Theme.SECONDARY, hovered) : Theme.TEXT, opacity));
            if (focused && (!edit.value().isEmpty() || time % 1.1 < 0.6)) {
                double cursor = edit.value().isEmpty() ? left - 2 : left(font) + RenderUtil.textWidth(font, prefix(edit.caret()), false);
                double alpha = 0.3 + 0.7 * (0.5 + 0.5 * Math.cos(time * Math.PI * 2 / 1.1));
                RenderUtil.drawRoundedRect(graphics, cursor, top, 0.6, textHeight, 0.3, Theme.opacity(Theme.TEXT, opacity * alpha));
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
            for (int index = 0; index <= edit.value().length();) {
                double next = Math.abs(mouseX - left(renderedFont) - RenderUtil.textWidth(renderedFont, prefix(index), false));
                if (next < distance) { best = index; distance = next; }
                if (index == edit.value().length()) { break; }
                index = edit.value().offsetByCodePoints(index, 1);
            }
            edit.move(best,false);
        }
        return true;
    }
    public boolean charTyped(CharacterEvent event) {
        if (!focused || !event.isAllowedChatCharacter()) { return false; }
        edit.insert(event.codepointAsString()); time=0; return true;
    }
    public boolean keyPressed(KeyEvent event, Supplier<String> clipboard, Consumer<String> copy) {
        if (!focused) { return false; } if (keyHandler.test(event)) { return true; }
        time=0; return edit.key(event,clipboard,copy);
    }
}
