package dev.riserecode.ui.component;

import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.font.CustomTextRenderer;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** Scrollable two-column visual controls, with independent named theme profiles and live preview. */
public final class ThemeEditor extends UIComponent {
    private record Control(ThemeManager.Control model, UIComponent view) { }
    private final ThemeManager manager;
    private final Map<String, List<Control>> groups = new LinkedHashMap<>();
    private final List<ActionButton> actions = new ArrayList<>();
    private final TextField name = new TextField(() -> "Theme name");
    private final Animation scroll = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING, 0, 0.2, MathUtil.Easing.OUT_QUART);
    private UIComponent captured;
    private double maxScroll;
    private String pendingReveal;
    public ThemeEditor(ThemeManager manager, Consumer<String> notice) {
        this.manager = manager; name.setValue("default");
        for (ThemeManager.Control model : manager.getControls()) {
            var setting = model.setting(); UIComponent view;
            if (setting.getName().equals("Appearance Preset")) { view = new PresetGallery(manager); }
            else if (setting instanceof BooleanSetting bool) { view = new ToggleSwitch(bool, manager::changed); }
            else if (setting instanceof dev.riserecode.setting.StringSetting text) { view = new dev.riserecode.ui.component.setting.StringControl(text, manager::changed); }
            else if (setting instanceof DoubleSetting number) { view = new Slider(number, manager::changed, model.step()); }
            else if (setting instanceof EnumSetting<?> option) { view = enumView(option, manager::changed); }
            else if (setting instanceof ColorSetting color) { view = new ColorPicker(color, manager::changed); }
            else { throw new IllegalArgumentException("Missing theme control " + setting.getName()); }
            groups.computeIfAbsent(model.group(), ignored -> new ArrayList<>()).add(new Control(model, view));
        }
        actions.add(new ActionButton("Save", () -> {
            try { notice.accept(manager.saveProfile(name.getValue()) ? "Theme saved" : "Could not save theme"); }
            catch (IllegalArgumentException invalid) { notice.accept(invalid.getMessage()); }
        }));
        actions.add(new ActionButton("Load", () -> {
            try { notice.accept(manager.loadProfile(name.getValue()) ? "Theme loaded" : "Theme not found or invalid"); }
            catch (IllegalArgumentException invalid) { notice.accept(invalid.getMessage()); }
        }));
        actions.add(new ActionButton("Reset", () -> { manager.reset(); notice.accept("Default theme restored"); }));
        actions.add(new ActionButton("Undo", () -> notice.accept(manager.undoPreset() ? "Preset undone" : "No preset to undo")));
        actions.add(new ActionButton("Logo", () -> { dev.riserecode.ui.component.FocusedInput.commit(); try { manager.importLogo(); notice.accept("Logo imported"); } catch (java.io.IOException | RuntimeException invalid) { notice.accept(invalid.getMessage()); } }));
        actions.add(new ActionButton("Fonts", () -> { CustomTextRenderer.close(); notice.accept("Fonts reloaded from resources and local files"); }));
    }
    private static double controlHeight(UIComponent view, double row) { return view instanceof PresetGallery ? 96 : view instanceof dev.riserecode.ui.component.setting.StringControl ? Math.max(32, row) : view instanceof ColorPicker picker ? picker.getPreferredHeight() : row; }
    private static <E extends Enum<E>> UIComponent enumView(EnumSetting<E> option, Runnable changed) { return new EnumSelector<>(option, changed); }
    public UIComponent getControl(String label) {
        return groups.values().stream().flatMap(List::stream).filter(control -> control.model.setting().getName().equals(label))
                .map(Control::view).findFirst().orElseThrow();
    }
    public double getScrollOffset() { return scroll.getValue(); }
    public double getMaxScroll() { return maxScroll; }
    public TextField getProfileField() { return name; }
    public List<ActionButton> getActions() { return List.copyOf(actions); }
    public void reveal(String label) {
        getControl(label); pendingReveal = label;
    }
    @Override public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        double pad = manager.number("Content Padding");
        double actionWidth = 32, actionGap = 3, toolbarWidth = actions.size() * (actionWidth + actionGap);
        name.setBounds(x, y, Math.max(60, width - toolbarWidth - 8), 22); name.render(graphics, font, mouseX, mouseY, seconds, opacity);
        for (int i = 0; i < actions.size(); i++) {
            actions.get(i).setBounds(x + width - toolbarWidth + i * (actionWidth + actionGap), y, actionWidth, 22);
            actions.get(i).render(graphics, font, mouseX, mouseY, seconds, opacity);
        }
        double viewportY = y + 31, viewportHeight = height - 31;
        scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget())));
        double offset = Math.max(0, scroll.update(seconds));
        double top = viewportY - offset;
        RenderUtil.scissorPush(graphics, x, viewportY, width, viewportHeight);
        try {
            for (var entry : groups.entrySet()) {
                String group = entry.getKey(); List<Control> values = entry.getValue();
                double row = Math.max(28, 18 * manager.number("Font Scale") + 12);
                double titleHeight = group.equals("Typography") ? 43 : 29;
                double innerWidth = width - pad * 2, gap = Math.max(6, pad), columnWidth = (innerWidth - gap) / 2;
                double content = 0;
                for (int i = 0; i < values.size(); i += 2) {
                    double rowHeight = controlHeight(values.get(i).view, row);
                    if (i + 1 < values.size() && values.get(i + 1).view instanceof ColorPicker) { rowHeight = Math.max(rowHeight, ((ColorPicker) values.get(i + 1).view).getPreferredHeight()); }
                    content += rowHeight + 3;
                }
                double cardHeight = titleHeight + content + pad;
                boolean cardVisible = top + cardHeight >= viewportY && top < y + height;
                if (cardVisible) {
                RenderUtil.drawRoundedRect(graphics, x, top, width - 4, cardHeight, 5, Theme.opacity(Theme.SURFACE, opacity));
                RenderUtil.accentText(graphics, font, group, x + pad, top + 8, 0.95, opacity);
                if (group.equals("Typography")) {
                    RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, CustomTextRenderer.status(), (innerWidth - 4) / 0.68),
                            x + pad, top + 26, 0.68, Theme.opacity(Theme.SECONDARY, opacity));
                }
                if (group.equals("Branding")) { dev.riserecode.ui.theme.BrandingLogo.draw(graphics,x+width-32,top+5,20,opacity); }
                }
                double controlY = top + titleHeight;
                for (int i = 0; i < values.size(); i += 2) {
                    double rowHeight = controlHeight(values.get(i).view, row);
                    if (i + 1 < values.size() && values.get(i + 1).view instanceof ColorPicker) { rowHeight = Math.max(rowHeight, ((ColorPicker) values.get(i + 1).view).getPreferredHeight()); }
                    for (int column = 0; column < 2 && i + column < values.size(); column++) {
                        UIComponent control = values.get(i + column).view;
                        control.setBounds(x + pad + column * (columnWidth + gap), controlY, control instanceof PresetGallery ? innerWidth - 4 : columnWidth - 4, rowHeight);
                        if (controlY + rowHeight >= viewportY && controlY < y + height) { control.render(graphics, font, mouseY < viewportY || mouseY >= y + height ? -1e6 : mouseX,
                                mouseY < viewportY || mouseY >= y + height ? -1e6 : mouseY, seconds, opacity); }
                    }
                    controlY += rowHeight + 3;
                }
                top += cardHeight + pad;
            }
            maxScroll = Math.max(0, top + offset - viewportY - viewportHeight);
        } finally { RenderUtil.scissorPop(graphics); }
        if (pendingReveal != null) {
            double desired = getControl(pendingReveal).getBounds().y() - y - 79 + scroll.getValue();
            scroll.animateTo(Math.max(0, Math.min(maxScroll, desired))); pendingReveal = null;
        }
        if (maxScroll > 0) {
            double thumbHeight = Math.max(18, viewportHeight * viewportHeight / (viewportHeight + maxScroll));
            double thumbY = viewportY + MathUtil.clamp01(offset / maxScroll) * (viewportHeight - thumbHeight);
            RenderUtil.drawRoundedRect(graphics, x + width - 2, thumbY, 1.5, thumbHeight, 0.75,
                    Theme.opacity(Theme.accent(), opacity * 0.65));
        }
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        groups.values().forEach(group -> group.forEach(control -> { if (control.view instanceof ColorPicker picker) { picker.pointerFocus(mouseX, mouseY); } }));
        if (!contains(mouseX, mouseY)) { name.setFocused(false); return false; }
        if (name.mouseClicked(mouseX, mouseY, button)) { return true; }
        for (ActionButton action : actions) { if (action.mouseClicked(mouseX, mouseY, button)) { return true; } }
        if (mouseY < y + 31) { return true; }
        for (var group : groups.values()) {
            for (Control control : group) {
                if (control.view.mouseClicked(mouseX, mouseY, button)) { captured = control.view; return true; }
            }
        }
        return true;
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button) { return captured != null && captured.mouseDragged(mouseX, mouseY, button); }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; } boolean result = captured.mouseReleased(mouseX, mouseY, button); captured = null; return result;
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (!contains(mouseX, mouseY)) { return false; }
        mouseReleased(0, 0, 0); scroll.animateTo(Math.max(0, Math.min(maxScroll, scroll.getTarget() - vertical * 32))); return true;
    }
    public void unfocus() { groups.values().forEach(group -> group.forEach(c -> { if (c.view instanceof ColorPicker picker) { picker.unfocus(); } })); name.setFocused(false); mouseReleased(0, 0, 0); }
    public boolean charTyped(CharacterEvent event) { return name.charTyped(event) || groups.values().stream().flatMap(List::stream).anyMatch(c -> c.view instanceof ColorPicker picker && picker.charTyped(event)); }
    public boolean keyPressed(KeyEvent event, Supplier<String> clipboard, Consumer<String> copy) { return name.keyPressed(event, clipboard, copy) || groups.values().stream().flatMap(List::stream).anyMatch(c -> c.view instanceof ColorPicker picker && picker.keyPressed(event, clipboard, copy)); }
}
