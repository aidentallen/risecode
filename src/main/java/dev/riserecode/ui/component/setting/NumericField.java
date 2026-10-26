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
        this.setting = setting;
        field.onChange(text -> {
            try {
                if (!text.matches("[+-]?[0-9]+")) { throw new IllegalArgumentException(); }
                double value = Double.parseDouble(text);
                setting.setValue(value); changed.run(); field.validation("");
            } catch (IllegalArgumentException invalid) { field.validation("Integer: " + (long)setting.getMin() + " to " + (long)setting.getMax()); }
        });
        field.onCancel(() -> { setting.setValue(original); changed.run(); });
        field.onCommit(() -> {field.setValue(Long.toString(setting.getValue().longValue()));field.validation("");});
    }
    public TextField field() { return field; }
    public void beginEditing(){if(!field.isFocused()){original=setting.getValue();field.setValue(Long.toString((long)original));}field.setFocused(true);field.selectAll();}
    @Override public void render(GuiGraphicsExtractor g, Font font, double mx, double my, double dt, double alpha) {
        RenderUtil.text(g,font,setting.getName(),x,y,.8,Theme.opacity(Theme.SECONDARY,alpha));
        field.setBounds(x,y+10,width,Math.max(17,height-10));
        if (!field.isFocused()) { field.setValue(Long.toString(setting.getValue().longValue())); }
        RenderUtil.drawSurfaceTransition(g,x,y+10,width,Math.max(17,height-10),3,updateHover(mx,my,dt),alpha);
        field.render(g,font,mx,my,dt,alpha);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if (button != 0 || !contains(mx,my)) { return false; }
        if (!field.isFocused()) { original=setting.getValue(); field.setValue(Long.toString((long)original)); }
        field.setFocused(true); field.selectAll(); return true;
    }
}
