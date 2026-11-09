package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.render.LiquidGlass;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class Slider extends UIComponent {
    private final DoubleSetting setting;
    private final Runnable changed;
    private final Animation position;
    private boolean dragging;
    private final double step;
    private final dev.riserecode.ui.component.TextField input = new dev.riserecode.ui.component.TextField(() -> "Value");
    private double original;
    private String cachedValue = "";
    private java.util.function.Supplier<String> accessibleName;
    public Slider accessibleName(java.util.function.Supplier<String> name){accessibleName=name;return this;}
    private String contextName(){return accessibleName==null?setting.getName():accessibleName.get();}
    private double cachedNumber = Double.NaN;
    public dev.riserecode.ui.component.TextField input() { return input; }
    public void beginEditing() {
        if (!input.isFocused()&&input.validation().isEmpty()) { original = setting.getValue(); input.setValue(Double.toString(original)); }
        input.setFocused(true); input.selectAll();
    }
    public double value(){return setting.getValue();}
    public String name(){return setting.getName();}
    public void synchronizeExternal(double value){if(Double.compare(setting.getValue(),value)!=0){setting.setValue(value);replaceDraft();}}
    private void replaceDraft(){original=setting.getValue();input.setValue(Double.toString(original));input.validation("");}
    private void adjustTo(double value){setting.setValue(value);replaceDraft();changed.run();}
    public void adjust(int direction){double increment=step>0?step:setting.keyboardIncrement();adjustTo(Math.clamp(setting.getValue()+direction*increment,setting.getMin(),setting.getMax()));}
    private void preview(String text) {
        try {
            if (!text.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) { throw new IllegalArgumentException(); }
            double number = Double.parseDouble(text);
            if (!Double.isFinite(number) || number < setting.getMin() || number > setting.getMax()) { throw new IllegalArgumentException(); }
            setting.setValue(number); changed.run(); input.validation("");
        } catch (IllegalArgumentException invalid) { input.validation(setting.getMin() + " to " + setting.getMax()); }
    }

    public Slider(DoubleSetting setting, Runnable changed) {
        this(setting, changed, 0);
    }
    public Slider(DoubleSetting setting, Runnable changed, double step) {
        this.setting = setting; this.changed = changed; this.step = step;
        input.accessibleName(()->contextName()+", "+setting.getDescription()+", range "+setting.getMin()+" to "+setting.getMax());input.inlineValidation(false);input.contextBounds(this::getBounds);
        input.onKey(event -> {
            if (event.key() != 265 && event.key() != 264) { return false; }
            double increment = (step > 0 ? step : setting.keyboardIncrement()) * (event.hasShiftDown() ? 10 : 1);
            setting.setValue(Math.clamp(setting.getValue() + (event.key()==265?increment:-increment),setting.getMin(),setting.getMax()));input.setValue(Double.toString(setting.getValue()));input.validation("");changed.run(); return true;
        });
        input.beforeFocus(()->{if(input.validation().isEmpty()){original=setting.getValue();input.setValue(Double.toString(original));}});
        input.onChange(this::preview);
        input.onCancel(() -> { setting.setValue(original);input.setValue(Double.toString(original));input.validation("");changed.run(); });
        input.onCommit(() -> {if(input.validation().isEmpty()){input.setValue(Double.toString(setting.getValue()));}});
        position = Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.DRAGGING, normalized(), 0.1, MathUtil.Easing.OUT_QUART);
    }

    private double normalized() {
        return setting.position(setting.getValue());
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Font font, double mouseX, double mouseY,
            double deltaSeconds, double opacity) {
        keyboard(graphics, opacity, () -> contextName() + ", slider, " + setting.getValue()+", "+setting.getDescription()+", range "+setting.getMin()+" to "+setting.getMax(), this::beginEditing, this::adjust);
        double hovered = updateHover(mouseX, mouseY, deltaSeconds);
        position.animateTo(normalized());
        double progress = MathUtil.clamp01(position.update(deltaSeconds));
        if (cachedNumber != setting.getValue()) { cachedNumber = setting.getValue(); double increment=step>0?step:setting.keyboardIncrement();int digits=increment>0?Math.clamp(Math.max(step>0?0:2,(int)Math.ceil(-Math.log10(increment))),0,6):2;cachedValue=java.math.BigDecimal.valueOf(cachedNumber).setScale(digits,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(); }
        String label = Theme.displayName(setting.getName());
        double labelScale=.88;
        String fullLabel=label;label=RenderUtil.ellipsize(font,label,(width-4)/labelScale);
        if(contains(mouseX,mouseY)){graphics.setTooltipForNextFrame(font,java.util.List.of(net.minecraft.network.chat.Component.literal(fullLabel),net.minecraft.network.chat.Component.literal(setting.getDescription())),java.util.Optional.empty(),(int)mouseX,(int)mouseY);}
        RenderUtil.text(graphics, font, label,
                x + hovered * 0.7 * Animation.movement(dev.riserecode.ui.theme.ThemeManager.Channel.HOVER), y + 1, labelScale, Theme.opacity(Theme.TEXT, opacity));
        input.setBounds(x,y+dev.riserecode.ui.component.SettingMetrics.labelHeight(),width,dev.riserecode.ui.component.SettingMetrics.fieldHeight());
        if(!input.isFocused()&&input.validation().isEmpty()&&!input.getValue().equals(cachedValue)){input.setValue(cachedValue);}
        RenderUtil.drawRoundedRect(graphics,input.getBounds().x(),input.getBounds().y(),width,input.getBounds().height(),3,Theme.opacity(Theme.SURFACE,opacity));
        input.render(graphics,font,mouseX,mouseY,deltaSeconds,opacity);
        dev.riserecode.ui.component.SettingMetrics.validation(graphics,font,input,opacity);
        double trackX = x + 5;
        double trackWidth = width - 10;
        double trackY = y + height - 6;
        RenderUtil.drawRoundedRect(graphics, trackX, trackY, trackWidth, 1.5, 0.75,
                Theme.opacity(Theme.MUTED, opacity * 0.45));
        if (progress > 0.001) {
            RenderUtil.drawRoundedRect(graphics, trackX, trackY, trackWidth * progress, 1.5, 0.75,
                    Theme.opacity(Theme.accent(), opacity * 0.8));
        }
        double thumb = trackX + trackWidth * progress;
        if(LiquidGlass.enabled()){
            double knobWidth=11+(dragging?2:hovered),knobHeight=7;
            RenderUtil.drawDropShadow(graphics,thumb-knobWidth/2,trackY-2.75,knobWidth,knobHeight,3.5,Theme.opacity(Theme.SHADOW,opacity*.35));
            RenderUtil.drawRoundedRect(graphics,thumb-knobWidth/2,trackY-2.75,knobWidth,knobHeight,3.5,Theme.opacity(0xFFDDEAF3,opacity));
            LiquidGlass.sheen(graphics,thumb-knobWidth/2,trackY-2.75,knobWidth,knobHeight,3.5,opacity);
            return;
        }
        double size = 4 + hovered + (dragging ? 0.5 : 0);
        RenderUtil.drawGlow(graphics, thumb - size / 2, trackY - size / 2 + 0.75, size, size, size / 2,
                Theme.opacity(Theme.accent(), opacity * (0.6 + hovered * 0.4)));
        RenderUtil.drawRoundedRect(graphics, thumb - size / 2, trackY - size / 2 + 0.75, size, size, size / 2,
                Theme.opacity(Theme.TEXT, opacity));
    }

    private void setFromMouse(double mouseX) {
        double progress = MathUtil.clamp01((mouseX - x - 5) / Math.max(1, width - 10));
        double value = setting.valueAt(progress);
        if (step > 0) { value = setting.getMin() + Math.rint((value - setting.getMin()) / step) * step; }
        adjustTo(Math.max(setting.getMin(), Math.min(setting.getMax(), value)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) { return false; }
        if (mouseY < y + dev.riserecode.ui.component.SettingMetrics.labelHeight() || input.contains(mouseX,mouseY)) { beginEditing(); return true; }
        if(mouseY<y+height-12){return true;}
        dev.riserecode.ui.component.FocusedInput.commit();
        dragging = true;
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) { return false; }
        setFromMouse(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean consumed = dragging;
        dragging = false;
        return consumed;
    }
}
