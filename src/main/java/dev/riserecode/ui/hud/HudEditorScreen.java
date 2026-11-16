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
public final class HudEditorScreen extends Screen implements dev.riserecode.ui.component.KeyboardNavigation.PageScrollable {
    private record Guide(double value, boolean vertical) { }
    private final HudRenderer hud;
    private final dev.riserecode.ui.component.ActionButton done=new dev.riserecode.ui.component.ActionButton("Done",this::onClose);
    public static final String SHORTCUT_HELP="Tab / Shift+Tab: choose a HUD element or setting. Enter: edit the selected element. Ctrl+arrows: move by one pixel. Ctrl+Shift+arrows: move by ten pixels. G: toggle grid snapping. Page Up/Down: scroll the inspector. Escape: save and return.";
    private final dev.riserecode.ui.component.ActionButton help=new dev.riserecode.ui.component.ActionButton("Help",this::openHelp);
    private void openHelp(){minecraft.gui.setScreen(new dev.riserecode.ui.menu.InfoScreen(this,"HUD controls",SHORTCUT_HELP));}
    private final List<Guide> guides = new ArrayList<>();
    private final List<UIComponent> controls = new ArrayList<>();
    private final Animation contextOpening = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.EXPANSION, 0, 0.18, MathUtil.Easing.OUT_QUART);
    private HudRenderer.Box dragging;
    private HudElement selected;
    private dev.riserecode.ui.component.SettingsSection settingsSection;
    private UIComponent captured;
    private double dragX, dragY, panelX, panelY, panelHeight, contextMaxScroll;
    private final Animation contextScroll = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.SCROLLING, 0, 0.15, MathUtil.Easing.OUT_QUART);
    private final net.minecraft.client.gui.screens.Screen parent;
    private long previousFrame;
    private dev.riserecode.setting.DoubleSetting positionX,positionY;
    private Slider positionXControl,positionYControl;
    private double contextTop,contextVisibleHeight,panelWidth;
    public HudEditorScreen(HudRenderer hud) { this(hud,null); }
    public HudEditorScreen(HudRenderer hud, net.minecraft.client.gui.screens.Screen parent) { super(Component.literal("risecode HUD editor")); this.hud = hud; this.parent=parent; }
    public dev.riserecode.ui.component.SettingsSection getSettingsSection(){return settingsSection;}
    public HudElement getSelected() { return selected; }
    public boolean isDraggingElement() { return dragging != null; }
    public UIComponent.Bounds getContextViewportBounds(){return new UIComponent.Bounds(panelX,contextTop,panelWidth,contextVisibleHeight);}
    public List<UIComponent> getControls() { return List.copyOf(controls); }
    private void changed() { Client.INSTANCE.getConfigManager().requestSave(); }
    private void select(HudElement element) {
        selected = element;
        settingsSection=new dev.riserecode.ui.component.SettingsSection("hud:"+element.id()+":layout","Layout & Style",()->{if(captured!=null){captured.mouseReleased(0,0,0);captured=null;}controls.forEach(c->{if(c instanceof ColorPicker picker){picker.unfocus();}});});
        controls.clear(); contextOpening.reset(0); contextOpening.animateTo(1);
        controls.add(new Slider(element.layout().scale, this::changed, 0.01));
        positionX = new dev.riserecode.setting.DoubleSetting("Position X", "Normalized anchor", element.layout().x(), 0, 1);
        positionY = new dev.riserecode.setting.DoubleSetting("Position Y", "Normalized anchor", element.layout().y(), 0, 1);
        positionXControl=new Slider(positionX, () -> { element.layout().position(positionX.getValue(), element.layout().y()); changed(); }, 0.001);controls.add(positionXControl);
        positionYControl=new Slider(positionY, () -> { element.layout().position(element.layout().x(), positionY.getValue()); changed(); }, 0.001);controls.add(positionYControl);
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
            dev.riserecode.ui.component.KeyboardNavigation.control(g, box.element(), new UIComponent.Bounds(box.x(),box.y(),box.width(),box.height()),1,
                    () -> "HUD element, " + box.element().label() + ", activate for layout and style", () -> select(box.element()), null);
            int color = box.element() == selected ? Theme.accent() : Theme.opacity(Theme.SECONDARY, 0.6);
            RenderUtil.drawRoundedBorder(g, box.x() - 2, box.y() - 2, box.width() + 4, box.height() + 4, 2, color, 0.8);
            if(mouseX>=box.x()&&mouseX<box.x()+box.width()&&mouseY>=box.y()&&mouseY<box.y()+box.height()){g.setTooltipForNextFrame(font,java.util.List.of(Component.literal(box.element().label()+" · click to edit")),java.util.Optional.empty(),mouseX,mouseY);}
        }
        for (Guide guide : guides) {
            if (guide.vertical()) { RenderUtil.line(g, guide.value(), 0, guide.value(), height, 0.8, Theme.opacity(Theme.accent(), 0.7)); }
            else { RenderUtil.line(g, 0, guide.value(), width, guide.value(), 0.8, Theme.opacity(Theme.accent(), 0.7)); }
        }
        RenderUtil.drawRoundedRect(g,width-194,4,190,30,6,Theme.BACKGROUND|0xFF000000);
        String hint = (selected==null?"HUD":selected.label())+" · Escape: return";
        hint=RenderUtil.ellipsize(font,hint,(width-40)/.8);
        double hintWidth = RenderUtil.textWidth(font, hint, false) * .8 + 22;
        RenderUtil.drawRoundedRect(g, (width - hintWidth) / 2, height - 26, hintWidth, 21, 6, 0xDF111820);
        RenderUtil.text(g, font, hint, (width - hintWidth) / 2 + 11, height - 19, .8, Theme.TEXT);
        if (selected != null && !controls.isEmpty()) {
            synchronizePositionControls();
            panelWidth=Math.min(210+40*Math.max(0,Theme.number("Font Scale",1)-1),width-16);
            double titleHeight=9*Theme.number("Font Scale",1)+14,sectionHeight=Math.max(dev.riserecode.ui.component.SettingsSection.HEADER,9*Theme.number("Font Scale",1)*.85+10);
            double total=controls.stream().mapToDouble(HudEditorScreen::controlHeight).sum(),body=settingsSection.bodyHeight(total);
            panelHeight=Math.min(Math.max(24,height-66),titleHeight+sectionHeight+body+8);
            panelX=Math.max(8,width-panelWidth-12);panelY=40;
            contextTop=panelY+titleHeight+sectionHeight;
            contextVisibleHeight=Math.max(0,panelHeight-titleHeight-sectionHeight-8);
            contextMaxScroll=Math.max(0,total-contextVisibleHeight);
            if(contextScroll.getValue()>contextMaxScroll){contextScroll.reset(contextMaxScroll);}
            contextScroll.animateTo(Math.clamp(contextScroll.getTarget(),0,contextMaxScroll));
            double opening=MathUtil.clamp01(contextOpening.getValue());
            RenderUtil.drawDropShadow(g,panelX,panelY,panelWidth,panelHeight,8,Theme.opacity(0x70000000,opening));
            RenderUtil.drawRoundedRect(g,panelX,panelY,panelWidth,panelHeight,8,Theme.opacity(Theme.BACKGROUND|0xFF000000,opening*.99));
            RenderUtil.accentText(g,font,RenderUtil.ellipsize(font,selected.label(),panelWidth-20),panelX+10,panelY+8,1,opening);
            settingsSection.setBounds(panelX+6,panelY+titleHeight,panelWidth-12,sectionHeight);settingsSection.render(g,font,mouseX,mouseY,seconds,opening);
            if(body>.01&&contextVisibleHeight>0){
                dev.riserecode.ui.component.KeyboardNavigation.pushScrollRegion(bounds->{double shift=bounds.y()<contextTop?bounds.y()-contextTop:Math.max(0,bounds.y()+bounds.height()-contextTop-contextVisibleHeight);if(shift!=0){contextScroll.reset(Math.clamp(contextScroll.getValue()+shift,0,contextMaxScroll));}});
                RenderUtil.scissorPush(g,panelX,contextTop,panelWidth,contextVisibleHeight);
                try{double cy=contextTop-contextScroll.getValue();for(var control:controls){double h=controlHeight(control);control.setBounds(panelX+10,cy,panelWidth-20,h-5);control.render(g,font,settingsSection.isExpanded()?mouseX:-1e6,settingsSection.isExpanded()?mouseY:-1e6,seconds,opening*settingsSection.amount(total));cy+=h;}}
                finally{RenderUtil.scissorPop(g);dev.riserecode.ui.component.KeyboardNavigation.popScrollRegion();}
            }

        }
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        dev.riserecode.ui.component.FocusedInput.pointer(event.x(),event.y());
        controls.forEach(control -> { if (control instanceof ColorPicker picker) { picker.pointerFocus(event.x(), event.y()); } });
        if (selected != null && event.x() >= panelX && event.x() <= panelX + panelWidth && event.y() >= panelY && event.y() <= panelY + panelHeight) {
            if(settingsSection.mouseClicked(event.x(),event.y(),event.button())){return true;}
            if(!settingsSection.isExpanded()){return true;}
            if(event.y()<contextTop||event.y()>=contextTop+contextVisibleHeight){return true;}
            for (UIComponent control : controls) { if (control.mouseClicked(event.x(), event.y(), event.button())) { captured = control; return true; } }
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
        selected.layout().position(x / width, y / height);synchronizePositionControls();changed(); return true;
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
    public boolean nudge(KeyEvent event){
        if(selected==null||!event.hasControlDown()||event.key()<262||event.key()>265||dev.riserecode.ui.component.FocusedInput.active()){return false;}
        double step=event.hasShiftDown()?10:1;
        double dx=event.key()==262?step:event.key()==263?-step:0,dy=event.key()==264?step:event.key()==265?-step:0;
        selected.layout().position(selected.layout().x()+dx/Math.max(1,width),selected.layout().y()+dy/Math.max(1,height));synchronizePositionControls();changed();return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if(nudge(event)){return true;}
        if (dev.riserecode.ui.component.FocusedInput.key(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard)) { return true; }
        if (event.isEscape()) { onClose(); return true; }
        if (controls.stream().anyMatch(control -> control instanceof ColorPicker picker && picker.keyPressed(event, minecraft.keyboardHandler::getClipboard, minecraft.keyboardHandler::setClipboard))) { return true; }

        if (event.key() == 71) { hud.getSnap().setValue(!hud.getSnap().getValue()); changed(); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public void removed() { dev.riserecode.ui.component.FocusedInput.commit(); Client.INSTANCE.getConfigManager().save(); Client.INSTANCE.getProfileManager().saveActiveHudLayout(); }
    @Override public boolean isPauseScreen() { return true; }
    @Override public boolean charTyped(CharacterEvent event){if(dev.riserecode.ui.component.FocusedInput.character(event)){return true;}return controls.stream().anyMatch(control->control instanceof ColorPicker picker&&picker.charTyped(event));}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(selected==null||settingsSection==null||!settingsSection.isExpanded()||x<panelX||x>panelX+panelWidth||y<contextTop||y>contextTop+contextVisibleHeight){return false;}
        contextScroll.animateTo(Math.clamp(contextScroll.getTarget()-vertical*28,0,contextMaxScroll));return true;
    }
    private void synchronizePositionControls(){if(selected!=null&&positionXControl!=null){positionXControl.synchronizeExternal(selected.layout().x());positionYControl.synchronizeExternal(selected.layout().y());}}
    @Override public void scrollPage(int direction){dev.riserecode.ui.component.FocusedInput.commit();contextScroll.animateTo(Math.clamp(contextScroll.getTarget()+direction*Math.max(40,contextVisibleHeight*.8),0,contextMaxScroll));}
    private static double controlHeight(UIComponent control){return dev.riserecode.ui.component.SettingMetrics.height(control,28)+5;}
}
