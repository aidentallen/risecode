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
    @Override protected void init(){
        List<String> values=fields.isEmpty()?List.of(point.name(),""+point.x(),""+point.y(),""+point.z(),point.dimension(),point.group(),String.format(java.util.Locale.ROOT,"%06X",point.color()&0xFFFFFF)):fields.stream().map(EditBox::getValue).toList();fields.clear();
        int top=Math.max(28,(height-228)/2);for(int i=0;i<LABELS.size();i++){var box=new EditBox(font,width/2-40,top+i*25,150,20,Component.literal(LABELS.get(i)));box.setMaxLength(i==4?160:80);box.setValue(values.get(i));fields.add(addRenderableWidget(box));}
        addRenderableWidget(Button.builder(Component.literal("Save"),b->save()).bounds(width/2-110,top+180,105,20).build());addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(width/2+5,top+180,105,20).build());setInitialFocus(fields.getFirst());
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
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float d){super.extractRenderState(g,mx,my,d);g.text(font,title,width/2-110,12,0xFFFFFFFF);for(int i=0;i<fields.size();i++){g.text(font,LABELS.get(i),width/2-110,fields.get(i).getY()+6,0xFFE0E8EF);}if(!error.isEmpty()){g.text(font,error,12,height-16,0xFFFF7777);}}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
