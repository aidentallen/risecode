package dev.riserecode.ui.menu;

import dev.riserecode.ui.font.*;
import dev.riserecode.ui.theme.Theme;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/** Measured secondary text; bounded hover preview, with retained access to complete long details. */
public final class TooltipPresentation {
    public record Prepared(List<ClientTooltipComponent> components,int width,int height,boolean overflow,String fullText){}
    private record Key(List<Object> content,Font font,long generation,double fontScale,int width,int height){}
    private record TextLine(StyledText layout,double scale) implements ClientTooltipComponent,TooltipMotion.Text {
        @Override public StyledText risecode$layout(){return layout;}
        @Override public int getWidth(Font font){return (int)Math.ceil(layout.width(font)*scale);}
        @Override public int getHeight(Font font){return (int)Math.ceil(9*Theme.number("Font Scale",1)*scale)+2;}
        @Override public void extractText(GuiGraphicsExtractor g,Font font,int x,int y){layout.draw(g,font,x,y,scale,Theme.opacity(Theme.TEXT,TooltipMotion.alpha()));}
    }
    private static Key key;
    private static Prepared cached,last;
    private static WeakReference<Screen> owner=new WeakReference<>(null);
    private static long visible;
    private static dev.riserecode.ui.component.UIComponent.Bounds bounds=new dev.riserecode.ui.component.UIComponent.Bounds(0,0,0,0);
    private TooltipPresentation(){}
    public static Prepared prepare(Font font,List<ClientTooltipComponent> components,int width,int height){
        var signature=new ArrayList<Object>();for(var c:components){signature.add(c instanceof TooltipMotion.Text text?text.risecode$layout().runs():List.of(c,c.getWidth(font),c.getHeight(font)));}
        var next=new Key(List.copyOf(signature),font,CustomTextRenderer.generation(),Theme.number("Font Scale",1),width,height);if(next.equals(key)){return cached;}
        double scale=Math.min(1,13/(9*Theme.number("Font Scale",1))),maximum=Math.max(1,Math.min(300,Math.min(width-24,width*.8)));
        var lines=new ArrayList<ClientTooltipComponent>();var full=new ArrayList<String>();boolean onlyText=true;
        for(var c:components){if(c instanceof TooltipMotion.Text text){full.add(text.risecode$layout().plain());for(var part:text.risecode$layout().wrapWords(font,maximum/scale)){lines.add(new TextLine(part,scale));}}else{onlyText=false;lines.add(c);}}
        boolean overflow=false;int maximumHeight=Math.max(20,(int)Math.min(height-24,height*.42));
        if(onlyText&&measuredHeight(font,lines)>maximumHeight){overflow=true;var hint=new TextLine(StyledText.of(net.minecraft.network.chat.Component.literal("F1: full details").withStyle(net.minecraft.ChatFormatting.GRAY)),scale);while(!lines.isEmpty()&&measuredHeight(font,lines)+hint.getHeight(font)>maximumHeight){lines.removeLast();}lines.add(hint);}
        int measuredWidth=lines.stream().mapToInt(c->c.getWidth(font)).max().orElse(0);key=next;cached=new Prepared(List.copyOf(lines),measuredWidth,measuredHeight(font,lines),overflow,String.join("\n",full));return cached;
    }
    private static int measuredHeight(Font font,List<ClientTooltipComponent> lines){return Math.max(0,lines.stream().mapToInt(c->c.getHeight(font)).sum()+(lines.size()==1?-2:0));}
    public static int[] anchor(int x,int y){var screen=Minecraft.getInstance().gui.screen();if(screen instanceof dev.riserecode.ui.clickgui.ClickGui gui){var window=gui.getWindowBounds();var shown=gui.getDisplayedWindowBounds();double scale=shown.width()/window.width();x=(int)Math.round((window.centerX()+(x-window.centerX())*scale)*gui.getCanvasScale());y=(int)Math.round((window.centerY()+(y-window.centerY())*scale)*gui.getCanvasScale());}else if(screen instanceof PageAppearanceScreen page){x=(int)Math.round(page.width-16+(x-page.width+16)*page.visualScale());y=(int)Math.round(16+(y-16)*page.visualScale());}return new int[]{x,y};}
    public static void shown(Prepared value,int x,int y){last=value;visible=System.nanoTime();owner=new WeakReference<>(Minecraft.getInstance().gui.screen());bounds=new dev.riserecode.ui.component.UIComponent.Bounds(x,y,value.width(),value.height());}
    public static Prepared last(){return last;}
    public static dev.riserecode.ui.component.UIComponent.Bounds bounds(){return bounds;}
    public static boolean openDetails(){var mc=Minecraft.getInstance();if(last==null||!last.overflow()||!MenuTheme.activeMenu()||mc.gui.overlay()!=null||owner.get()!=mc.gui.screen()||System.nanoTime()-visible>250_000_000L){return false;}String text=last.fullText();mc.gui.setScreen(new InfoScreen(mc.gui.screen(),"Full tooltip details",text));return true;}
    public static void expire(){if(visible!=0&&(owner.get()==null||System.nanoTime()-visible>2_000_000_000L)){key=null;cached=null;last=null;visible=0;owner.clear();}}
}
