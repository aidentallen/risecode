package dev.riserecode.ui.component.setting;

import dev.riserecode.setting.StringSetting;
import dev.riserecode.ui.component.TextField;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class StringControl extends UIComponent {
    private final StringSetting setting;
    private final TextField field;
    private String original;
    public StringControl(StringSetting setting, Runnable changed) {
        this.setting = setting; field = new TextField(() -> setting.getDescription()); field.setMaximum(setting.maximum());
        field.onChange(value -> { try { setting.setValue(value); field.validation(""); changed.run(); } catch (IllegalArgumentException invalid) { field.validation(invalid.getMessage()); } });
        field.beforeFocus(() -> original = setting.getValue());
        field.onCancel(() -> { setting.setValue(original); field.setValue(original); field.validation(""); changed.run(); });
    }
    public TextField field() { return field; }
    @Override public void render(GuiGraphicsExtractor g, Font font, double mx, double my, double delta, double opacity) {
        RenderUtil.text(g, font, Theme.displayName(setting.getName()), x, y, 0.8, Theme.opacity(Theme.SECONDARY, opacity));
        double labelHeight=9*Theme.number("Font Scale",1)*.8+5;
        field.setBounds(x,y+labelHeight,width,Math.max(20,height-labelHeight-12));
        if (!field.isFocused()&&field.validation().isEmpty()) { field.setValue(setting.getValue()); }
        RenderUtil.drawRoundedRect(g, x,field.getBounds().y(),width,field.getBounds().height(), 3, Theme.opacity(Theme.HOVER, opacity));
        field.render(g, font, mx, my, delta, opacity);
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !contains(mx, my)) { return false; }
        if (!field.isFocused()&&field.validation().isEmpty()) { original = setting.getValue(); field.setValue(original); }
        field.setFocused(true); return true;
    }
}
