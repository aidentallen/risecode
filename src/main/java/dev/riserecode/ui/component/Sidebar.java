package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Scrollable navigation; utilities expand inline and share displayed coordinates with input. */
public final class Sidebar extends UIComponent {
    public record Item(String label, Icon icon) { }
    private static final double ROW = 20.75;
    private static final int PRIMARY = 11;
    private final List<Item> items;
    private final IntConsumer selectedAction;
    private final Animation expansion = Animation.ui(ThemeManager.Channel.EXPANSION,0,0.2,MathUtil.Easing.OUT_QUART);
    private final Animation scroll = Animation.ui(ThemeManager.Channel.SCROLLING,0,0.18,MathUtil.Easing.OUT_QUART);
    private final List<Animation> hovers;
    private final List<dev.riserecode.ui.animation.ButtonFeedback> presses;
    private final dev.riserecode.ui.animation.ButtonFeedback arrowPress = new dev.riserecode.ui.animation.ButtonFeedback();
    private final Animation arrowHover = Animation.ui(ThemeManager.Channel.HOVER,0,.16,MathUtil.Easing.OUT_QUART);
    private final Animation selectedY = Animation.ui(ThemeManager.Channel.NAVIGATION, 0, .2, MathUtil.Easing.OUT_QUART);
    private final Animation selectedWidth = Animation.ui(ThemeManager.Channel.NAVIGATION, 52, .2, MathUtil.Easing.OUT_QUART);
    private boolean expanded;
    private int selected;
    private boolean reveal;
    private double maximum;
    public Sidebar(List<Item> items, IntConsumer selectedAction) { this.items=List.copyOf(items);this.selectedAction=selectedAction;
        hovers = items.stream().map(item -> Animation.ui(ThemeManager.Channel.HOVER, 0, .16, MathUtil.Easing.OUT_QUART)).toList();
        presses = items.stream().map(item -> new dev.riserecode.ui.animation.ButtonFeedback()).toList();
    }
    public void setSelected(int index) { selected=index;reveal=true; if(index>=PRIMARY){ expanded=true;expansion.animateTo(1); } }
    public double selectionOffset() { return selectedY.getValue(); }
    public double pressScale(int index){return presses.get(index).scale();}
    public double hoverAmount(int index) { return hovers.get(index).getValue(); }
    public void activateSelected() { if(selected>=0){presses.get(selected).activate();} }
    public int getSelected(){return selected;}
    public boolean isExpanded(){return expanded;}
    public void toggleExpanded(){arrowPress.activate();expanded=!expanded;expansion.animateTo(expanded?1:0);dev.riserecode.ui.sound.UiSounds.play();}
    public Bounds getArrowBounds(){return new Bounds(x+10,y+43+PRIMARY*ROW-scroll.getValue(),Math.min(width-20,ButtonMetrics.width(net.minecraft.client.Minecraft.getInstance()==null?null:net.minecraft.client.Minecraft.getInstance().font,"Utilities")),ButtonMetrics.HEIGHT);}
    public Bounds getItemBounds(int index){return new Bounds(x+10,y+43+(index+(index>=PRIMARY?1:0))*ROW-scroll.getValue(),Math.min(width-20,ButtonMetrics.width(net.minecraft.client.Minecraft.getInstance()==null?null:net.minecraft.client.Minecraft.getInstance().font,items.get(index).label())),ButtonMetrics.HEIGHT);}
    public List<Integer> keyboardItems(){return java.util.stream.IntStream.range(0,expanded?items.size():Math.min(PRIMARY,items.size())).boxed().toList();}
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        double logoScale=Math.min(1.65,(width-37)/Math.max(1,RenderUtil.textWidth(font,Client.INSTANCE.getName(),false)));
        RenderUtil.text(g,font,Client.INSTANCE.getName(),x+14,y+14,logoScale,Theme.opacity(Theme.TEXT,alpha));
        RenderUtil.text(g,font,Client.INSTANCE.getVersion(),x+14,y+29,0.55,Theme.opacity(Theme.accent(),alpha*.85));
        double expandedAmount=expansion.update(dt),available=Math.max(1,height-51);
        maximum=Math.max(0,(Math.min(PRIMARY,items.size())+1+(items.size()-PRIMARY)*expandedAmount)*ROW-available);
        if(scroll.getValue()>maximum){scroll.reset(maximum);}scroll.animateTo(Math.clamp(scroll.getTarget(),0,maximum));scroll.update(dt);
        if(reveal&&selected>=0&&(selected<PRIMARY||expandedAmount>.99)){
            Bounds row=getItemBounds(selected);double shift=row.y()<y+43?row.y()-y-43:Math.max(0,row.y()+row.height()-y-height+8);
            scroll.animateTo(Math.clamp(scroll.getValue()+shift,0,maximum));reveal=false;
        }
        double targetY = (selected >= PRIMARY && !expanded ? PRIMARY : selected + (selected >= PRIMARY ? 1 : 0)) * ROW;
        selectedY.animateTo(targetY);
        double pillY = y + 43 + targetY + (selectedY.update(dt) - targetY) * Animation.movement(ThemeManager.Channel.NAVIGATION) - scroll.getValue();
        String selectedLabel = selected<0?"":Theme.label(items.get(selected).label());
        selectedWidth.animateTo(Math.min(width - 20, ButtonMetrics.width(font,selectedLabel)));
        double pillWidth = selectedWidth.update(dt);
        RenderUtil.scissorPush(g,x+4,y+40,width-8,available+3);
        try{
            double selectedPress = selected<0?1:presses.get(selected).scale();
            g.pose().pushMatrix();
            g.pose().translate((float)(x+10+pillWidth/2),(float)(pillY+8.5));g.pose().scale((float)selectedPress,(float)selectedPress);g.pose().translate((float)(-x-10-pillWidth/2),(float)(-pillY-8.5));
            RenderUtil.drawGlow(g,x+10,pillY,pillWidth,17,4,Theme.opacity(Theme.accent(),selected<0?0:alpha));
            RenderUtil.drawAccentRoundedRect(g,x+10,pillY,pillWidth,17,4,selected<0?0:alpha);
            g.pose().popMatrix();
            for(int i=0;i<items.size();i++){
                if(i>=PRIMARY&&expandedAmount<.001){break;}Bounds b=getItemBounds(i);
                double utilityClip=y+43+(PRIMARY+1)*ROW-scroll.getValue()+(items.size()-PRIMARY)*ROW*expandedAmount;
                if(i>=PRIMARY&&b.y()+b.height()>utilityClip){continue;}
                boolean over=contains(mx,my)&&my>=y+40&&mx>=b.x()&&mx<b.x()+b.width()&&my>=b.y()&&my<b.y()+b.height();
                hovers.get(i).animateTo(over?1:0); double hovered=hovers.get(i).update(dt);
                double press=presses.get(i).scale();g.pose().pushMatrix();
                g.pose().translate((float)b.centerX(),(float)b.centerY());g.pose().scale((float)press,(float)press);g.pose().translate((float)-b.centerX(),(float)-b.centerY());
                if(i!=selected){RenderUtil.drawRoundedRect(g,b.x(),b.y(),Math.min(b.width(),ButtonMetrics.width(font,items.get(i).label())),17,4,Theme.opacity(Theme.HOVER,alpha*hovered*.4));}
                double rowAlpha=alpha*(i>=PRIMARY?expandedAmount:1),shift=hovered*.65*Animation.movement(ThemeManager.Channel.HOVER);
                int color=Theme.opacity(i==selected?Theme.manager().colorSetting("Selected Tab Color").sample(b.x(),b.y()):Theme.blendPalette(Theme.SECONDARY,Theme.TEXT,hovered),rowAlpha);
                items.get(i).icon().draw(g,b.x()+4+shift,b.y()+3,color);
                RenderUtil.text(g,font,Theme.label(items.get(i).label()),b.x()+19+shift,b.y()+4,Math.min(ButtonMetrics.textScale(b.height()),(b.width()-21)/Math.max(1,RenderUtil.textWidth(font,Theme.label(items.get(i).label()),false))),color);
                g.pose().popMatrix();
            }
            Bounds arrow=getArrowBounds();
            double arrowScale=arrowPress.scale();g.pose().pushMatrix();g.pose().translate((float)arrow.centerX(),(float)arrow.centerY());g.pose().scale((float)arrowScale,(float)arrowScale);g.pose().translate((float)-arrow.centerX(),(float)-arrow.centerY());
            arrowHover.animateTo(contains(mx,my)&&my>=arrow.y()&&my<arrow.y()+arrow.height()?1:0);
            double arrowAmount=arrowHover.update(dt);
            RenderUtil.drawRoundedRect(g,arrow.x(),arrow.y(),Math.min(arrow.width(),ButtonMetrics.width(font,"Utilities")),17,4,Theme.opacity(Theme.HOVER,alpha*arrowAmount*.4));
            RenderUtil.text(g,font,Theme.label("Utilities"),arrow.x()+19,arrow.y()+4,Math.min(.8,ButtonMetrics.textScale(arrow.height())),Theme.opacity(Theme.blendPalette(Theme.SECONDARY,Theme.TEXT,arrowAmount),alpha));
            g.pose().pushMatrix();g.pose().translate((float)(arrow.x()+8),(float)(arrow.y()+9));g.pose().rotate((float)(expandedAmount*Math.PI/2));
            RenderUtil.line(g,-2,-3,1,0,1.3,Theme.opacity(Theme.TEXT,alpha));RenderUtil.line(g,1,0,-2,3,1.3,Theme.opacity(Theme.TEXT,alpha));g.pose().popMatrix();g.pose().popMatrix();
        }finally{RenderUtil.scissorPop(g);}
        if(maximum>0){double thumb=Math.max(12,available*available/(available+maximum));RenderUtil.drawRoundedRect(g,x+width-4,y+43+scroll.getValue()/maximum*(available-thumb),1.5,thumb,1,Theme.opacity(Theme.accent(),alpha*.6));}
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0||!contains(mx,my)||my<y+40){return false;}Bounds a=getArrowBounds();
        if(mx>=a.x()&&mx<a.x()+a.width()&&my>=a.y()&&my<a.y()+a.height()){toggleExpanded();arrowPress.press();return true;}
        for(int i:keyboardItems()){Bounds b=getItemBounds(i);if(mx>=b.x()&&mx<b.x()+b.width()&&my>=b.y()&&my<b.y()+b.height()){
            if(i>=PRIMARY&&expansion.getValue()<.99){return true;}presses.get(i).press();dev.riserecode.ui.sound.UiSounds.play();selectedAction.accept(i);return true;
        }}return true;
    }
    @Override public boolean mouseReleased(double mx,double my,int button){if(button!=0){return false;}presses.forEach(dev.riserecode.ui.animation.ButtonFeedback::release);arrowPress.release();return true;}
    @Override public boolean mouseScrolled(double mx,double my,double amount){if(!contains(mx,my)){return false;}scroll.animateTo(Math.clamp(scroll.getTarget()-amount*24,0,maximum));return true;}
}
