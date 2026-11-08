package dev.riserecode.ui.component;

import dev.riserecode.ui.component.setting.*;
import dev.riserecode.ui.theme.Theme;

/** Shared font-aware geometry for measurement, rendering and hit testing. */
public final class SettingMetrics {
    private SettingMetrics() { }
    public static double height(UIComponent view,double base){
        double font=Theme.number("Font Scale",1);
        if(view instanceof ColorPicker picker){return picker.getPreferredHeight();}
        if(view instanceof EnumSelector<?>){return Math.max(26,18*font*.85+10);}
        if(view instanceof Slider){return Math.max(40,9*font*.88+30);}
        if(view instanceof StringControl||view instanceof NumericField){return Math.max(48,9*font*.8+5+Math.max(20,9*font+6)+12);}
        return Math.max(base,9*font*.85+10);
    }
}
