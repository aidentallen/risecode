package dev.riserecode.ui.component;

import dev.riserecode.ui.menu.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.util.*;

/** Retained preview controls and acquisition/import modes with native field drafts. */
public final class CosmeticDetailsScreen extends RetainedScreen {
    private final CosmeticsEditor editor;
    private EditBox path;
    private EditBoxDraft pathDraft;
    private Button acquire,equip,view,status,back,pose,pause;
    private final List<AbstractWidget> acquisition=new ArrayList<>(),controls=new ArrayList<>();
    private String localStatus="",lastStatus="";
    private boolean importing,controlsView;
    private long previousFrame;
    public CosmeticDetailsScreen(Screen parent,CosmeticsEditor editor){super(parent,"Cosmetic preview & acquisition");this.editor=editor;}
    private String status(){return localStatus.isEmpty()?editor.manager().library().status():localStatus;}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". Left/Right rotate. Up/Down zoom. R resets view. Import PNG cape path. "+status());}
    public EditBox path(){return path;}
    public boolean controlsView(){return controlsView;}
    public String fullStatus(){return status();}
    @Override protected void repositionElements(){if(path!=null){pathDraft=EditBoxDraft.capture(path);}super.repositionElements();}
    private Button button(String label,int x,int y,int w,Runnable action){return addRenderableWidget(Button.builder(Component.literal(label),b->action.run()).bounds(x,y,w,20).build());}
    @Override protected void init(){
        acquisition.clear();controls.clear();lastStatus="";previousFrame=0;
        button("Back",12,8,60,this::onClose);
        view=button(controlsView?"Acquire / import":"View controls",78,8,112,()->{controlsView=!controlsView;if(path.isFocused()){setFocused(view);}updateView();});
        acquire=button("Download / retry",width-120,8,108,()->{var lib=editor.manager().library();var def=lib.find(editor.selectedId());localStatus="";if(lib.busy()){lib.cancel();}else if(def!=null){lib.download(def);}});acquisition.add(acquire);
        equip=button("Select in draft",width-132,height-114,120,()->{editor.select(editor.selectedId());localStatus="Selected in draft · Back, then Apply when ready";});acquisition.add(equip);
        pose=button("Pose: "+editor.previewPose(),12,height-114,Math.max(80,width-152),this::choosePose);acquisition.add(pose);
        status=button("Status / error details",12,height-89,width-24,()->minecraft.gui.setScreen(new InfoScreen(this,"Cosmetic status",status())));acquisition.add(status);
        path=addRenderableWidget(new EditBox(font,12,height-52,Math.max(80,width-132),20,Component.literal("PNG cape path")));path.setMaxLength(4096);path.setHint(Component.literal("PNG cape path…"));if(pathDraft!=null){pathDraft.restore(path);}acquisition.add(path);
        acquisition.add(button("Import PNG",width-112,height-52,100,this::importCape));
        controls.add(button("Rotate left",12,height-88,90,()->editor.turn(-15)));controls.add(button("Rotate right",106,height-88,90,()->editor.turn(15)));
        var reset=button("Reset view",width-120,height-88,108,editor::resetPreviewView);reset.setTooltip(Tooltip.create(Component.literal("Reset rotation and zoom; outfit and pose stay unchanged")));controls.add(reset);
        controls.add(button("Zoom out",12,height-62,90,()->editor.zoomBy(-.08)));controls.add(button("Zoom in",106,height-62,90,()->editor.zoomBy(.08)));
        controls.add(button("Choose pose",width-120,height-62,108,this::choosePose));
        pause=button(editor.previewPaused()?"Resume animation":"Pause animation",12,height-36,150,()->editor.previewPaused(!editor.previewPaused()));controls.add(pause);
        back=button("Back to outfit",width/2-65,height-26,130,this::onClose);updateView();if(pathDraft!=null&&pathDraft.focused()&&!controlsView){setInitialFocus(path);pathDraft.restore(path);}
    }
    @Override protected void setInitialFocus(){if(pathDraft!=null&&pathDraft.focused()&&!controlsView){setInitialFocus(path);pathDraft.restore(path);}else{super.setInitialFocus();}}
    private void updateView(){for(var widget:acquisition){widget.visible=!controlsView;}for(var widget:controls){widget.visible=controlsView;}if(back!=null){back.setRectangle(controlsView?136:130,20,controlsView?width-148:width/2-65,height-(controlsView?36:26));}}
    private void choosePose(){minecraft.gui.setScreen(new ChoiceScreen<>(this,"Preview pose",List.of(CosmeticsEditor.PreviewPose.values()),editor.previewPose(),editor::previewPose));}
    private void importCape(){if(importing)return;long token=editor.revision();try{importing=true;localStatus="Importing cape…";editor.manager().library().importCape(Path.of(path.getValue())).whenComplete((def,error)->minecraft.execute(()->{importing=false;if(error!=null){localStatus="Import failed: "+(error.getCause()==null?error.getMessage():error.getCause().getMessage());}else if(editor.revision()==token){editor.select(def.id());localStatus="Cape imported and selected in draft";}else{localStatus="Cape imported to library; outfit was left unchanged";}}));}catch(RuntimeException error){importing=false;localStatus="Enter a valid PNG path";}}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        long now=System.nanoTime();editor.advancePreview(previousFrame==0?0:(now-previousFrame)*1e-9);previousFrame=now;
        var lib=editor.manager().library();var def=lib.find(editor.selectedId());boolean available=lib.available(def);acquire.active=lib.busy()||def!=null&&!available;acquire.setMessage(Component.literal(lib.busy()?"Cancel download":available?"Installed":"Download / retry"));equip.active=available;
        view.setMessage(Component.literal(controlsView?"Acquire / import":"View controls"));pose.setMessage(Component.literal("Pose: "+dev.riserecode.ui.theme.Theme.displayName(editor.previewPose().name())+" ›"));pause.setMessage(Component.literal(editor.previewPaused()?"Resume animation":"Pause animation"));
        String current=status();if(!current.equals(lastStatus)){lastStatus=current;status.setMessage(Component.literal("Status details: "+current));status.setTooltip(Tooltip.create(Component.literal(current)));}
        super.extractRenderState(g,mx,my,partial);
        String name=def==null?"Choose an item in Browse; PNG cape import is available.":def.name()+" · "+def.license();ScreenText.draw(g,font,dev.riserecode.util.RenderUtil.ellipsize(font,name,(width-24)/.7),12,34,.7,0xFFFFFFFF);
        ScreenText.draw(g,font,controlsView?"Arrows: view · R: reset":"PNG cape path (64×32 or scaled)",12,52,.6,0xFFB7CBD9);
        editor.renderAvatar(g,avatarBounds());
        if(controlsView&&dev.riserecode.ui.theme.Theme.bool("Reduced Motion",false)){ScreenText.draw(g,font,"Reduced Motion: animation paused",12,height-108,.6,0xFFB7CBD9);}
    }
    public UIComponent.Bounds avatarBounds(){return new UIComponent.Bounds(12,70,width-24,Math.max(1,height-(controlsView?172:190)-(controlsView&&dev.riserecode.ui.theme.Theme.bool("Reduced Motion",false)?18:0)));}
    @Override public boolean keyPressed(KeyEvent e){if(!path.isFocused()){if(e.key()==262||e.key()==263){editor.turn(e.key()==262?15:-15);return true;}if(e.key()==265||e.key()==264){editor.zoomBy(e.key()==265?.08:-.08);return true;}if(e.key()==82){editor.resetPreviewView();return true;}}return super.keyPressed(e);}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){var b=avatarBounds();if(x>=b.x()&&x<b.x()+b.width()&&y>=b.y()&&y<b.y()+b.height()){editor.zoomBy(v*.08);return true;}return super.mouseScrolled(x,y,h,v);}
}
