package dev.riserecode.ui.menu;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Themed child-screen text with measured wrapping and the same typeface as the client. */
public final class ScreenText {
    private ScreenText(){}
    public static double lineHeight(double scale){return Math.ceil(9*Theme.number("Font Scale",1)*scale)+4;}
    public static List<String> wrap(Font font,String text,double width,double scale){var lines=new ArrayList<String>();String line="";for(String word:text.split("\\s+")){String next=line.isEmpty()?word:line+" "+word;if(!line.isEmpty()&&RenderUtil.textWidth(font,next,false)*scale>width){lines.add(line);line=word;}else{line=next;}}if(!line.isEmpty())lines.add(line);return List.copyOf(lines);}
    public static void draw(GuiGraphicsExtractor g,Font font,String text,double x,double y,double scale,int color){RenderUtil.text(g,font,text,x,y,scale,color);}
}
