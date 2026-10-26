package dev.riserecode.ui.component;

import dev.riserecode.Client;
import dev.riserecode.ui.menu.ClientSettingsScreen.Section;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Footer settings content hosted by the existing client window. */
public final class ClientSettingsEditor extends UIComponent {
    private final Section section;
    private final ThemeEditor editor;
    private String message="";
    public ClientSettingsEditor(Section section){
        this.section=section;
        var client=Client.INSTANCE;var manager=client.getThemeManager();var prefs=client.getClientPreferences();
        var controls=new ArrayList<ThemeManager.Control>();
        switch(section){
            case SOUNDS->{controls.add(new ThemeManager.Control("Client Sounds",prefs.sounds,0));controls.addAll(manager.getControls().stream().filter(c->c.group().equals("Interface Sounds")).toList());}
            case DISCORD->{controls.add(new ThemeManager.Control("Discord Activity",prefs.discord,0));controls.add(new ThemeManager.Control("Discord Activity",prefs.discordId,0));}
            case PERFORMANCE->controls.addAll(client.getPerformance().controls());
            case SCALE->controls.add(new ThemeManager.Control("GUI Scale",manager.getSetting("Interface Scale"),.01));
        }
        editor=new ThemeEditor(manager,text->message=text,controls,true).sectionKey("client:"+section);
        editor.reveal(controls.getFirst().setting().getName());
        if(section==Section.SOUNDS){editor.reveal("Click Sound");}if(section==Section.PERFORMANCE){editor.section("Individual Controls").setExpanded(true);}
    }
    public void revealDetails(){
        editor.reveal(switch(section){case SOUNDS->"Client Sounds";case DISCORD->"Discord Activity";case SCALE->"Interface Scale";case PERFORMANCE->"World Resolution";});
    }
    public Section section(){return section;}
    public ThemeEditor editor(){return editor;}
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        String status=section==Section.PERFORMANCE?dev.riserecode.render.WorldResolution.statusLabel():section==Section.DISCORD?Client.INSTANCE.getDiscordActivity().status():section==Section.SCALE?Math.round(Client.INSTANCE.getThemeManager().number("Interface Scale")*100)+"%":"Interface sounds";
        RenderUtil.text(g,font,RenderUtil.ellipsize(font,message.isEmpty()?status:message,(width-12)/.8),x+6,y,.8,Theme.opacity(Theme.SECONDARY,alpha));
        editor.setBounds(x,y+17,width,Math.max(1,height-17));editor.render(g,font,mx,my,dt,alpha);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){return editor.mouseClicked(mx,my,button);}
    @Override public boolean mouseDragged(double mx,double my,int button){return editor.mouseDragged(mx,my,button);}
    @Override public boolean mouseReleased(double mx,double my,int button){return editor.mouseReleased(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double vertical){return editor.mouseScrolled(mx,my,vertical);}
    public boolean charTyped(CharacterEvent event){return editor.charTyped(event);}
    public boolean keyPressed(KeyEvent event,Supplier<String> clipboard,Consumer<String> copy){return editor.keyPressed(event,clipboard,copy);}
    public void unfocus(){editor.unfocus();}
}
