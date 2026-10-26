package dev.riserecode.ui.hud;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.ColorPicker;
import net.minecraft.client.input.CharacterEvent;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Original drag/snap editor using the same live element registry as gameplay. */
public final class HudEditorScreen extends Screen {
    private record Guide(double value, boolean vertical) { }
    private final HudRenderer hud;
    private final List<Guide> guides = new ArrayList<>();
    private final List<UIComponent> controls = new ArrayList<>();
    private final Animation contextOpening = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.EXPANSION, 0, 0.18, MathUtil.Easing.OUT_QUART);
    private HudRenderer.Box dragging;
    private HudElement selected;
    private dev.riserecode.ui.component.SettingsSection settingsSection;
    private UIComponent captured;
    private double dragX, dragY, panelX, panelY, panelHeight, contextMaxScroll;
    private final Animation contextScroll = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING, 0, 0.15, MathUtil.Easing.OUT_QUART);
    private long previousFrame;
    public HudEditorScreen(HudRenderer hud) { super(Component.literal("risecode HUD editor")); this.hud = hud; }
    public dev.riserecode.ui.component.SettingsSection getSettingsSection(){return settingsSection;}
    public HudElement getSelected() { return selected; }
    public boolean isDraggingElement() { return dragging != null; }
    public List<UIComponent> getControls() { return List.copyOf(controls); }
    private void changed() { Client.INSTANCE.getConfigManager().requestSave(); }
    private void select(HudElement element) {
        selected = element;
        settingsSection=new dev.riserecode.ui.component.SettingsSection("hud:"+element.id()+":layout","Layout & Style",()->{if(captured!=null){captured.mouseReleased(0,0,0);captured=null;}controls.forEach(c->{if(c instanceof ColorPicker picker){picker.unfocus();}});});
        controls.clear(); contextOpening.reset(0); contextOpening.animateTo(1);
        controls.add(new Slider(element.layout().scale, this::changed, 0.01));
        var positionX = new dev.riserecode.setting.DoubleSetting("Position X", "Normalized anchor", element.layout().x(), 0, 1);
        var positionY = new dev.riserecode.setting.DoubleSetting("Position Y", "Normalized anchor", element.layout().y(), 0, 1);
        controls.add(new Slider(positionX, () -> { element.layout().position(positionX.getValue(), element.layout().y()); changed(); }, 0.001));
        controls.add(new Slider(positionY, () -> { element.layout().position(element.layout().x(), positionY.getValue()); changed(); }, 0.001));
        controls.add(new EnumSelector<>(element.layout().alignment, this::changed));
        controls.add(new EnumSelector<>(element.layout().background, this::changed));
        controls.add(new EnumSelector<>(element.layout().border, this::changed));
        if (element.module() != null) { controls.add(new ToggleSwitch("Enabled", element.module()::isEnabled,
                value -> { Client.INSTANCE.getModuleManager().setEnabled(element.module(), value); changed(); })); }
        controls.add(new ToggleSwitch(hud.getSnap(), this::changed)); controls.add(new Slider(hud.getGridSize(), this::changed, 1));
        controls.add(new ColorPicker(element.layout().backgroundColor, this::changed, true)); contextScroll.reset(0);
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) { }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        long now = System.nanoTime(); double seconds = previousFrame == 0 ? 0 : (now - previousFrame) * 1e-9; previousFrame = now;
        hud.extractHud(g, true);
        if (dragging != null && hud.getSnap().getValue()) {
            double step = hud.getGridSize().getValue() * g.guiHeight() / 576.0;
            for (double x = 0; x < width; x += step) { RenderUtil.line(g, x, 0, x, height, 0.4, 0x10FFFFFF); }
            for (double y = 0; y < height; y += step) { RenderUtil.line(g, 0, y, width, y, 0.4, 0x10FFFFFF); }
        }
        for (HudRenderer.Box box : hud.getBoxes()) {
            int color = box.element() == selected ? Theme.accent() : Theme.opacity(Theme.SECONDARY, 0.6);
            RenderUtil.drawRoundedBorder(g, box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4, 2, color, 0.8);
            double labelY = box.y() >= 12 ? box.y() - 10 : box.y() + box.height() + 3;
            RenderUtil.text(g, font, box.element().label(), box.x(), Math.min(height - 9, labelY), 0.8, color);
        }
        for (Guide guide : guides) {
            if (guide.vertical()) { RenderUtil.line(g, guide.value(), 0, guide.value(), height, 0.8, Theme.opacity(Theme.accent(), 0.7)); }
            else { RenderUtil.line(g, 0, guide.value(), width, guide.value(), 0.8, Theme.opacity(Theme.accent(), 0.7)); }
        }
        String hint = "HUD Editor  ·  Drag to move  ·  Right click to customize  ·  G: grid  ·  Esc: save";
        double hintWidth = RenderUtil.textWidth(font, hint, false) * 0.9 + 22;
        RenderUtil.drawRoundedRect(g, (width - hintWidth) / 2, height - 26, hintWidth, 21, 6, 0xDF111820);
        RenderUtil.text(g, font, hint, (width - hintWidth) / 2 + 11, height - 19, 0.9, Theme.TEXT);
        if (selected != null && !controls.isEmpty()) {
            double panelWidth = Math.min(190, width - 12), contentHeight = dev.riserecode.ui.component.SettingsSection.HEADER+settingsSection.bodyHeight(controls.stream().mapToDouble(HudEditorScreen::controlHeight).sum());
            panelHeight = Math.min(height - 50, 33 + contentHeight); contextMaxScroll = Math.max(0, contentHeight - (panelHeight - 33));
            contextScroll.animateTo(Math.clamp(contextScroll.getTarget(), 0, contextMaxScroll));
            panelX = Math.max(6, width - panelWidth - 12); panelY = Math.max(8, Math.min(height - panelHeight - 34, 40));
            double opening = MathUtil.clamp01(contextOpening.getValue());
            RenderUtil.drawDropShadow(g, panelX, panelY, panelWidth, panelHeight, 8, Theme.opacity(0x70000000, opening));
            RenderUtil.drawRoundedRect(g, panelX, panelY, panelWidth, panelHeight, 8, Theme.opacity(Theme.BACKGROUND, opening * 0.93));
            RenderUtil.accentText(g, font, selected.label(), panelX + 10, panelY + 9 + (1 - opening) * 4, 1, opening);
            settingsSection.setBounds(panelX+6,panelY+29,panelWidth-12,dev.riserecode.ui.component.SettingsSection.HEADER);settingsSection.render(g,font,mouseX,mouseY,seconds,opening);
            double controlY = panelY + 30 + dev.riserecode.ui.component.SettingsSection.HEADER - contextScroll.getValue();
            RenderUtil.scissorPush(g, panelX, panelY + 29+dev.riserecode.ui.component.SettingsSection.HEADER, panelWidth, Math.max(0,panelHeight - 29-dev.riserecode.ui.component.SettingsSection.HEADER));
            try {
                for (UIComponent control : controls) {
                    control.setBounds(panelX + 10, controlY, panelWidth - 20, controlHeight(control) - 3);
                    control.render(g, font, mouseX, mouseY, seconds, opening); controlY += controlHeight(control);
                }
            } finally { RenderUtil.scissorPop(g); }
        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        dev.riserecode.ui.component.FocusedInput.pointer(event.x(),event.y());
        controls.forEach(control -> { if (control instanceof ColorPicker picker) { picker.pointerFocus(event.x(), event.y()); } });
        if (selected != null && event.x() >= panelX && event.x() <= panelX + 190 && event.y() >= panelY && event.y() <= panelY + panelHeight) {
            if(settingsSection.mouseClicked(event.x(),event.y(),event.button())){return true;}
            if(!settingsSection.isExpanded()){return true;}
            for (UIComponent control : controls) { if (control.getBounds().y()>=panelY+29+dev.riserecode.ui.component.SettingsSection.HEADER&&control.getBounds().y()+control.getBounds().height()<=panelY+panelHeight&&control.mouseClicked(event.x(), event.y(), event.button())) { captured = control; return true; } }
            return true;
        }
        List<HudRenderer.Box> boxes = hud.getBoxes();
        for (int i = boxes.size() - 1; i >= 0; i--) {
            HudRenderer.Box box = boxes.get(i);
            if (box.contains(event.x(), event.y())) {
                select(box.element());
                if (event.button() == 0) { dragging = box; dragX = event.x() - box.x(); dragY = event.y() - box.y(); }
                return true;
            }
        }
        selected = null; controls.clear(); return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (captured != null) { return captured.mouseDragged(event.x(), event.y(), event.button()); }
        if (dragging == null) { return false; }
        double x = event.x() - dragX, y = event.y() - dragY;
        if (hud.getSnap().getValue()) { double grid = hud.getGridSize().getValue() * height / 576.0; x = Math.rint(x / grid) * grid; y = Math.rint(y / grid) * grid; }
        guides.clear();
        x = align(x, dragging.width(), width, true); y = align(y, dragging.height(), height, false);
        x = Math.clamp(x, 0, Math.max(0, width - dragging.width())); y = Math.clamp(y, 0, Math.max(0, height - dragging.height()));
        selected.layout().position(x / width, y / height); changed(); return true;
    }
    private double align(double start, double size, double screen, boolean vertical) {
        List<Double> anchors = new ArrayList<>(List.of(0.0, screen / 2, screen));
        for (HudRenderer.Box box : hud.getBoxes()) {
            if (box.element() == selected) { continue; }
            double p = vertical ? box.x() : box.y(), s = vertical ? box.width() : box.height(); anchors.add(p); anchors.add(p + s / 2); anchors.add(p + s);
        }
        double best = start, distance = 4.5;
        for (double anchor : anchors) {
            for (double offset : new double[] {0, size / 2, size}) {
                double diff = Math.abs(start + offset - anchor);
                if (diff < distance) { distance = diff; best = anchor - offset; }
            }
        }
        double startAligned = best;
        if (best != start) { for (double offset : new double[] {0, size / 2, size}) { if (anchors.stream().anyMatch(v -> Math.abs(v - (startAligned + offset)) < 0.01)) { guides.add(new Guide(best + offset, vertical)); } } }
        return best;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (captured != null) { captured.mouseReleased(event.x(), event.y(), event.button()); captured = null; }
        dragging = null; guides.clear(); changed(); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (dev.riserecode.ui.component.FocusedInput.key(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)) { return true; }
        if (event.isEscape() || event.key() == 344) { onClose(); return true; }
        if (controls.stream().anyMatch(control -> control instanceof ColorPicker picker && picker.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard))) { return true; }
        if (event.key() == 71) { hud.getSnap().setValue(!hud.getSnap().getValue()); changed(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.gui.setScreen(null); }
    @Override public void removed() { dev.riserecode.ui.component.FocusedInput.commit(); Client.INSTANCE.getConfigManager().save(); Client.INSTANCE.getProfileManager().saveActiveHudLayout(); }
    @Override public boolean isPauseScreen() { return true; }
    private static double controlHeight(UIComponent control) { return control instanceof ColorPicker picker ? picker.getPreferredHeight() + 3 : 28; }
    @Override public boolean charTyped(CharacterEvent event) { if (dev.riserecode.ui.component.FocusedInput.character(event)) { return true; } return controls.stream().anyMatch(control -> control instanceof ColorPicker picker && picker.charTyped(event)); }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (selected == null || settingsSection==null||!settingsSection.isExpanded()|| x < panelX || x > panelX + 190 || y < panelY || y > panelY + panelHeight) { return false; }
        contextScroll.animateTo(Math.clamp(contextScroll.getTarget() - vertical * 28, 0, contextMaxScroll)); return true;
    }

}
