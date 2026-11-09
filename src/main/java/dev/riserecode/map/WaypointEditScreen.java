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
    public WaypointEditScreen(Screen parent,MapService maps,WaypointStore.Waypoint point){super(Component.literal("Edit waypoint"));this.parent=parent;this.maps=maps;this.point=point;}
    @Override protected void setInitialFocus(){if(!fields.isEmpty()){setInitialFocus(fields.getFirst());}}
    @Override protected void init(){
        List<String> values=fields.isEmpty()?List.of(point.name(),""+point.x(),""+point.y(),""+point.z(),point.dimension(),point.group(),String.format(java.util.Locale.ROOT,"%06X",point.color()&0xFFFFFF)):fields.stream().map(EditBox::getValue).toList();fields.clear();
        boolean compact=height<300;int columns=compact?2:1,rows=(LABELS.size()+columns-1)/columns,cellWidth=Math.min(240,(width-36)/columns),left=(width-columns*cellWidth)/2,top=24;
        int rowHeight=compact?Math.min(32,Math.max(24,(height-78)/rows)):27;
        for(int i=0;i<LABELS.size();i++){
            int column=compact?i%2:0,row=compact?i/2:i;
            var box=new EditBox(font,left+column*cellWidth,top+row*rowHeight+9,cellWidth-8,Math.max(15,rowHeight-11),Component.literal(LABELS.get(i)));
            box.setMaxLength(i==4?160:80);box.setValue(values.get(i));fields.add(addRenderableWidget(box));
        }
        int footer=height-26,buttonWidth=Math.min(105,(width-36)/2);
        addRenderableWidget(Button.builder(Component.literal("Save"),b->save()).bounds(width/2-buttonWidth-4,footer,buttonWidth,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(width/2+4,footer,buttonWidth,20).build());setInitialFocus(fields.getFirst());
    }
    private int coordinate(int index,int limit){
        try{int value=Integer.parseInt(fields.get(index).getValue());if(Math.abs((long)value)>limit){throw new NumberFormatException();}return value;}
        catch(NumberFormatException invalid){setFocused(fields.get(index));throw new IllegalArgumentException(LABELS.get(index)+": enter a whole number from -"+limit+" to "+limit);}
    }
    private void save(){
        try{
            if(fields.get(0).getValue().isBlank()){setFocused(fields.get(0));throw new IllegalArgumentException("Name: enter a waypoint name");}
            int x=coordinate(1,30_000_000),y=coordinate(2,2048),z=coordinate(3,30_000_000);
            String dimension;
            try{dimension=net.minecraft.resources.Identifier.parse(fields.get(4).getValue()).toString();}
            catch(RuntimeException invalid){setFocused(fields.get(4));throw new IllegalArgumentException("Dimension: use a resource ID such as minecraft:overworld");}
            String color=fields.get(6).getValue().replace("#","");
            if(!color.matches("[0-9a-fA-F]{6}")){setFocused(fields.get(6));throw new IllegalArgumentException("Color: enter six hexadecimal digits, e.g. 64D8C4");}
            var p=new WaypointStore.Waypoint(point.id(),fields.get(0).getValue(),x,y,z,dimension,fields.get(5).getValue(),0xFF000000|Integer.parseUnsignedInt(color,16),point.visible(),point.tracked(),point.death(),point.timestamp(),point.reached());
            maps.waypoints().put(p);maps.saveWaypoints();error="";onClose();
        }catch(IllegalArgumentException invalid){error=invalid.getMessage();minecraft.getNarrator().saySystemNow(error);}
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float d){dev.riserecode.ui.menu.MenuBackdrop.draw(g);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){
        super.extractRenderState(g,mx,my,d);g.text(font,title,12,8,0xFFFFFFFF);
        for(int i=0;i<fields.size();i++){g.text(font,LABELS.get(i),fields.get(i).getX(),fields.get(i).getY()-9,0xFFE0E8EF);}
        if(!error.isEmpty()){var lines=font.split(Component.literal(error),width-24);for(int i=0;i<Math.min(2,lines.size());i++){g.text(font,lines.get(i),12,height-48+i*9,0xFFFF7788);}}
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
