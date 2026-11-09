package dev.riserecode.ui.component;

import dev.riserecode.ui.menu.RetainedScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;

/** Full-window avatar/acquisition/import workflow, independent of the client's compact pane size. */
public final class CosmeticDetailsScreen extends RetainedScreen {
    private final CosmeticsEditor editor;
    private EditBox path;
    private Button acquire,equip;
    private String localStatus="";
    private boolean importing;
    public CosmeticDetailsScreen(Screen parent,CosmeticsEditor editor){super(parent,"Cosmetic preview & acquisition");this.editor=editor;}
    @Override public Component getNarrationMessage(){return Component.literal(title.getString()+". Arrows rotate. Wheel zooms. Import PNG cape path. "+(localStatus.isEmpty()?editor.manager().library().status():localStatus));}
    public EditBox path(){return path;}
    @Override protected void init(){int left=12,w=width-24;
        addRenderableWidget(Button.builder(Component.literal("Back"),b->onClose()).bounds(left,8,60,20).build());
        acquire=addRenderableWidget(Button.builder(Component.literal("Download"),b->{var lib=editor.manager().library();var def=lib.find(editor.selectedId());localStatus="";if(lib.busy()){lib.cancel();}else if(def!=null){lib.download(def);}}).bounds(width-120,8,108,20).build());
        equip=addRenderableWidget(Button.builder(Component.literal("Select in draft"),b->{editor.select(editor.selectedId());localStatus="Selected in draft · Back, then Apply when ready";}).bounds(width-104,height-114,92,20).build());
        addRenderableWidget(Button.builder(Component.literal("Rotate left"),b->editor.turn(-15)).bounds(12,height-114,90,20).build());
        addRenderableWidget(Button.builder(Component.literal("Rotate right"),b->editor.turn(15)).bounds(110,height-114,90,20).build());
        path=addRenderableWidget(new EditBox(font,12,height-52,Math.max(80,w-108),20,Component.literal("PNG cape path")));path.setMaxLength(4096);path.setHint(Component.literal("PNG cape path…"));
        addRenderableWidget(Button.builder(Component.literal("Import PNG"),b->importCape()).bounds(width-112,height-52,100,20).build());
        addRenderableWidget(Button.builder(Component.literal("Back to outfit"),b->onClose()).bounds(width/2-65,height-26,130,20).build());
    }
    private void importCape(){if(importing)return;long token=editor.revision();try{importing=true;localStatus="Importing cape…";editor.manager().library().importCape(Path.of(path.getValue())).whenComplete((def,error)->minecraft.execute(()->{importing=false;if(error!=null){localStatus="Import failed: "+(error.getCause()==null?error.getMessage():error.getCause().getMessage());}else{if(editor.revision()==token){editor.select(def.id());localStatus="Cape imported and selected in draft";}else{localStatus="Cape imported to library; outfit was left unchanged";}}}));}catch(RuntimeException error){importing=false;localStatus="Enter a valid PNG path";}}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        super.extractRenderState(g,mx,my,partial);var lib=editor.manager().library();var def=lib.find(editor.selectedId());boolean available=lib.available(def);acquire.active=lib.busy()||def!=null&&!available;acquire.setMessage(Component.literal(lib.busy()?"Cancel download":available?"Installed":"Download / retry"));equip.active=available;
        String name=def==null?"Choose an item in Browse; local cape import is available below.":def.name()+" · "+def.license();dev.riserecode.ui.menu.ScreenText.draw(g,font,dev.riserecode.util.RenderUtil.ellipsize(font,name,(width-24)/.7),12,34,.7,0xFFFFFFFF);
        dev.riserecode.ui.menu.ScreenText.draw(g,font,"Arrows rotate · Wheel zooms",12,52,.65,0xFFB7CBD9);
        editor.renderAvatar(g,new UIComponent.Bounds(12,70,width-24,Math.max(1,height-190)));
        String status=localStatus.isEmpty()?lib.status():localStatus;var lines=dev.riserecode.ui.menu.ScreenText.wrap(font,status,width-24,.7);for(int i=0;i<Math.min(2,lines.size());i++)dev.riserecode.ui.menu.ScreenText.draw(g,font,lines.get(i),12,height-89+i*dev.riserecode.ui.menu.ScreenText.lineHeight(.7),.7,0xFFE6EDF4);

    }
    @Override public boolean keyPressed(KeyEvent e){if(!path.isFocused()&&(e.key()==262||e.key()==263)){editor.turn(e.key()==262?15:-15);return true;}return super.keyPressed(e);}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){editor.zoomBy(v*.08);return true;}
}
