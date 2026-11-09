package dev.riserecode.ui.menu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Focusable full text for notices and complete shortcut help, with keyboard/wheel paging. */
public final class InfoScreen extends RetainedScreen {
    private final String text;
    private int scroll,max;
    private final ScreenText.Layout layout=new ScreenText.Layout();
    public int scrollPosition(){return scroll;}
    public int maximumScroll(){return max;}
    public InfoScreen(Screen parent,String title,String text){super(parent,title);this.text=text;}
    public String text(){return text;}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". "+text);}
    @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("Done"),b->onClose()).bounds(width/2-50,height-28,100,20).build());}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);ScreenText.draw(g,font,dev.riserecode.util.RenderUtil.ellipsize(font,title.getString(),(width-32)/.9),16,12,.9,0xFFFFFFFF);
        var lines=layout.lines(font,text,Math.max(1,width-40),.75);double lineHeight=ScreenText.lineHeight(.75);int visible=Math.max(1,(int)((height-88)/lineHeight));max=Math.max(0,lines.size()-visible);scroll=Math.clamp(scroll,0,max);
        for(int i=scroll;i<Math.min(lines.size(),scroll+visible);i++){ScreenText.draw(g,font,lines.get(i),20,42+(i-scroll)*lineHeight,.75,0xFFE6EDF4);}
        if(max>0){ScreenText.draw(g,font,"Page Up/Down or wheel",20,height-48,.65,0xFFB7CBD9);}
    }
    @Override public boolean keyPressed(KeyEvent e){if(e.key()==266||e.key()==267||e.key()==265||e.key()==264){scroll=Math.clamp(scroll+(e.key()==266||e.key()==265?-1:1)*(e.key()==266||e.key()==267?Math.max(1,(int)((height-88)/ScreenText.lineHeight(.75))-1):1),0,max);return true;}return super.keyPressed(e);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll=Math.clamp(scroll-(int)vertical*3,0,max);return true;}
}
