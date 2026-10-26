package dev.riserecode.ui.component;

import dev.riserecode.ui.theme.ModuleAppearance;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.ui.component.setting.ToggleSwitch;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Embedded appearance editor; its surrounding module panel owns scrolling. */
public final class ModuleAppearanceEditor extends UIComponent {
    private final ModuleAppearance model;
    private final SettingsSection header;
    private final ToggleSwitch global;
    private ThemeEditor editor;
    private final ActionButton reset;
    private final ThemeManager manager;
    private final String id;
    private boolean inheritedLast;
    public ModuleAppearanceEditor(ThemeManager manager,dev.riserecode.module.Module module){
        this.manager=manager;id=module.capabilityId();model=manager.module(module);inheritedLast=model.global.getValue();
        header=new SettingsSection("module:"+id+":appearance","Appearance",this::unfocus);
        global=new ToggleSwitch(model.global,()->{unfocus();manager.changed();});
        reset=new ActionButton("Reset this module",()->{FocusedInput.commit();unfocus();model.resetLocal();});
    }
    public SettingsSection section(){return header;}
    public ModuleAppearance model(){return model;}
    public ToggleSwitch globalControl(){return global;}
    public ThemeEditor editor(){if(editor==null){editor=new ThemeEditor(manager,ignored->{},model.controls().stream().filter(c->c.setting()!=model.global).toList(),true).sectionKey("module:"+id);editor.replaceSectionActions("Presets",java.util.List.of(new ActionButton("Reset this module",()->{FocusedInput.commit();unfocus();model.resetLocal();})));}return editor;}
    public double preferredHeight(double available){if(!model.hasControls()){return 0;}if(model.global.getValue()&&!inheritedLast){FocusedInput.commit();unfocus();}inheritedLast=model.global.getValue();double content=22+(model.global.getValue()?0:22+editor().preferredHeight(available));return SettingsSection.HEADER+header.bodyHeight(content);}
    @Override public void render(GuiGraphicsExtractor g,Font font,double mx,double my,double dt,double alpha){
        if(!model.hasControls()){return;}
        header.setBounds(x,y,width,SettingsSection.HEADER);header.render(g,font,mx,my,dt,alpha);
        double body=height-SettingsSection.HEADER;if(body<=.01){return;}
        dev.riserecode.util.RenderUtil.scissorPush(g,x,y+SettingsSection.HEADER,width,body);
        try{
            global.setBounds(x,y+SettingsSection.HEADER,width,22);global.render(g,font,header.isExpanded()?mx:-1e6,header.isExpanded()?my:-1e6,dt,alpha);
            if(!model.global.getValue()){reset.setBounds(x,y+SettingsSection.HEADER+22,Math.min(width,120),17);reset.render(g,font,header.isExpanded()?mx:-1e6,header.isExpanded()?my:-1e6,dt,alpha);var view=editor();view.setBounds(x,y+SettingsSection.HEADER+44,width,view.preferredHeight(width));view.render(g,font,header.isExpanded()?mx:-1e6,header.isExpanded()?my:-1e6,dt,alpha);}
        }finally{dev.riserecode.util.RenderUtil.scissorPop(g);}
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(!model.hasControls()||!contains(mx,my)){return false;}if(header.mouseClicked(mx,my,button)){return true;}
        if(!header.isExpanded()){return true;}if(my>=global.getBounds().y()&&my<global.getBounds().y()+22&&global.getBounds().y()+22<=y+height+.01&&global.mouseClicked(mx,my,button)){return true;}
        return !model.global.getValue()&&(reset.mouseClicked(mx,my,button)||editor().mouseClicked(mx,my,button));
    }
    @Override public boolean mouseDragged(double mx,double my,int button){return header.isExpanded()&&!model.global.getValue()&&editor!=null&&editor.mouseDragged(mx,my,button);}
    @Override public boolean mouseReleased(double mx,double my,int button){return editor!=null&&editor.mouseReleased(mx,my,button);}
    public boolean charTyped(CharacterEvent event){return header.isExpanded()&&!model.global.getValue()&&editor!=null&&editor.charTyped(event);}
    public boolean keyPressed(KeyEvent event,Supplier<String> clipboard,Consumer<String> copy){return header.isExpanded()&&!model.global.getValue()&&editor!=null&&editor.keyPressed(event,clipboard,copy);}
    public void unfocus(){if(editor!=null){editor.unfocus();}}
}
