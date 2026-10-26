package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Reusable disclosure header. Open states are global UI preferences, not gameplay profiles. */
public final class SettingsSection extends UIComponent {
    public static final double HEADER=24;
    private final String id,label;
    private boolean expanded, card;
    private final dev.riserecode.ui.animation.ButtonFeedback press=new dev.riserecode.ui.animation.ButtonFeedback();
    private final Animation disclosure=Animation.ui(ThemeManager.Channel.EXPANSION,0,.2,MathUtil.Easing.OUT_QUART);
    public SettingsSection card(boolean value){card=value;return this;}
    private final Runnable closing;
    private final Animation body=Animation.ui(ThemeManager.Channel.EXPANSION,0,.2,MathUtil.Easing.OUT_QUART);
    public SettingsSection(String id,String label,Runnable closing){
        this.id=id;this.label=label;this.closing=closing;
        var config=Client.INSTANCE.getConfigManager();
        if(config!=null){var ui=config.getUiPreferences();var states=ui.has("sections")&&ui.get("sections").isJsonObject()?ui.getAsJsonObject("sections"):new com.google.gson.JsonObject();var value=states.get(id);expanded=value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isBoolean()&&value.getAsBoolean();}
    }
    public double feedbackScale(){return card?press.scale():1;}
    public double surfaceHover(){return hover.getValue();}
    public boolean isExpanded(){return expanded;}
    public void setExpanded(boolean value){
        if(expanded==value){return;}if(!value){FocusedInput.commit();closing.run();}expanded=value;
        var config=Client.INSTANCE.getConfigManager();if(config!=null){var ui=config.getUiPreferences();var states=ui.has("sections")&&ui.get("sections").isJsonObject()?ui.getAsJsonObject("sections"):new com.google.gson.JsonObject();states.addProperty(id,value);ui.add("sections",states);config.setUiPreferences(ui);}
    }
    public double bodyHeight(double content){body.animateTo(expanded?content:0);return Math.max(0,body.getValue());}
    public double amount(double content){return content<=0?0:Math.clamp(bodyHeight(content)/content,0,1);}
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        double h=updateHover(mx,my,dt);body.update(dt);
        disclosure.animateTo(expanded?1:0);disclosure.update(dt);
        if(card){

            RenderUtil.text(g,font,RenderUtil.ellipsize(font,label,width-30),x+7+h*.7*Animation.movement(ThemeManager.Channel.HOVER),y+7,Math.min(1,15/(9*Theme.number("Font Scale",1))),Theme.opacity(Theme.TEXT,alpha));
            RenderUtil.text(g,font,Theme.label(expanded?"Click to collapse":"Click to expand"),x+7,y+25,Math.min(.8,10/(9*Theme.number("Font Scale",1))),Theme.opacity(Theme.SECONDARY,alpha*.8));
            g.pose().pushMatrix();g.pose().translate((float)(x+width-13),(float)(y+13));g.pose().rotate((float)(disclosure.getValue()*Math.PI/2));
            RenderUtil.line(g,-2,-3,1,0,1.2,Theme.opacity(Theme.SECONDARY,alpha));RenderUtil.line(g,1,0,-2,3,1.2,Theme.opacity(Theme.SECONDARY,alpha));g.pose().popMatrix();return;
        }
        if(h>.001){RenderUtil.drawRoundedRect(g,x,y,width,HEADER,4,Theme.opacity(Theme.HOVER,alpha*h*.4));}
        RenderUtil.accentText(g,font,RenderUtil.ellipsize(font,label,(width-25)/.85),x+18,y+7,.85,alpha);
        double angle=disclosure.getValue()*Math.PI/2;g.pose().pushMatrix();g.pose().translate((float)(x+8),(float)(y+11));g.pose().rotate((float)angle);
        RenderUtil.line(g,-2,-3,1,0,1.2,Theme.opacity(Theme.TEXT,alpha));RenderUtil.line(g,1,0,-2,3,1.2,Theme.opacity(Theme.TEXT,alpha));g.pose().popMatrix();
    }
    @Override public boolean mouseClicked(double mx,double my,int button){if((button!=0&&button!=1)||!contains(mx,my)){return false;}press.activate();setExpanded(!expanded);dev.riserecode.ui.sound.UiSounds.play();return true;}
}
