package dev.riserecode.ui.component.setting;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.component.KeyboardNavigation;
import dev.riserecode.ui.component.SettingMetrics;
import dev.riserecode.ui.component.TextField;
import dev.riserecode.ui.component.UIComponent;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.ColorUtil;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/** Measured HSV/alpha/hex editor. Each endpoint retains its own edit baseline and invalid draft. */
public final class ColorPicker extends UIComponent {
    private static final class Pipelines { private static final RenderPipeline HSV=UiMesh.pipeline("hsv","hsv"); }
    private record Layout(double header,double endpoint,double row,double sv,double alpha,double hexLabel,double hex,double mode,double direction,double children,double slider,double total) { }
    private final ColorSetting setting;
    private final Runnable changed;
    private final Animation opening=Animation.ui(dev.riserecode.ui.theme.ThemeManager.Channel.EXPANSION,0,.2,MathUtil.Easing.OUT_QUART);
    private final TextField[] hexFields={new TextField(()->"AARRGGBB"),new TextField(()->"AARRGGBB")};
    private final int[] baselines=new int[2];
    private final ColorSetting.Mode[] baselineModes=new ColorSetting.Mode[2];
    private final DoubleSetting speed=DoubleSetting.extended("Speed","Color animation speed",.18,0,3),spacing=DoubleSetting.extended("Spacing","Gradient band width",100,1,1000);
    private final Slider speedSlider,spacingSlider;
    private final DoubleSetting[] channels=new DoubleSetting[4];
    private final Slider[] channelSliders=new Slider[4];
    private final Object[] endpointFocus={new Object(),new Object()};
    private final Object modeFocus=new Object(),directionFocus=new Object();
    private boolean expanded,secondary;
    private double hue,saturation,value;
    private int displayed,captured;
    private UIComponent capturedChild;
    public ColorPicker(ColorSetting setting,Runnable changed){this(setting,changed,false);}
    public ColorPicker(ColorSetting setting,Runnable changed,boolean compact){
        this.setting=setting;this.changed=changed;
        speedSlider=new Slider(speed,()->{setting.setSpeed(speed.getValue());changed.run();}).accessibleName(()->setting.getName()+", animation speed");
        spacingSlider=new Slider(spacing,()->{setting.setSpacing(spacing.getValue());changed.run();}).accessibleName(()->setting.getName()+", gradient spacing");
        for(int i=0;i<4;i++){
            final int index=i,shift=(3-i)*8;String name=new String[]{"Alpha","Red","Green","Blue"}[i];
            channels[i]=DoubleSetting.extended(name,"Color channel",(current()>>>shift)&255,0,255);
            channelSliders[i]=new Slider(channels[i],()->{write((current()&~(255<<shift))|((int)Math.round(channels[index].getValue())<<shift),true);readColor();},1).accessibleName(()->context()+", "+name);
        }
        for(int i=0;i<2;i++){
            final int endpoint=i;var field=hexFields[i];baselines[i]=color(i);baselineModes[i]=setting.getMode();
            field.accessibleName(()->setting.getName()+", "+(endpoint==0?"start":"end")+", hex AARRGGBB");field.inlineValidation(false);field.contextBounds(()->{var l=layout();return new Bounds(x,y+l.hexLabel(),width,SettingMetrics.labelHeight()+SettingMetrics.fieldHeight()+SettingMetrics.errorHeight());});
            field.beforeFocus(()->{if(field.validation().isEmpty()){baselines[endpoint]=color(endpoint);baselineModes[endpoint]=setting.getMode();}});
            field.onChange(ignored->previewHex(endpoint));
            field.onCommit(()->{if(field.validation().isEmpty()){field.setValue(ColorSetting.hex(color(endpoint)));baselines[endpoint]=color(endpoint);commitModeBaseline();}});
            field.onCancel(()->{if(endpoint==0){setting.setValue(baselines[endpoint]);}else{setting.setSecondary(baselines[endpoint]);}setting.setMode(baselineModes[endpoint]);displayed=current();field.validation("");field.setValue(ColorSetting.hex(color(endpoint)));changed.run();readColor();});
            field.setValue(ColorSetting.hex(color(i)));
        }
        readColor();
    }
    private String context(){return setting.getName()+", "+(secondary?"end":"start");}
    private TextField hex(){return hexFields[secondary?1:0];}
    private int color(int endpoint){return endpoint==0?setting.getValue():setting.getSecondary();}
    private int current(){return color(secondary?1:0);}
    private Layout layout(){
        double header=Math.max(22,9*Theme.number("Font Scale",1)*.85+10),row=Math.max(24,9*Theme.number("Font Scale",1)*.8+10);
        double endpoint=header+4,sv=endpoint+row+5,alpha=sv+64+6,hexLabel=alpha+16,hex=hexLabel+SettingMetrics.labelHeight();
        double mode=hex+SettingMetrics.fieldHeight()+SettingMetrics.errorHeight()+5,direction=mode+row+4,children=direction+row+8;
        double slider=SettingMetrics.height(speedSlider,0),total=children+6*(slider+5)+3;
        return new Layout(header,endpoint,row,sv,alpha,hexLabel,hex,mode,direction,children,slider,total);
    }
    public ColorSetting getSetting(){return setting;}
    public boolean isExpanded(){return expanded;}
    public TextField getHexField(){return hex();}
    public TextField getHexField(boolean end){return hexFields[end?1:0];}
    public Slider channel(int index){return channelSliders[index];}
    public Slider speed(){return speedSlider;}
    public Slider spacing(){return spacingSlider;}
    public Bounds getSaturationBounds(){var l=layout();return new Bounds(x,y+l.sv(),Math.max(1,width-20),64);}
    public double getPreferredHeight(){if(KeyboardNavigation.instantGeometry()){opening.reset(expanded?1:0);}var l=layout();return l.header()+(l.total()-l.header())*opening.getValue();}
    public void setExpanded(boolean value){expanded=value;opening.animateTo(value?1:0);if(!value){unfocus();mouseReleased(0,0,0);}}
    public void unfocus(){for(var field:hexFields){if(field.isFocused()){field.commit();}}}
    public void pointerFocus(double mx,double my){if(!hex().contains(mx,my)&&hex().isFocused()){hex().commit();}}
    private void commitModeBaseline(){java.util.Arrays.fill(baselineModes,setting.getMode());}
    private void replaceHex(int endpoint){var field=hexFields[endpoint];field.validation("");field.setValue(ColorSetting.hex(color(endpoint)));baselines[endpoint]=color(endpoint);commitModeBaseline();}
    private void writeEndpoint(int endpoint,int color,boolean replaceDraft){
        if(setting.getMode()==ColorSetting.Mode.THEME||endpoint==1&&setting.getMode()==ColorSetting.Mode.SOLID){setting.setMode(endpoint==1?ColorSetting.Mode.GRADIENT:ColorSetting.Mode.SOLID);}
        if(endpoint==0){setting.setValue(color);}else{setting.setSecondary(color);}
        if(endpoint==(secondary?1:0)){displayed=color;}
        if(replaceDraft){replaceHex(endpoint);}changed.run();
    }
    private void write(int color,boolean replaceDraft){writeEndpoint(secondary?1:0,color,replaceDraft);}
    private void readColor(){
        displayed=current();double r=((displayed>>>16)&255)/255.0,g=((displayed>>>8)&255)/255.0,b=(displayed&255)/255.0;
        double max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),delta=max-min;value=max;saturation=max==0?0:delta/max;
        if(delta>0){double h=max==r?(g-b)/delta:max==g?(b-r)/delta+2:(r-g)/delta+4;hue=(h/6+1)%1;}
        for(int i=0;i<4;i++){channelSliders[i].synchronizeExternal((displayed>>>((3-i)*8))&255);}
        if(!hex().isFocused()&&hex().validation().isEmpty()){hex().setValue(ColorSetting.hex(displayed));}
    }
    public static int hsv(double hue,double saturation,double value){return ColorUtil.hsv(hue,MathUtil.clamp01(saturation),MathUtil.clamp01(value));}
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double opacity){
        var l=layout();KeyboardNavigation.control(g,this,new Bounds(x,y,width,l.header()),opacity,()->setting.getName()+", color, "+ColorSetting.hex(current())+", "+(expanded?"expanded":"collapsed"),()->{setExpanded(!expanded);opening.reset(expanded?1:0);},null);
        opening.animateTo(expanded?1:0);opening.update(dt);if(current()!=displayed){replaceHex(secondary?1:0);readColor();}
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,Theme.displayName(setting.getName()),(width-42)/.85),x,y+4,.85,Theme.opacity(Theme.TEXT,opacity));
        RenderUtil.drawPaintRect(g,x+width-36,y+3,28,12,3,setting.paint(),opacity);RenderUtil.text(g,font,expanded?"−":"+",x+width-6,y+4,.8,Theme.opacity(Theme.SECONDARY,opacity));
        if(opening.getValue()<=.001){return;}double alpha=opacity*MathUtil.clamp01(opening.getValue());
        RenderUtil.scissorPush(g,x,y+l.header(),width,Math.max(.1,height-l.header()));
        try{
            for(int i=0;i<2;i++){
                final boolean end=i==1;double left=x+i*(width/2+2);var bounds=new Bounds(left,y+l.endpoint(),width/2-3,l.row());
                KeyboardNavigation.control(g,endpointFocus[i],bounds,alpha,()->setting.getName()+", "+(end?"end":"start")+" color, "+(secondary==end?"selected":"unselected"),()->selectEndpoint(end),direction->selectEndpoint(direction>0));
                RenderUtil.drawRoundedRect(g,bounds.x(),bounds.y(),bounds.width(),bounds.height(),3,Theme.opacity(secondary==end?Theme.HOVER:Theme.SURFACE,alpha));
                RenderUtil.text(g,font,RenderUtil.ellipsize(font,end?"End":"Start",Math.max(1,bounds.width()-22)/.76),left+5,bounds.y()+5,.76,Theme.opacity(secondary==end?Theme.accent():Theme.SECONDARY,alpha));
                RenderUtil.drawRoundedRect(g,left+bounds.width()-14,bounds.y()+7,9,8,2,Theme.opacity(color(i)|0xFF000000,alpha));
            }
            var sv=getSaturationBounds();int white=Theme.opacity(-1,alpha);
            UiMesh.quad(g,Pipelines.HSV,TextureSetup.noTexture(),sv.x(),sv.y(),sv.width(),sv.height(),0,0,1,1,white,white,(int)Math.round(hue*30000),0,0,0);
            UiMesh.quad(g,Pipelines.HSV,TextureSetup.noTexture(),x+width-12,sv.y(),12,sv.height(),0,0,1,1,white,white,0,1,0,0);
            RenderUtil.drawRoundedBorder(g,x+saturation*sv.width()-2.5,sv.y()+(1-value)*sv.height()-2.5,5,5,2.5,white,1);
            RenderUtil.drawRoundedRect(g,x+width-13,sv.y()+hue*sv.height()-1,14,2,.5,white);
            for(int column=0;column<Math.ceil(width/5);column++){RenderUtil.drawRoundedRect(g,x+column*5,y+l.alpha(),Math.min(5,width-column*5),8,0,Theme.opacity((column&1)==0?0xFF46505A:0xFF222A32,alpha));}
            RenderUtil.drawGradientRect(g,x,y+l.alpha(),width,8,current()&0xFFFFFF,current()|0xFF000000,true);RenderUtil.drawRoundedRect(g,x+(current()>>>24)/255.0*width-1,y+l.alpha()-1,2,10,1,white);
            RenderUtil.text(g,font,"Hex · AARRGGBB",x,y+l.hexLabel(),.8,Theme.opacity(Theme.SECONDARY,alpha));
            hex().setBounds(x,y+l.hex(),width,SettingMetrics.fieldHeight());RenderUtil.drawRoundedRect(g,x,y+l.hex(),width,SettingMetrics.fieldHeight(),3,Theme.opacity(Theme.HOVER,alpha));hex().render(g,font,mx,my,dt,alpha);SettingMetrics.validation(g,font,hex(),alpha);
            KeyboardNavigation.control(g,modeFocus,new Bounds(x,y+l.mode(),width,l.row()),alpha,()->setting.getName()+", color mode, "+setting.getMode(),this::openMode,this::changeMode);
            KeyboardNavigation.control(g,directionFocus,new Bounds(x,y+l.direction(),width,l.row()),alpha,()->setting.getName()+", gradient direction, "+setting.getDirection(),this::openDirection,ignored->changeDirection());
            RenderUtil.text(g,font,RenderUtil.ellipsize(font,"Mode: "+Theme.displayName(setting.getMode().name())+" ›",width/.79),x+2,y+l.mode()+5,.79,Theme.opacity(Theme.TEXT,alpha));
            RenderUtil.text(g,font,RenderUtil.ellipsize(font,"Direction: "+Theme.displayName(setting.getDirection().name())+" ›",width/.79),x+2,y+l.direction()+5,.79,Theme.opacity(Theme.TEXT,alpha));
            speedSlider.synchronizeExternal(setting.getSpeed());spacingSlider.synchronizeExternal(setting.getSpacing());
            Slider[] children={speedSlider,spacingSlider,channelSliders[0],channelSliders[1],channelSliders[2],channelSliders[3]};double cy=y+l.children();
            for(var child:children){double h=SettingMetrics.height(child,0);child.setBounds(x,cy,width,h);child.render(g,font,mx,my,dt,alpha);cy+=h+5;}
        }finally{RenderUtil.scissorPop(g);}
    }
    public void selectEndpoint(boolean endpoint){dev.riserecode.ui.component.FocusedInput.commit();secondary=endpoint;readColor();}
    private void openMode(){var values=ColorSetting.Mode.values();setting.setMode(values[(setting.getMode().ordinal()+1)%values.length]);commitModeBaseline();changed.run();readColor();}
    private void openDirection(){var values=ColorUtil.GradientDirection.values();setting.setDirection(values[(setting.getDirection().ordinal()+1)%values.length]);changed.run();}
    private void changeMode(int direction){dev.riserecode.ui.component.FocusedInput.commit();var options=ColorSetting.Mode.values();setting.setMode(options[Math.floorMod(setting.getMode().ordinal()+direction,options.length)]);commitModeBaseline();changed.run();readColor();}
    private void changeDirection(){setting.setDirection(setting.getDirection()==ColorUtil.GradientDirection.HORIZONTAL?ColorUtil.GradientDirection.VERTICAL:ColorUtil.GradientDirection.HORIZONTAL);changed.run();}
    private void update(double mx,double my){
        var l=layout();var sv=getSaturationBounds();
        if(captured==1){saturation=MathUtil.clamp01((mx-x)/sv.width());value=1-MathUtil.clamp01((my-sv.y())/sv.height());}
        else if(captured==2){hue=MathUtil.clamp01((my-sv.y())/sv.height());}
        else if(captured==3){write((current()&0xFFFFFF)|((int)Math.round(MathUtil.clamp01((mx-x)/width)*255)<<24),true);readColor();return;}
        write((current()&0xFF000000)|(hsv(hue,saturation,value)&0xFFFFFF),true);readColor();
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        pointerFocus(mx,my);if(!contains(mx,my)||button!=0&&button!=1){return false;}var l=layout();double local=my-y;
        if(local<l.header()){setExpanded(!expanded);return true;}if(!expanded){return true;}
        if(local>=l.endpoint()&&local<l.endpoint()+l.row()){selectEndpoint(mx>=x+width/2);return true;}
        if(button==0&&local>=l.sv()&&local<=l.sv()+64){captured=mx>=x+width-12?2:mx<x+width-20?1:0;if(captured!=0){dev.riserecode.ui.component.FocusedInput.commit();update(mx,my);}return true;}
        if(button==0&&local>=l.alpha()-2&&local<=l.alpha()+10){captured=3;dev.riserecode.ui.component.FocusedInput.commit();update(mx,my);return true;}
        if(hex().mouseClicked(mx,my,button)){return true;}
        if(local>=l.mode()&&local<l.mode()+l.row()){if(button==0){openMode();}else{changeMode(-1);}return true;}
        if(local>=l.direction()&&local<l.direction()+l.row()){if(button==0){openDirection();}else{changeDirection();}return true;}
        for(var child:new Slider[]{speedSlider,spacingSlider,channelSliders[0],channelSliders[1],channelSliders[2],channelSliders[3]}){if(child.mouseClicked(mx,my,button)){capturedChild=child;return true;}}return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button){if(capturedChild!=null){return capturedChild.mouseDragged(mx,my,button);}if(captured==0||button!=0){return false;}update(mx,my);return true;}
    @Override public boolean mouseReleased(double mx,double my,int button){boolean result=captured!=0||capturedChild!=null;captured=0;if(capturedChild!=null){capturedChild.mouseReleased(mx,my,button);capturedChild=null;}return result;}
    private void previewHex(int endpoint){var field=hexFields[endpoint];try{int color=ColorSetting.parseHex(field.getValue());writeEndpoint(endpoint,color,false);field.validation("");if(endpoint==(secondary?1:0)){readColor();}}catch(IllegalArgumentException invalid){field.validation("Use RRGGBB or AARRGGBB");}}
    public boolean charTyped(CharacterEvent event){return hex().charTyped(event);}
    public boolean keyPressed(KeyEvent event,Supplier<String> clipboard,Consumer<String> copy){if(!hex().isFocused()){return false;}return dev.riserecode.ui.component.FocusedInput.key(event,clipboard,copy);}
}
