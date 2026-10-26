package dev.riserecode.ui.component;

import dev.riserecode.module.Module;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.setting.ColorPicker;
import dev.riserecode.ui.component.setting.KeybindControl;
import dev.riserecode.Client;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;
import dev.riserecode.ui.component.setting.EnumSelector;
import dev.riserecode.ui.component.setting.Slider;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A compact card; modules own behavior, this adapter owns all presentation. */
public final class ModuleButton extends UIComponent {
    public static final double HEADER_HEIGHT = 40;
    public record SettingControl(Setting<?> setting, UIComponent component, double height) { }
    private final Module module;
    private final ModuleManager manager;
    private final ModuleAppearanceEditor appearance;
    public ModuleAppearanceEditor appearance(){return appearance;}
    private final SettingsSection bindings;

    private final KeybindControl keybind;
    private final dev.riserecode.ui.animation.ButtonFeedback press=new dev.riserecode.ui.animation.ButtonFeedback();
    private final BaritoneControls baritone;
    private final ActionButton glassEditor;
    private final List<SettingControl> controls = new ArrayList<>();
    private final Map<UIComponent, Animation> positions = new IdentityHashMap<>();
    private final Animation enabled;
    private final Animation expansion = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.EXPANSION, 0, 0.24, MathUtil.Easing.OUT_QUART);
    private final Animation bodyHeight = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.EXPANSION, 0, 0.2, MathUtil.Easing.OUT_QUART);
    private long entered=System.nanoTime();
    private double visualScale=1;
    public void enter(){entered=System.nanoTime();}
    private double localX(double pointer){return x+width/2+(pointer-x-width/2)/visualScale;}
    private double localY(double pointer){return y+height/2+(pointer-y-height/2)/visualScale;}
    @Override public boolean contains(double mx,double my){return super.contains(localX(mx),localY(my));}
    private boolean expanded;
    private boolean showCategory;
    private UIComponent captured;

    public ModuleButton(Module module, ModuleManager manager, Runnable changed) {
        this.module = module; this.manager = manager;
        appearance = new ModuleAppearanceEditor(Client.INSTANCE.getThemeManager(),module);
        bindings = new SettingsSection("module:"+module.capabilityId()+":bindings","Keybind",()->{cancelKeyCapture();dev.riserecode.ui.component.FocusedInput.commit();});

        glassEditor=module instanceof dev.riserecode.module.modules.GlassModule glass?new ActionButton("Customize / Preview",()->{FocusedInput.commit();var mc=net.minecraft.client.Minecraft.getInstance();mc.gui.setScreen(new GlassEditorScreen(glass,mc.gui.screen()));}):null;
        baritone = module instanceof dev.riserecode.module.modules.BaritoneModule b ? new BaritoneControls(b) : null;
        keybind = new KeybindControl(module, Client.INSTANCE.getKeybindManager());
        enabled = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.TOGGLES, module.isEnabled() ? 1 : 0, 0.2, MathUtil.Easing.IN_OUT_CUBIC);
        for (Setting<?> setting : module.getSettings()) {
            if(glassEditor!=null){continue;}
            if(setting==module.popIn||setting==module.popOverride||setting==module.popTiming||setting==module.popStrength){continue;}
            UIComponent component;
            double controlHeight;
            if (setting instanceof BooleanSetting bool) { component = new ToggleSwitch(bool, changed); controlHeight = 15; }
            else if (setting instanceof dev.riserecode.setting.StringSetting text) { component = new dev.riserecode.ui.component.setting.StringControl(text, changed); controlHeight = 32; }
            else if (setting instanceof DoubleSetting number && module instanceof dev.riserecode.module.modules.BaritoneModule && setting.getName().startsWith("Destination ")) {
                component = new dev.riserecode.ui.component.setting.NumericField(number, changed); controlHeight = 32;
            }
            else if (setting instanceof DoubleSetting number) { component = new Slider(number, changed, module instanceof dev.riserecode.module.modules.CapePhysicsModule cape && number == cape.strength() ? 0.01 : 0); controlHeight = 24; }
            else if (setting instanceof EnumSetting<?> option) { component = enumControl(option, changed); controlHeight = 15; }
            else if (setting instanceof dev.riserecode.setting.BlockListSetting blocks) { component = new dev.riserecode.ui.component.setting.BlockListControl(blocks, changed); controlHeight = 20; }
            else if (setting instanceof ColorSetting color) { component = new ColorPicker(color, changed, true); controlHeight = 22; }
            else { throw new IllegalArgumentException("No UI adapter for " + setting.getClass().getName()); }
            controls.add(new SettingControl(setting, component, controlHeight));
            positions.put(component, Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.EXPANSION, 0, 0.2, MathUtil.Easing.OUT_QUART));
        }
        var coordinates=controls.stream().map(SettingControl::component).filter(dev.riserecode.ui.component.setting.NumericField.class::isInstance).map(dev.riserecode.ui.component.setting.NumericField.class::cast).toList();
        for(int i=0;i<coordinates.size();i++){final int index=i;coordinates.get(i).field().onKey(event->{if(event.key()!=258){return false;}coordinates.get(index).field().commit();coordinates.get(Math.floorMod(index+(event.hasShiftDown()?-1:1),coordinates.size())).beginEditing();return true;});}
    }
    private static <E extends Enum<E>> UIComponent enumControl(EnumSetting<E> setting, Runnable changed) { return new EnumSelector<>(setting, changed); }
    public Module getModule() { return module; }
    public boolean isExpanded() { return expanded; }
    public void setShowCategory(boolean value) { showCategory = value; }
    public List<SettingControl> getSettingControls() { return List.copyOf(controls); }
    public KeybindControl getKeybindControl() { return keybind; }
    public void cancelKeyCapture() { keybind.cancelCapture(); }
    public void update(double deltaSeconds) {
        if(!expanded&&!bodyHeight.isRunning()&&bodyHeight.getValue()==0&&!expansion.isRunning()&&expansion.getValue()==0){return;}
        dev.riserecode.ui.theme.ModuleAppearance.with("",()->updateScoped(deltaSeconds));
    }
    private void updateScoped(double deltaSeconds) {
        expansion.animateTo(expanded ? 1 : 0); expansion.update(deltaSeconds);
        if(!expanded&&!bodyHeight.isRunning()&&bodyHeight.getValue()==0){return;}
        double result = (glassEditor==null?4:44) + appearance.preferredHeight(width-14) + SettingsSection.HEADER+bindings.bodyHeight(KeybindControl.HEIGHT) + (baritone == null ? 0 : 58)
                + (module instanceof dev.riserecode.module.modules.ChunkAnimationsModule ? 13 : 0);
        for (SettingControl control : controls) { if (control.setting().isVisible()) { result += controlHeight(control); } }
        bodyHeight.animateTo(expanded ? result : 0); bodyHeight.update(deltaSeconds);
        // Visibility changes release a captured slider before it can edit a hidden setting.
        if (captured != null && controls.stream().anyMatch(c -> c.component() == captured && !c.setting().isVisible())) {
            captured.mouseReleased(0, 0, 0); captured = null;
        }
    }
    private static double controlHeight(SettingControl control) {
        return control.component() instanceof ColorPicker picker ? picker.getPreferredHeight() : control.height();
    }
    public void pointerFocus(double mouseX, double mouseY) {
        mouseX=localX(mouseX);mouseY=localY(mouseY);
        final double pointerX=mouseX,pointerY=mouseY;
        controls.forEach(c -> { if (c.component() instanceof ColorPicker picker) { picker.pointerFocus(pointerX,pointerY); } });
        if (!keybind.getBindingButton().contains(mouseX, mouseY)) { keybind.cancelCapture(); }
    }
    public boolean charTyped(CharacterEvent event) { if(expanded&&appearance.charTyped(event)){return true;}return expanded && controls.stream().anyMatch(c -> c.setting().isVisible() && c.component() instanceof ColorPicker picker && picker.charTyped(event)); }
    public boolean keyPressed(KeyEvent event, Supplier<String> clipboard, Consumer<String> copy) { if(expanded&&appearance.keyPressed(event,clipboard,copy)){return true;}return expanded && controls.stream().anyMatch(c -> c.setting().isVisible() && c.component() instanceof ColorPicker picker && picker.keyPressed(event, clipboard, copy)); }
    public double getPreferredHeight() { return HEADER_HEIGHT + bodyHeight.getValue(); }
    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        dev.riserecode.ui.theme.ModuleAppearance.with("",()->renderScoped(graphics,font,mouseX,mouseY,seconds,opacity));
    }
    private void renderScoped(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY, double seconds, double opacity) {
        double progress=dev.riserecode.ui.animation.PopIn.progress((System.nanoTime()-entered)*1e-9,dev.riserecode.ui.animation.PopIn.windowDuration("Client"));visualScale=dev.riserecode.ui.animation.PopIn.windowScale("Client",progress)*press.scale();
        graphics.pose().pushMatrix();graphics.pose().translate((float)(x+width/2),(float)(y+height/2));graphics.pose().scale((float)visualScale,(float)visualScale);graphics.pose().translate((float)(-x-width/2),(float)(-y-height/2));
        try{renderCard(graphics,font,localX(mouseX),localY(mouseY),seconds,opacity*progress);}finally{graphics.pose().popMatrix();}
    }
    private void renderCard(GuiGraphicsExtractor graphics,Font font,double mouseX,double mouseY,double seconds,double opacity){
        hover.animateTo(mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEADER_HEIGHT ? 1 : 0);
        double hovered = hover.update(seconds);
        enabled.animateTo(module.isEnabled() ? 1 : 0);
        double active = enabled.update(seconds);
        RenderUtil.drawSurfaceTransition(graphics, x, y, width, height, Theme.BUTTON_RADIUS, hovered * 0.65, opacity);

        String badge = module.classification().label();
        double badgeWidth = RenderUtil.textWidth(font,badge,false)*.62+8;
        RenderUtil.drawRoundedRect(graphics,x+width-badgeWidth-7,y+7,badgeWidth,12,3,Theme.opacity(Theme.HOVER,opacity));
        RenderUtil.text(graphics,font,badge,x+width-badgeWidth-3,y+9,.62,Theme.opacity(Theme.SECONDARY,opacity));
        String name = Theme.displayName(module.getName());
        double left = x + 7 + hovered * 0.7 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER);
        if (active > 0.01) {
            RenderUtil.accentText(graphics, font, RenderUtil.ellipsize(font, name, width - badgeWidth - 26), left, y + 7, Math.min(1,15/(9*Theme.number("Font Scale",1))), opacity * MathUtil.clamp01(active));
        }
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, name, width - badgeWidth - 26), left, y + 7, Math.min(1,15/(9*Theme.number("Font Scale",1))),
                Theme.opacity(Theme.TEXT, opacity * (1 - MathUtil.clamp01(active))));
        if (showCategory) {
            String category = "(" + Theme.label(module.getCategory().getDisplayName()) + ")";
            double categoryX = left + RenderUtil.textWidth(font, name, false) + 5;
            if (categoryX + RenderUtil.textWidth(font, category, false) * 0.75 < x + width - 6) {
                RenderUtil.text(graphics, font, category, categoryX, y + 9, 0.75, Theme.opacity(Theme.SECONDARY, opacity * 0.7));
            }
        }
        RenderUtil.text(graphics, font, RenderUtil.ellipsize(font, mouseX>=x+width-badgeWidth-7 && mouseX<x+width-7 && mouseY>=y+7 && mouseY<y+22 ? module.classification().description() : expanded ? details(module) : module.getDescription(), (width - 14) / 0.8),
                x + 7, y + 25, Math.min(.8,10/(9*Theme.number("Font Scale",1))), Theme.opacity(Theme.SECONDARY, opacity * 0.8));
        if (height - HEADER_HEIGHT <= 0.1) { return; }
        RenderUtil.scissorPush(graphics, x + 3, y + HEADER_HEIGHT, width - 6, height - HEADER_HEIGHT - 2);
        try {
            double top = 0;
            if(glassEditor!=null){glassEditor.setBounds(x+7,y+HEADER_HEIGHT,width-14,26);glassEditor.render(graphics,font,mouseX,mouseY,seconds,opacity*expansion.getValue());RenderUtil.text(graphics,font,RenderUtil.ellipsize(font,((dev.riserecode.module.modules.GlassModule)module).status(),(width-14)/.6),x+7,y+HEADER_HEIGHT+29,.6,Theme.opacity(Theme.SECONDARY,opacity*expansion.getValue()));top+=40;}
            for (SettingControl control : controls) {
                if (!control.setting().isVisible()) { continue; }
                Animation position = positions.get(control.component()); position.animateTo(top);
                control.component().setBounds(x + 7, y + HEADER_HEIGHT + position.update(seconds), width - 14, controlHeight(control));
                control.component().render(graphics, font, mouseX, mouseY, seconds, opacity * expansion.getValue());
                top += controlHeight(control);
            }
            if (baritone != null) { baritone.setBounds(x + 7, y + HEADER_HEIGHT + top, width - 14, 58); baritone.render(graphics, font, mouseX, mouseY, seconds, opacity * expansion.getValue()); top += 58; }

            if (module instanceof dev.riserecode.module.modules.ChunkAnimationsModule chunks) {
                RenderUtil.text(graphics,font,RenderUtil.ellipsize(font,chunks.status(),(width-14)/0.6),x+7,y+HEADER_HEIGHT+top,0.6,Theme.opacity(Theme.SECONDARY,opacity*expansion.getValue())); top+=13;
            }
            appearance.setBounds(x+7,y+HEADER_HEIGHT+top,width-14,appearance.preferredHeight(width-14));
            appearance.render(graphics,font,mouseX,mouseY,seconds,opacity*expansion.getValue());top+=appearance.getBounds().height();
            bindings.setBounds(x+7,y+HEADER_HEIGHT+top,width-14,SettingsSection.HEADER);bindings.render(graphics,font,mouseX,mouseY,seconds,opacity*expansion.getValue());top+=SettingsSection.HEADER;
            double bindingHeight=bindings.bodyHeight(KeybindControl.HEIGHT);
            if(bindingHeight>.01){RenderUtil.scissorPush(graphics,x+7,y+HEADER_HEIGHT+top,width-14,bindingHeight);try{
                keybind.setBounds(x + 7, y + HEADER_HEIGHT + top, width - 14, KeybindControl.HEIGHT);
                keybind.render(graphics, font, bindings.isExpanded()?mouseX:-1e6,bindings.isExpanded()?mouseY:-1e6, seconds, opacity * expansion.getValue());
            }finally{RenderUtil.scissorPop(graphics);}}
        } finally { RenderUtil.scissorPop(graphics); }
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {return dev.riserecode.ui.theme.ModuleAppearance.call("",()->clickScoped(mouseX,mouseY,button));}
    private boolean clickScoped(double mouseX, double mouseY, int button) {
        pointerFocus(mouseX, mouseY);double rawX=mouseX,rawY=mouseY;
        mouseX=localX(mouseX);mouseY=localY(mouseY);
        if (!super.contains(mouseX, mouseY)||(button!=0&&button!=1)) { return false; }
        if (mouseY < y + HEADER_HEIGHT) {
            press.activate();dev.riserecode.ui.sound.UiSounds.play();
            if (button == 1) {
                expanded = !expanded;
                if (!expanded) { cancelKeyCapture();appearance.unfocus();dev.riserecode.ui.component.FocusedInput.commit(); mouseReleased(rawX, rawY, 0); }
            } else if (button == 0) { manager.toggle(module); }
            return true;
        }
        if (!expanded) { return true; }
        if(glassEditor!=null&&glassEditor.mouseClicked(mouseX,mouseY,button)){captured=glassEditor;return true;}
        for (SettingControl control : controls) {
            // Partially revealed rows must not accept input outside the animated card's clip.
            if (control.setting().isVisible() && control.component().getBounds().y() + controlHeight(control) <= y + height + 0.01
                    && control.component().mouseClicked(mouseX, mouseY, button)) {
                captured = control.component(); return true;
            }
        }
        if (baritone != null && baritone.mouseClicked(mouseX, mouseY, button)) { return true; }
        if(appearance.mouseClicked(mouseX,mouseY,button)){captured=appearance;return true;}
        if(bindings.mouseClicked(mouseX,mouseY,button)){return true;}
        if (bindings.isExpanded() && bindings.bodyHeight(KeybindControl.HEIGHT)>=KeybindControl.HEIGHT-.01 && keybind.getBounds().y() + KeybindControl.HEIGHT <= y + height + 0.01 && keybind.mouseClicked(mouseX, mouseY, button)) {
            captured = keybind; return true;
        }
        return true;
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) { return captured != null && captured.mouseDragged(localX(mouseX),localY(mouseY),button); }
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (captured == null) { return false; }
        boolean consumed = captured.mouseReleased(localX(mouseX),localY(mouseY),button); captured = null; return consumed;
    }
    private static String details(Module module) {
        if(module instanceof dev.riserecode.module.modules.NoFallModule clutch)return clutch.status();
        if(module instanceof dev.riserecode.module.modules.BlockAnimationsModule blocks)return blocks.status();
        if(module instanceof dev.riserecode.module.modules.FullBrightModule full&&full.getMode().getValue()==dev.riserecode.module.modules.FullBrightModule.Mode.SHADER_NIGHT_VISION)return full.shaderStatus();
        return module.getDescription();
    }

}
