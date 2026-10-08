package dev.riserecode.ui.component.setting;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.TextField;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.ColorUtil;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** Shared HSV/alpha/hex picker. Each color owns its endpoints, gradient and animation policy. */
public final class ColorPicker extends UIComponent {
    private static final class Pipelines { private static final RenderPipeline HSV = UiMesh.pipeline("hsv", "hsv"); }
    public static final double EXPANDED_HEIGHT = 310;
    private final ColorSetting setting;
    private final Runnable changed;
    private final Animation opening;
    private final TextField hex = new TextField(() -> "AARRGGBB");
    private final DoubleSetting speed = new DoubleSetting("Speed", "Color animation speed", 0.18, 0, 3);
    private final DoubleSetting spacing = new DoubleSetting("Spacing", "Gradient band width", 100, 1, 1000);
    private final Slider speedSlider;
    private final Slider spacingSlider;
    private final DoubleSetting[] channels = new DoubleSetting[4];
    private final Slider[] channelSliders = new Slider[4];
    private boolean expanded;
    private boolean secondary;
    private double hue, saturation, value;
    private int displayed, captured, original;
    private UIComponent capturedChild;

    public ColorPicker(ColorSetting setting, Runnable changed) { this(setting, changed, false); }
    public ColorPicker(ColorSetting setting, Runnable changed, boolean compact) {
        this.setting = setting; this.changed = changed; expanded = false;
        opening = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.EXPANSION, expanded ? 1 : 0, 0.2, MathUtil.Easing.OUT_QUART);
        speedSlider = new Slider(speed, () -> { setting.setSpeed(speed.getValue()); changed.run(); });
        spacingSlider = new Slider(spacing, () -> { setting.setSpacing(spacing.getValue()); changed.run(); });
        for (int i=0;i<4;i++) {
            final int index=i, shift=(3-i)*8;
            channels[i]=new DoubleSetting(new String[]{"Alpha","Red","Green","Blue"}[i],"Color channel",(current()>>>shift)&255,0,255);
            channelSliders[i]=new Slider(channels[i],()->{write((current()&~(255<<shift))|((int)Math.round(channels[index].getValue())<<shift));readColor();},1);
        }
        hex.onChange(ignored -> commitHex()); hex.onCancel(() -> { write(original); readColor(); });
        hex.onCommit(() -> { hex.setValue(ColorSetting.hex(current())); hex.validation(""); });
        readColor();
    }
    public ColorSetting getSetting() { return setting; }
    public boolean isExpanded() { return expanded; }
    public TextField getHexField() { return hex; }
    public Slider channel(int index) { return channelSliders[index]; }
    public Slider speed() { return speedSlider; }
    public Slider spacing() { return spacingSlider; }
    public double getPreferredHeight() { return 22 + (EXPANDED_HEIGHT - 22) * opening.getValue(); }
    public void setExpanded(boolean value) { expanded = value; opening.animateTo(expanded ? 1 : 0); if (!expanded) { unfocus(); mouseReleased(0, 0, 0); } }
    public void unfocus() { hex.setFocused(false); }
    public void pointerFocus(double mouseX, double mouseY) { if (!hex.contains(mouseX, mouseY)) { unfocus(); } }
    private int current() { return secondary ? setting.getSecondary() : setting.getValue(); }
    private void write(int color) { if (setting.getMode() == ColorSetting.Mode.THEME || secondary && setting.getMode() == ColorSetting.Mode.SOLID) { setting.setMode(secondary ? ColorSetting.Mode.GRADIENT : ColorSetting.Mode.SOLID); } if (secondary) { setting.setSecondary(color); } else { setting.setValue(color); } displayed = color; changed.run(); }
    private void readColor() {
        displayed = current();
        double r = ((displayed >>> 16) & 255) / 255.0, g = ((displayed >>> 8) & 255) / 255.0, b = (displayed & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
        value = max; saturation = max == 0 ? 0 : delta / max;
        if (delta > 0) {
            double h = max == r ? (g - b) / delta : max == g ? (b - r) / delta + 2 : (r - g) / delta + 4;
            hue = (h / 6 + 1) % 1;
        }
        for(int i=0;i<4;i++){if(channels[i]!=null){channels[i].setValue((double)((displayed>>>((3-i)*8))&255));}}
        if (!hex.isFocused()) { hex.setValue(ColorSetting.hex(displayed)); }
    }
    public static int hsv(double hue, double saturation, double value) { return ColorUtil.hsv(hue, MathUtil.clamp01(saturation), MathUtil.clamp01(value)); }
    @Override public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        opening.animateTo(expanded ? 1 : 0); opening.update(seconds);
        if (current() != displayed) { readColor(); }
        String label = Theme.displayName(setting.getName());
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, label, (width - 42) / 0.85), x, y + 4, 0.85, Theme.opacity(Theme.TEXT, opacity));
        RenderUtil.drawPaintRect(graphics, x + width - 36, y + 3, 28, 12, 3, setting.paint(), opacity);
        RenderUtil.text(graphics, font, expanded ? "−" : "+", x + width - 6, y + 4, 0.8, Theme.opacity(Theme.SECONDARY, opacity));
        if (opening.getValue() <= 0.001) { return; }
        double alpha = opacity * MathUtil.clamp01(opening.getValue());
        RenderUtil.scissorPush(graphics, x, y + 22, width, Math.max(0.1, height - 22));
        try {
            for (int i = 0; i < 2; i++) {
                double left = x + i * (width / 2 + 2); boolean selected = secondary == (i == 1);
                RenderUtil.drawRoundedRect(graphics, left, y + 22, width / 2 - 3, 16, 3, Theme.opacity(selected ? Theme.HOVER : Theme.SURFACE, alpha));
                RenderUtil.text(graphics, font, i == 0 ? "Start" : "End", left + 5, y + 25, 0.76, Theme.opacity(selected ? Theme.accent() : Theme.SECONDARY, alpha));
                RenderUtil.drawRoundedRect(graphics, left + width / 2 - 17, y + 26, 9, 8, 2, Theme.opacity((i == 0 ? setting.getValue() : setting.getSecondary()) | 0xFF000000, alpha));
            }
            double top = y + 42, boxWidth = width - 20; int white = Theme.opacity(0xFFFFFFFF, alpha);
            UiMesh.quad(graphics, Pipelines.HSV, TextureSetup.noTexture(), x, top, boxWidth, 48, 0, 0, 1, 1,
                    white, white, (int) Math.round(hue * 30000), 0, 0, 0);
            UiMesh.quad(graphics, Pipelines.HSV, TextureSetup.noTexture(), x + width - 12, top, 12, 48, 0, 0, 1, 1,
                    white, white, 0, 1, 0, 0);
            double dotX = x + saturation * boxWidth, dotY = top + (1 - value) * 48;
            RenderUtil.drawRoundedRect(graphics, dotX - 2.5, dotY - 2.5, 5, 5, 2.5, white);
            RenderUtil.drawRoundedRect(graphics, dotX - 1.5, dotY - 1.5, 3, 3, 1.5, Theme.opacity(current() | 0xFF000000, alpha));
            RenderUtil.drawRoundedRect(graphics, x + width - 13, top + hue * 48 - 1, 14, 2, 0.5, white);
            for (int column = 0; column < Math.ceil(width / 5); column++) {
                RenderUtil.drawRoundedRect(graphics, x + column * 5, y + 95, Math.min(5, width - column * 5), 7, 0,
                        Theme.opacity((column & 1) == 0 ? 0xFF46505A : 0xFF222A32, alpha));
            }
            RenderUtil.drawGradientRect(graphics, x, y + 95, width, 7, current() & 0xFFFFFF, current() | 0xFF000000, true);
            RenderUtil.drawRoundedRect(graphics, x + (current() >>> 24) / 255.0 * width - 1, y + 94, 2, 9, 1, white);
            hex.setBounds(x, y + 108, width, 18);
            RenderUtil.drawRoundedRect(graphics, x, y + 108, width, 18, 3, Theme.opacity(Theme.HOVER, alpha));
            hex.render(graphics, font, mouseX, mouseY, seconds, alpha);
            RenderUtil.text(graphics, font, "Mode: " + Theme.displayName(setting.getMode().name()), x + 2, y + 132, 0.79, Theme.opacity(Theme.TEXT, alpha));
            RenderUtil.text(graphics, font, "Direction: " + Theme.displayName(setting.getDirection().name()), x + 2, y + 151, 0.79, Theme.opacity(Theme.SECONDARY, alpha));
            speed.setValue(setting.getSpeed()); spacing.setValue(setting.getSpacing());
            speedSlider.setBounds(x, y + 170, width, 22); spacingSlider.setBounds(x, y + 193, width, 22);
            speedSlider.render(graphics, font, mouseX, mouseY, seconds, alpha);
            spacingSlider.render(graphics, font, mouseX, mouseY, seconds, alpha);
            for(int i=0;i<4;i++){channelSliders[i].setBounds(x,y+216+i*22,width,22);channelSliders[i].render(graphics,font,mouseX,mouseY,seconds,alpha);}
        } finally { RenderUtil.scissorPop(graphics); }
    }
    private void update(double mouseX, double mouseY) {
        if (captured == 1) { saturation = MathUtil.clamp01((mouseX - x) / (width - 20)); value = 1 - MathUtil.clamp01((mouseY - y - 42) / 48); }
        else if (captured == 2) { hue = MathUtil.clamp01((mouseY - y - 42) / 48); }
        else if (captured == 3) { write((current() & 0xFFFFFF) | ((int) Math.round(MathUtil.clamp01((mouseX - x) / width) * 255) << 24)); readColor(); return; }
        write((current() & 0xFF000000) | (hsv(hue, saturation, value) & 0xFFFFFF)); readColor();
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        pointerFocus(mouseX, mouseY);
        if (!contains(mouseX, mouseY) || button != 0 && button != 1) { return false; }
        double local = mouseY - y;
        if (local < 22) { setExpanded(!expanded); return true; }
        if (!expanded || opening.getValue() < 0.99) { return true; }
        if (local >= 22 && local < 39) { secondary = mouseX >= x + width / 2; readColor(); return true; }
        if (button == 0 && local >= 42 && local <= 90) {
            captured = mouseX >= x + width - 12 ? 2 : mouseX < x + width - 20 ? 1 : 0;
            if (captured != 0) { update(mouseX, mouseY); } return true;
        }
        if (button == 0 && local >= 93 && local <= 104) { captured = 3; update(mouseX, mouseY); return true; }
        if (!hex.isFocused()) { original = current(); }
        if (hex.mouseClicked(mouseX, mouseY, button)) { return true; }
        if (local >= 129 && local < 146) { var options = ColorSetting.Mode.values(); setting.setMode(options[Math.floorMod(setting.getMode().ordinal() + (button == 0 ? 1 : -1), options.length)]); changed.run(); return true; }
        if (local >= 148 && local < 166) { setting.setDirection(setting.getDirection() == ColorUtil.GradientDirection.HORIZONTAL ? ColorUtil.GradientDirection.VERTICAL : ColorUtil.GradientDirection.HORIZONTAL); changed.run(); return true; }
        for (Slider child : channelSliders) { if (child.mouseClicked(mouseX,mouseY,button)) { capturedChild=child;return true; } }
        for (UIComponent child : new UIComponent[] {speedSlider, spacingSlider}) { if (child.mouseClicked(mouseX, mouseY, button)) { capturedChild = child; return true; } }
        return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (capturedChild != null) { return capturedChild.mouseDragged(mouseX, mouseY, button); }
        if (captured == 0 || button != 0) { return false; } update(mouseX, mouseY); return true;
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean result = captured != 0 || capturedChild != null; captured = 0;
        if (capturedChild != null) { capturedChild.mouseReleased(mouseX, mouseY, button); capturedChild = null; } return result;
    }
    private void commitHex() {
        try { write(ColorSetting.parseHex(hex.getValue())); hex.validation(""); readColor(); }
        catch (IllegalArgumentException ignored) { hex.validation("Use RRGGBB or AARRGGBB"); }
    }
    public boolean charTyped(CharacterEvent event) { if (!hex.charTyped(event)) { return false; } commitHex(); return true; }
    public boolean keyPressed(KeyEvent event, Supplier<String> clipboard, Consumer<String> copy) {
        if (!hex.isFocused()) { return false; }
        if (event.isConfirmation()) { commitHex(); hex.setFocused(false); hex.setValue(ColorSetting.hex(current())); return true; }
        if (hex.keyPressed(event, clipboard, copy)) { commitHex(); return true; } return false;
    }
}
