package dev.riserecode.map;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** One editor used from the map and manager. Editing validates the whole destination before commit. */
public final class WaypointEditScreen extends Screen {
    private final Screen parent;private final MapService maps;private final WaypointStore.Waypoint point;private final List<EditBox> fields=new ArrayList<>();private String error="";
    private static final List<String> LABELS=List.of("Name","X","Y","Z","Dimension","Group","Color");
    private List<dev.riserecode.ui.menu.EditBoxDraft> drafts=List.of();
    private int focused=-1,validationOwner=-1;
    private Button saveButton,cancelButton;
    private enum DimensionChoice {
        OVERWORLD("minecraft:overworld"), NETHER("minecraft:the_nether"), THE_END("minecraft:the_end"), CUSTOM_ID("");
        final String id;DimensionChoice(String id){this.id=id;}
    }
    private enum ColorChoice {
        MINT("64D8C4"), BLUE("64A8FF"), RED("FF6B6B"), GOLD("FFD166"), PURPLE("C792EA"), WHITE("FFFFFF"), CUSTOM_HEX("");
        final String hex;ColorChoice(String hex){this.hex=hex;}
    }
    private void captureDrafts(){if(fields.isEmpty()){return;}focused=fields.indexOf(getFocused());if(getFocused()==saveButton){focused=7;}else if(getFocused()==cancelButton){focused=8;}drafts=fields.stream().map(dev.riserecode.ui.menu.EditBoxDraft::capture).toList();}
    @Override public void removed(){captureDrafts();}






    private final dev.riserecode.ui.menu.ScreenText.Layout errorLayout=new dev.riserecode.ui.menu.ScreenText.Layout();
    public WaypointEditScreen(Screen parent,MapService maps,WaypointStore.Waypoint point){super(Component.literal("Edit waypoint"));this.parent=parent;this.maps=maps;this.point=point;}
    @Override protected void repositionElements(){captureDrafts();super.repositionElements();}
    @Override protected void setInitialFocus(){if(fields.isEmpty()){return;}if(drafts.isEmpty()){setInitialFocus(fields.getFirst());}else{if(focused>=0){setInitialFocus(focused<7?fields.get(focused):(focused==7?saveButton:cancelButton));}else{clearFocus();}for(int i=0;i<fields.size();i++){drafts.get(i).restore(fields.get(i));}}}
    @Override protected void init(){
        List<String> values=!drafts.isEmpty()?drafts.stream().map(dev.riserecode.ui.menu.EditBoxDraft::value).toList():fields.isEmpty()?List.of(point.name(),""+point.x(),""+point.y(),""+point.z(),point.dimension(),point.group(),String.format(java.util.Locale.ROOT,"%06X",point.color()&0xFFFFFF)):fields.stream().map(EditBox::getValue).toList();fields.clear();
        boolean compact=height<300;int columns=compact?2:1,rows=(LABELS.size()+columns-1)/columns,cellWidth=Math.min(240,(width-36)/columns),left=(width-columns*cellWidth)/2,top=24;
        int rowHeight=compact?Math.min(32,Math.max(24,(height-78)/rows)):27;
        for(int i=0;i<LABELS.size();i++){
            int column=compact?i%2:0,row=compact?i/2:i,origin=left+column*cellWidth;
            var box=new EditBox(font,origin,top+row*rowHeight+9,cellWidth-8,Math.max(15,rowHeight-11),Component.literal(LABELS.get(i)));
            box.setMaxLength(i==4?160:80);box.setValue(values.get(i));fields.add(box);int index=i;
            addRenderableWidget(box);box.setResponder(value->{if(validationOwner==index){error=validation(index);}});
        }
        int footer=height-26,buttonWidth=Math.min(105,(width-36)/2);
        saveButton=addRenderableWidget(Button.builder(Component.literal("Save"),b->save()).bounds(width/2-buttonWidth-4,footer,buttonWidth,20).build());
        cancelButton=addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(width/2+4,footer,buttonWidth,20).build());setInitialFocus();
    }
    private String validation(int index){
        String value=fields.get(index).getValue();
        if(index==0){return value.isBlank()?"Name: enter a waypoint name":"";}
        if(index>=1&&index<=3){int limit=index==2?2048:30_000_000;try{if(Math.abs((long)Integer.parseInt(value))<=limit){return "";}}catch(NumberFormatException ignored){}return LABELS.get(index)+": enter a whole number from -"+limit+" to "+limit;}
        if(index==4){try{net.minecraft.resources.Identifier.parse(value);return "";}catch(RuntimeException invalid){return "Dimension: enter an ID such as minecraft:overworld";}}
        if(index==6&&!value.replace("#","").matches("[0-9a-fA-F]{6}")){return "Color: enter six hex digits, e.g. 64D8C4";}
        return "";
    }
    private void save(){
        for(int i=0;i<fields.size();i++){String invalid=validation(i);if(!invalid.isEmpty()){validationOwner=i;error=invalid;setFocused(fields.get(i));minecraft.getNarrator().saySystemNow(error);return;}}
        try{
            var p=new WaypointStore.Waypoint(point.id(),fields.get(0).getValue(),Integer.parseInt(fields.get(1).getValue()),Integer.parseInt(fields.get(2).getValue()),Integer.parseInt(fields.get(3).getValue()),net.minecraft.resources.Identifier.parse(fields.get(4).getValue()).toString(),fields.get(5).getValue(),0xFF000000|Integer.parseUnsignedInt(fields.get(6).getValue().replace("#",""),16),point.visible(),point.tracked(),point.death(),point.timestamp(),point.reached());
            maps.waypoints().put(p);maps.saveWaypoints();error="";validationOwner=-1;onClose();
        }catch(IllegalArgumentException|IllegalStateException invalid){validationOwner=-1;error=invalid.getMessage();minecraft.getNarrator().saySystemNow(error);}
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float d){dev.riserecode.ui.menu.MenuBackdrop.draw(g);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        super.extractRenderState(g,mx,my,d);double scale=dev.riserecode.ui.theme.Theme.number("Font Scale",1);
        dev.riserecode.ui.menu.ScreenText.draw(g,font,title.getString(),12,8,Math.min(1.1,12/(9*scale)),0xFFFFFFFF);
        var color=fields.get(6).getValue().replace("#","");if(color.matches("[0-9a-fA-F]{6}")){int tint=0xFF000000|Integer.parseUnsignedInt(color,16);var box=fields.get(6);g.fill(box.getX()+box.getWidth()-8,box.getY()-7,box.getX()+box.getWidth()-2,box.getY()-1,tint);}
        for(int i=0;i<fields.size();i++){dev.riserecode.ui.menu.ScreenText.draw(g,font,LABELS.get(i),fields.get(i).getX(),fields.get(i).getY()-9,Math.min(.8,1/scale),0xFFE0E8EF);}
        if(!error.isEmpty()){var lines=errorLayout.lines(font,error,width-24,.65);for(int i=0;i<Math.min(2,lines.size());i++){dev.riserecode.ui.menu.ScreenText.draw(g,font,lines.get(i),12,height-64+i*dev.riserecode.ui.menu.ScreenText.lineHeight(.65),.65,0xFFFF7788);}}
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
