package dev.riserecode.ui.menu;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.text.BreakIterator;
import java.util.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Measured themed wrapping, including exact long tokens and deliberate paragraph breaks. */
public final class ScreenText {
    private ScreenText(){}
    public static double lineHeight(double scale){return Math.ceil(9*Theme.number("Font Scale",1)*scale)+4;}
    public static List<String> wrap(Font font,String text,double width,double scale){
        var result=new ArrayList<String>();double limit=Math.max(1,width);
        for(String paragraph:text.split("\\n",-1)){
            if(paragraph.isEmpty()){result.add("");continue;}
            var graphemes=BreakIterator.getCharacterInstance(Locale.ROOT);graphemes.setText(paragraph);int start=0;
            while(start<paragraph.length()){
                int end=start,lastWord=-1,next=graphemes.following(start);
                while(next!=BreakIterator.DONE){
                    if(end>start&&RenderUtil.textWidth(font,paragraph.substring(start,next),false)*scale>limit){break;}
                    end=next;if(Character.isWhitespace(paragraph.codePointBefore(end))){lastWord=end;}next=graphemes.next();
                }
                if(next!=BreakIterator.DONE&&lastWord>start){end=lastWord;}
                if(end<=start){end=graphemes.following(start);}
                result.add(paragraph.substring(start,end));start=end;
            }
        }
        return List.copyOf(result);
    }
    public static final class Layout {
        private String text;private double width,scale,fontScale;private long generation;private Font font;private List<String> lines=List.of();
        public List<String> lines(Font font,String text,double width,double scale){double fs=Theme.number("Font Scale",1);long gen=dev.riserecode.ui.font.CustomTextRenderer.generation();if(!Objects.equals(this.text,text)||this.width!=width||this.scale!=scale||this.fontScale!=fs||this.generation!=gen||this.font!=font){this.text=text;this.width=width;this.scale=scale;this.fontScale=fs;this.generation=gen;this.font=font;lines=wrap(font,text,width,scale);}return lines;}
    }
    public static void draw(GuiGraphicsExtractor g,Font font,String text,double x,double y,double scale,int color){RenderUtil.text(g,font,text,x,y,scale,color);}
}
