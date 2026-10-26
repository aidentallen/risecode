package dev.riserecode.ui.menu;

import dev.riserecode.Client;
import dev.riserecode.ui.component.*;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

/** Focused panels opened by the client footer. Reuses the canonical settings. */
public final class ClientSettingsScreen extends Screen {
    public enum Section { SOUNDS, DISCORD, SCALE, PERFORMANCE }
    private final Screen parent;
    private final Section section;
    private ThemeEditor editor;
    private String message="";
    private final ActionButton done=new ActionButton("Done",this::onClose);
    private long previous=System.nanoTime();
    public ClientSettingsScreen(Screen parent,Section section){super(Component.literal(switch(section){case SOUNDS->"Client sounds";case DISCORD->"Discord activity";case SCALE->"GUI scale";case PERFORMANCE->"Performance";}));this.parent=parent;this.section=section;}
    public ThemeEditor editor(){return editor;}
    public Section section(){return section;}
    @Override protected void init(){
        var client=Client.INSTANCE;var manager=client.getThemeManager();var prefs=client.getClientPreferences();
        var controls=new ArrayList<ThemeManager.Control>();
        switch(section){
            case SOUNDS->{controls.add(new ThemeManager.Control("Client Sounds",prefs.sounds,0));controls.addAll(manager.getControls().stream().filter(c->c.group().equals("Interface Sounds")).toList());}
            case DISCORD->{controls.add(new ThemeManager.Control("Discord Activity",prefs.discord,0));controls.add(new ThemeManager.Control("Discord Activity",prefs.discordId,0));}
            case PERFORMANCE->controls.addAll(client.getPerformance().controls());
            case SCALE->controls.add(new ThemeManager.Control("GUI Scale",manager.getSetting("Interface Scale"),.01));
        }
        editor=new ThemeEditor(manager,text->message=text,controls,true).sectionKey("client:"+section);
        double w=Math.min(430,width-32);editor.setBounds((width-w)/2,51,w,Math.max(1,height-90));
        editor.reveal(controls.getFirst().setting().getName());
        if(section==Section.SOUNDS){editor.reveal("Click Sound");}if(section==Section.PERFORMANCE){editor.section("Individual Controls").setExpanded(true);}
        done.setBounds(width/2.0-45,height-29,90,17);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        long now=System.nanoTime();double dt=(now-previous)*1e-9;previous=now;
        var b=editor.getBounds();RenderUtil.drawRoundedRect(g,b.x()-8,10,b.width()+16,height-20,7,Theme.opacity(Theme.BACKGROUND,.96));
        RenderUtil.text(g,font,getTitle().getString(),b.x()+8,20,1,Theme.TEXT);
        String status=section==Section.PERFORMANCE?dev.riserecode.render.WorldResolution.statusLabel():section==Section.DISCORD?Client.INSTANCE.getDiscordActivity().status():section==Section.SCALE?Math.round(Client.INSTANCE.getThemeManager().number("Interface Scale")*100)+"%":"Interface sounds";
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,message.isEmpty()?status:message,b.width()/.8),b.x()+8,35,.8,Theme.SECONDARY);
        editor.render(g,font,mx,my,dt,1);done.render(g,font,mx,my,dt,1);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){FocusedInput.pointer(e.x(),e.y());return done.mouseClicked(e.x(),e.y(),e.button())||editor.mouseClicked(e.x(),e.y(),e.button());}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){return editor.mouseDragged(e.x(),e.y(),e.button());}
    @Override public boolean mouseReleased(MouseButtonEvent e){dev.riserecode.ui.animation.ButtonFeedback.releaseAll();return editor.mouseReleased(e.x(),e.y(),e.button());}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){return editor.mouseScrolled(x,y,v);}
    @Override public boolean charTyped(CharacterEvent e){return FocusedInput.character(e);}
    @Override public boolean keyPressed(KeyEvent e){
        if(Client.INSTANCE.getKeybindManager().getClickGuiKey().matches(e)){onClose();if(parent instanceof dev.riserecode.ui.clickgui.ClickGui gui){gui.onClose();}return true;}
        if(FocusedInput.key(e,minecraft.keyboardHandler::getClipboard,minecraft.keyboardHandler::setClipboard)){return true;}
        if(e.isEscape()){onClose();return true;}if(editor.keyPressed(e,minecraft.keyboardHandler::getClipboard,minecraft.keyboardHandler::setClipboard)){return true;}return super.keyPressed(e);
    }
    @Override public void onClose(){FocusedInput.commit();editor.unfocus();Client.INSTANCE.getThemeManager().save();minecraft.gui.setScreen(parent);}
    @Override public void removed(){FocusedInput.commit();editor.unfocus();Client.INSTANCE.getThemeManager().save();}
}
