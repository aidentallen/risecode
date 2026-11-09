package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.component.TextField;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Block-coordinate editor; incomplete/invalid text never replaces the last valid value. */
public final class NumericField extends UIComponent {
    private final DoubleSetting setting;
    private final TextField field = new TextField(() -> "Block coordinate");
    private double original;
    public NumericField(DoubleSetting setting, Runnable changed) {
        this.setting = setting;field.accessibleName(()->setting.getName()+", "+setting.getDescription()+", integer range "+setting.getMin()+" to "+setting.getMax());field.inlineValidation(false);field.contextBounds(this::getBounds);
        field.onChange(text -> {
            try {
                if (!text.matches("[+-]?[0-9]+")) { throw new IllegalArgumentException(); }
                double value = Double.parseDouble(text);
                setting.setValue(value); changed.run(); field.validation("");
            } catch (IllegalArgumentException invalid) { field.validation("Integer: " + (long)setting.getMin() + " to " + (long)setting.getMax()); }
        });
        field.beforeFocus(() -> original = setting.getValue());
        field.onCancel(() -> { setting.setValue(original);field.setValue(Long.toString((long)original));field.validation("");changed.run(); });
        field.onCommit(() -> {if(field.validation().isEmpty()){field.setValue(Long.toString(setting.getValue().longValue()));}});
    }
    public TextField field() { return field; }
    public void beginEditing(){if(!field.isFocused()&&field.validation().isEmpty()){original=setting.getValue();field.setValue(Long.toString((long)original));}field.setFocused(true);field.selectAll();}
    @Override public void render(GuiGraphicsExtractor g, Font font, double mx, double my, double dt, double alpha) {
        RenderUtil.text(g,font,setting.getName(),x,y,.8,Theme.opacity(Theme.SECONDARY,alpha));
        double labelHeight=dev.riserecode.ui.component.SettingMetrics.labelHeight();
        field.setBounds(x,y+labelHeight,width,dev.riserecode.ui.component.SettingMetrics.fieldHeight());
        if (!field.isFocused()&&field.validation().isEmpty()) { field.setValue(Long.toString(setting.getValue().longValue())); }
        RenderUtil.drawSurfaceTransition(g,x,field.getBounds().y(),width,field.getBounds().height(),3,updateHover(mx,my,dt),alpha);
        field.render(g,font,mx,my,dt,alpha);
        dev.riserecode.ui.component.SettingMetrics.validation(g,font,field,alpha);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if (button != 0 || !contains(mx,my)) { return false; }
        if (!field.isFocused()&&field.validation().isEmpty()) { original=setting.getValue(); field.setValue(Long.toString((long)original)); }
        field.setFocused(true); field.selectAll(); return true;
    }
}
