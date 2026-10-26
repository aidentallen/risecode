package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.animation.PopIn;
import dev.riserecode.ui.clickgui.ClickGui;
import dev.riserecode.ui.menu.ClientSettingsScreen;
import dev.riserecode.ui.sound.UiSounds;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

/** Sidebar footer with a modal, inward-opening settings menu. */
public final class ClientSettingsPopup extends UIComponent {
    public static final int ROWS=8;
    private final ClickGui owner;
    private final Animation opening=Animation.ui(ThemeManager.Channel.OPENING,0,.26,MathUtil.Easing.OUT_QUART)
            .timing(()->PopIn.windowDuration("Client"));
    private final Animation[] hovers=java.util.stream.IntStream.range(0,ROWS).mapToObj(i->Animation.ui(ThemeManager.Channel.HOVER,0,.14,MathUtil.Easing.OUT_QUART)).toArray(Animation[]::new);
    private final dev.riserecode.ui.animation.ButtonFeedback[] presses=java.util.stream.IntStream.range(0,ROWS).mapToObj(i->new dev.riserecode.ui.animation.ButtonFeedback()).toArray(dev.riserecode.ui.animation.ButtonFeedback[]::new);
    private final dev.riserecode.ui.animation.ButtonFeedback triggerPress=new dev.riserecode.ui.animation.ButtonFeedback();
    private boolean open;
    private int selected;
    private boolean keyboard;
    private double lastX=Double.NaN,lastY=Double.NaN;
    private double maxWidth,maxHeight;
    private Bounds popup=new Bounds(0,0,1,1);
    public ClientSettingsPopup(ClickGui owner){this.owner=owner;}
    public boolean isOpen(){return open;}
    public Bounds popupBounds(){return popup;}
    private double popupScale(){return PopIn.windowScale("Client",opening.getValue());}
    private double entranceOffset(){return Theme.bool("Reduced Motion",false)?0:(1-opening.getValue())*8;}
    public Bounds displayedPopupBounds(){
        double scale=popupScale(),offset=entranceOffset();
        return new Bounds(popup.x()-offset,popup.y()+popup.height()*(1-scale)+offset,popup.width()*scale,popup.height()*scale);
    }
    private double pointerX(double mx){return popup.x()+(mx-popup.x()+entranceOffset())/popupScale();}
    private double pointerY(double my){double bottom=popup.y()+popup.height();return bottom+(my-bottom-entranceOffset())/popupScale();}
    public Bounds rowBounds(int i){return new Bounds(popup.x()+6,popup.y()+8+i*22,popup.width()-12,ButtonMetrics.HEIGHT);}
    public void viewport(double w,double h){maxWidth=w;maxHeight=h;}
    public void toggle(){open=!open;opening.animateTo(open?1:0);if(open){FocusedInput.commit();}else{Client.INSTANCE.getKeybindManager().cancelCapture();}}
    public void close(){open=false;opening.animateTo(0);Client.INSTANCE.getKeybindManager().cancelCapture();}
    private static boolean inside(Bounds b,double px,double py){return px>=b.x()&&px<b.x()+b.width()&&py>=b.y()&&py<b.y()+b.height();}
    private String label(int row){
        var client=Client.INSTANCE;var prefs=client.getClientPreferences();var keys=client.getKeybindManager();
        return switch(row){case 0->"Profiles";case 1->"Friends";case 2->"Client Sounds";case 3->"Command Suggestions";case 4->"Discord Activity";case 5->keys.isMenuCapturing()?"Press a key · Esc cancels":"Menu Keybind";case 6->"GUI Scale";default->"Performance";};
    }
    private String value(int row){var client=Client.INSTANCE;var prefs=client.getClientPreferences();return switch(row){case 0,1->"›";case 2->prefs.sounds.getValue()?"On  ›":"Off  ›";case 3->prefs.suggestions.getValue()?"On":"Off";case 4->prefs.discord.getValue()?"On  ›":"Off  ›";case 5->client.getKeybindManager().getClickGuiKey().getTranslatedKeyMessage().getString();case 7->client.getPerformance().mode.getValue().name()+"  ›";default->Math.round(client.getThemeManager().number("Interface Scale")*100)+"%  ›";};}
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        if(mx!=lastX||my!=lastY){keyboard=false;lastX=mx;lastY=my;}
        double amount=opening.update(dt);hover.animateTo(contains(mx,my)?1:0);double over=hover.update(dt);
        double triggerScale=triggerPress.scale();g.pose().pushMatrix();g.pose().translate((float)(x+width/2),(float)(y+height/2));g.pose().scale((float)triggerScale,(float)triggerScale);g.pose().translate((float)(-x-width/2),(float)(-y-height/2));
        RenderUtil.drawButton(g,x,y,width,height,over,alpha);Icon.PLAYER.draw(g,x+4,y+3,Theme.opacity(Theme.accent(),alpha));
        String name=Minecraft.getInstance().getUser().getName();
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,name,(width-30)/.72),x+18,y+5,.72,Theme.opacity(Theme.TEXT,alpha));
        RenderUtil.text(g,font,"›",x+width-10,y+4,.8,Theme.opacity(Theme.SECONDARY,alpha));g.pose().popMatrix();
        double w=Math.min(206,Math.max(1,maxWidth-16)),h=ROWS*22+12;
        popup=new Bounds(Math.clamp(x+width+5,8,Math.max(8,maxWidth-w-8)),Math.clamp(y+height-h,8,Math.max(8,maxHeight-h-8)),w,h);
        if(amount<.001){return;}double opacity=alpha*amount;
        double px=pointerX(mx),py=pointerY(my),scale=popupScale(),offset=entranceOffset(),bottom=popup.y()+h;
        g.pose().pushMatrix();
        g.pose().translate((float)(popup.x()-offset),(float)(bottom+offset));
        g.pose().scale((float)scale,(float)scale);
        g.pose().translate((float)-popup.x(),(float)-bottom);
        try{
        RenderUtil.drawDropShadow(g,popup.x(),popup.y(),w,h,10,Theme.opacity(Theme.SHADOW,opacity*.7));
        RenderUtil.drawPopup(g,popup.x(),popup.y(),w,h,opacity*.82);
        for(int i=0;i<ROWS;i++){
            Bounds b=rowBounds(i);boolean hot=open&&inside(b,px,py);if(hot&&!keyboard){selected=i;}hovers[i].animateTo(hot?1:0);double hovered=hovers[i].update(dt);
            double pressScale=presses[i].scale();g.pose().pushMatrix();g.pose().translate((float)b.centerX(),(float)b.centerY());g.pose().scale((float)pressScale,(float)pressScale);g.pose().translate((float)-b.centerX(),(float)-b.centerY());
            RenderUtil.drawButton(g,b.x(),b.y(),b.width(),b.height(),Math.max(hovered,selected==i?.32:0),opacity);
            String tail=value(i);double tailWidth=Math.min(83,RenderUtil.textWidth(font,tail,false)*.68);
            RenderUtil.text(g,font,RenderUtil.ellipsize(font,label(i),(b.width()-tailWidth-18)/.76),b.x()+5,b.y()+5,.76,Theme.opacity(Theme.TEXT,opacity));
            RenderUtil.text(g,font,RenderUtil.ellipsize(font,tail,83/.68),b.x()+b.width()-tailWidth-5,b.y()+5,.68,Theme.opacity(i==2||i==3||i==4?Theme.accent():Theme.SECONDARY,opacity));g.pose().popMatrix();
        }
        String notice=Client.INSTANCE.getKeybindManager().getCaptureNotice();if(!notice.isEmpty()){RenderUtil.text(g,font,RenderUtil.ellipsize(font,notice,w/.7),popup.x(),popup.y()-12,.7,Theme.opacity(Theme.accent(),opacity));}
        }finally{g.pose().popMatrix();}
    }
    private void panel(ClientSettingsScreen.Section section){close();owner.openClientSettings(section);}
    public void activate(int row,boolean setting){
        var prefs=Client.INSTANCE.getClientPreferences();
        switch(row){
            case 0->{close();owner.selectPage(ClickGui.Page.PROFILES);}
            case 1->{close();owner.selectPage(ClickGui.Page.FRIENDS);}
            case 2->{if(setting){panel(ClientSettingsScreen.Section.SOUNDS);}else{prefs.sounds.setValue(!prefs.sounds.getValue());}}
            case 3->prefs.suggestions.setValue(!prefs.suggestions.getValue());
            case 4->{if(setting){panel(ClientSettingsScreen.Section.DISCORD);}else{prefs.discord.setValue(!prefs.discord.getValue());}}
            case 5->Client.INSTANCE.getKeybindManager().beginMenuCapture();
            case 6->panel(ClientSettingsScreen.Section.SCALE);
            default->panel(ClientSettingsScreen.Section.PERFORMANCE);
        }
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(contains(mx,my)){if(button==0){triggerPress.press();toggle();UiSounds.play();}return true;}
        if(!open){return false;}
        mx=pointerX(mx);my=pointerY(my);
        if(button==0||button==1){for(int i=0;i<ROWS;i++){Bounds b=rowBounds(i);if(inside(b,mx,my)){presses[i].press();selected=i;if(button==0||i!=3&&i!=5){activate(i,button==1||(i==2||i==4)&&mx>=b.x()+b.width()-16);}UiSounds.play();return true;}}}
        if(!inside(popup,mx,my)){close();}return true;
    }
    @Override public boolean mouseReleased(double mx,double my,int button){if(button==0||button==1){triggerPress.release();for(var press:presses){press.release();}}return open;}
    public boolean keyPressed(KeyEvent e){
        if(!open){return false;}
        keyboard=true;
        if(e.isEscape()){close();return true;}
        if(e.key()==264||e.key()==265||e.key()==258){selected=Math.floorMod(selected+(e.key()==265||e.key()==258&&e.hasShiftDown()?-1:1),ROWS);return true;}
        if(e.isConfirmation()||e.key()==262){activate(selected,e.key()==262);UiSounds.play();return true;}return true;
    }
}
