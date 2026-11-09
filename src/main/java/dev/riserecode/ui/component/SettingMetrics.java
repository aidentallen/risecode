package dev.riserecode.ui.component;

import dev.riserecode.ui.component.setting.*;
import dev.riserecode.ui.theme.Theme;

/** Shared font-aware geometry for measurement, rendering and hit testing. */
public final class SettingMetrics {
    private SettingMetrics() { }
    public static double labelHeight(){return 9*Theme.number("Font Scale",1)*.88+5;}
    public static double fieldHeight(){return Math.max(20,9*Theme.number("Font Scale",1)+6);}
    public static double errorHeight(){return 9*Theme.number("Font Scale",1)*.7+5;}
    public static void validation(net.minecraft.client.gui.GuiGraphicsExtractor g,net.minecraft.client.gui.Font font,TextField field,double opacity){
        if(!field.validation().isEmpty()){var b=field.getBounds();dev.riserecode.util.RenderUtil.text(g,font,dev.riserecode.util.RenderUtil.ellipsize(font,field.validation(),b.width()/.7),b.x(),b.y()+b.height()+2,.7,Theme.opacity(0xFFFF7788,opacity));}
    }
    public static double height(UIComponent view,double base){
        double font=Theme.number("Font Scale",1);
        if(view instanceof ColorPicker picker){return picker.getPreferredHeight();}
        if(view instanceof EnumSelector<?>){return Math.max(26,18*font*.85+10);}
        if(view instanceof Slider){return labelHeight()+fieldHeight()+errorHeight()+12;}
        if(view instanceof StringControl||view instanceof NumericField){return labelHeight()+fieldHeight()+errorHeight();}
        return Math.max(base,9*font*.85+10);
    }
}
