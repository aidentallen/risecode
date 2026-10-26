package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;

/** Navigation and actions use one reference-canvas geometry. */
public final class ButtonMetrics {
    public static final double HEIGHT=17, TEXT_SCALE=.88, PADDING=14, ICON_SIZE=17, GAP=5;
    private ButtonMetrics() { }
    public static double textScale(double height){return Math.min(TEXT_SCALE,Math.max(1,height-4)/(9*Theme.number("Font Scale",1)));}
    public static double width(Font font,String label){return Math.max(ICON_SIZE,Math.ceil((font==null?label.length()*5.0:RenderUtil.textWidth(font,Theme.label(label),false))*textScale(HEIGHT)+PADDING*2));}
    public static java.util.List<UIComponent.Bounds> flow(Font font,java.util.List<String> labels,double available){
        var result=new java.util.ArrayList<UIComponent.Bounds>();double x=0,y=0;
        for(String label:labels){double w=Math.min(Math.max(1,available),width(font,label));if(x>0&&x+w>available){x=0;y+=rowHeight();}result.add(new UIComponent.Bounds(x,y,w,HEIGHT));x+=w+GAP;}
        return java.util.List.copyOf(result);
    }
    public static double flowHeight(java.util.List<UIComponent.Bounds> positions){return positions.isEmpty()?0:positions.getLast().y()+HEIGHT;}
    public static double rowHeight(){return HEIGHT+GAP;}
}
