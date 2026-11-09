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
    private Button saveButton,cancelButton,dimensionButton,colorButton;
    private enum DimensionChoice {
        OVERWORLD("minecraft:overworld"), NETHER("minecraft:the_nether"), THE_END("minecraft:the_end"), CUSTOM_ID("");
        final String id;DimensionChoice(String id){this.id=id;}
    }
    private enum ColorChoice {
        MINT("64D8C4"), BLUE("64A8FF"), RED("FF6B6B"), GOLD("FFD166"), PURPLE("C792EA"), WHITE("FFFFFF"), CUSTOM_HEX("");
        final String hex;ColorChoice(String hex){this.hex=hex;}
    }
    private void captureDrafts(){if(fields.isEmpty()){return;}focused=fields.indexOf(getFocused());if(getFocused()==saveButton){focused=7;}else if(getFocused()==cancelButton){focused=8;}else if(getFocused()==dimensionButton){focused=9;}else if(getFocused()==colorButton){focused=10;}drafts=fields.stream().map(dev.riserecode.ui.menu.EditBoxDraft::capture).toList();}
    @Override public void removed(){captureDrafts();}
    private String choiceCaption(int index,String value){
        if(index==4){return Arrays.stream(DimensionChoice.values()).filter(v->v!=DimensionChoice.CUSTOM_ID&&v.id.equals(value)).map(v->dev.riserecode.ui.theme.Theme.displayName(v.name())+" ›").findFirst().orElse("Worlds ›");}
        return Arrays.stream(ColorChoice.values()).filter(v->v!=ColorChoice.CUSTOM_HEX&&v.hex.equalsIgnoreCase(value.replace("#",""))).map(v->dev.riserecode.ui.theme.Theme.displayName(v.name())+" ›").findFirst().orElse("Colors ›");
    }
    private void chooseDimension(){captureDrafts();focused=9;var selected=Arrays.stream(DimensionChoice.values()).filter(v->v.id.equals(fields.get(4).getValue())).findFirst().orElse(null);minecraft.gui.setScreen(new dev.riserecode.ui.menu.ChoiceScreen<>(this,"Choose dimension",List.of(DimensionChoice.values()),selected,v->{if(v==DimensionChoice.CUSTOM_ID){editAdvanced(4);}else{applyChoice(4,v.id);}}).afterReturn());}
    private void chooseColor(){captureDrafts();focused=10;var selected=Arrays.stream(ColorChoice.values()).filter(v->v.hex.equalsIgnoreCase(fields.get(6).getValue().replace("#",""))).findFirst().orElse(null);minecraft.gui.setScreen(new dev.riserecode.ui.menu.ChoiceScreen<>(this,"Choose color",List.of(ColorChoice.values()),selected,v->{if(v==ColorChoice.CUSTOM_HEX){editAdvanced(6);}else{applyChoice(6,v.hex);}}).afterReturn());}
    private void editAdvanced(int index){captureDrafts();focused=index==4?9:10;minecraft.gui.setScreen(new dev.riserecode.ui.menu.TextValueScreen(this,index==4?"Custom dimension ID":"Custom hex color",dev.riserecode.ui.menu.EditBoxDraft.capture(fields.get(index)),index==4?160:80,value->{value.restore(fields.get(index));var next=new ArrayList<>(drafts);next.set(index,value);drafts=List.copyOf(next);}));}
    private void applyChoice(int index,String value){fields.get(index).setValue(value);var copy=new ArrayList<>(drafts);copy.set(index,new dev.riserecode.ui.menu.EditBoxDraft(value,value.length(),value.length(),false));drafts=List.copyOf(copy);}
    private final dev.riserecode.ui.menu.ScreenText.Layout errorLayout=new dev.riserecode.ui.menu.ScreenText.Layout();
    public WaypointEditScreen(Screen parent,MapService maps,WaypointStore.Waypoint point){super(Component.literal("Edit waypoint"));this.parent=parent;this.maps=maps;this.point=point;}
    @Override protected void repositionElements(){captureDrafts();super.repositionElements();}
    @Override protected void setInitialFocus(){if(fields.isEmpty()){return;}if(drafts.isEmpty()){setInitialFocus(fields.getFirst());}else{if(focused>=0){setInitialFocus(focused<7?fields.get(focused):switch(focused){case 7->saveButton;case 8->cancelButton;case 9->dimensionButton;default->colorButton;});}else{clearFocus();}for(int i=0;i<fields.size();i++){drafts.get(i).restore(fields.get(i));}}}
    @Override protected void init(){
        List<String> values=!drafts.isEmpty()?drafts.stream().map(dev.riserecode.ui.menu.EditBoxDraft::value).toList():fields.isEmpty()?List.of(point.name(),""+point.x(),""+point.y(),""+point.z(),point.dimension(),point.group(),String.format(java.util.Locale.ROOT,"%06X",point.color()&0xFFFFFF)):fields.stream().map(EditBox::getValue).toList();fields.clear();
        boolean compact=height<300;int columns=compact?2:1,rows=(LABELS.size()+columns-1)/columns,cellWidth=Math.min(240,(width-36)/columns),left=(width-columns*cellWidth)/2,top=24;
        int rowHeight=compact?Math.min(32,Math.max(24,(height-78)/rows)):27;
        for(int i=0;i<LABELS.size();i++){
            int column=compact?i%2:0,row=compact?i/2:i;
            boolean choice=i==4||i==6;int choiceWidth=choice?82:0,origin=left+column*cellWidth;
            var box=new EditBox(font,origin+choiceWidth,top+row*rowHeight+9,cellWidth-8-choiceWidth,Math.max(15,rowHeight-11),Component.literal(LABELS.get(i)));
            box.setMaxLength(i==4?160:80);box.setValue(values.get(i));fields.add(box);int index=i;
            Button picker;
            if(choice){picker=Button.builder(Component.literal(choiceCaption(index,box.getValue())),b->{if(index==4){chooseDimension();}else{chooseColor();}}).bounds(origin,box.getY(),78,box.getHeight()).createNarration(supplier->Component.literal((index==4?"Choose dimension. ":"Choose color. ")+choiceCaption(index,box.getValue())+". Custom value: "+box.getValue())).build();picker.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(index==4?"Choose a named world":"Choose a named color")));addRenderableWidget(picker);if(index==4){dimensionButton=picker;}else{colorButton=picker;}box.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(index==4?"Advanced: custom dimension ID":"Advanced: six-digit hex color")));}else{picker=null;}
            addRenderableWidget(box);box.setResponder(value->{if(picker!=null){picker.setMessage(Component.literal(choiceCaption(index,value)));}if(validationOwner==index){error=validation(index);}});
        }
        int footer=height-26,buttonWidth=Math.min(105,(width-36)/2);
        saveButton=addRenderableWidget(Button.builder(Component.literal("Save"),b->save()).bounds(width/2-buttonWidth-4,footer,buttonWidth,20).build());
        cancelButton=addRenderableWidget(Button.builder(Component.literal("Cancel"),b->onClose()).bounds(width/2+4,footer,buttonWidth,20).build());setInitialFocus();
    }
    private String validation(int index){
        String value=fields.get(index).getValue();
        if(index==0){return value.isBlank()?"Name: enter a waypoint name":"";}
        if(index>=1&&index<=3){int limit=index==2?2048:30_000_000;try{if(Math.abs((long)Integer.parseInt(value))<=limit){return "";}}catch(NumberFormatException ignored){}return LABELS.get(index)+": enter a whole number from -"+limit+" to "+limit;}
        if(index==4){try{net.minecraft.resources.Identifier.parse(value);return "";}catch(RuntimeException invalid){return "Dimension: choose a world or enter an ID such as minecraft:overworld";}}
        if(index==6&&!value.replace("#","").matches("[0-9a-fA-F]{6}")){return "Color: choose a color or enter six hex digits, e.g. 64D8C4";}
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
        for(int i=0;i<fields.size();i++){dev.riserecode.ui.menu.ScreenText.draw(g,font,LABELS.get(i),i==4||i==6?fields.get(i).getX()-82:fields.get(i).getX(),fields.get(i).getY()-9,Math.min(.8,1/scale),0xFFE0E8EF);}
        for(int i:new int[]{4,6}){dev.riserecode.ui.menu.ScreenText.draw(g,font,i==4?"ID":"Hex",fields.get(i).getX(),fields.get(i).getY()-9,Math.min(.6,.7/scale),dev.riserecode.ui.theme.Theme.SECONDARY);}
        if(!error.isEmpty()){var lines=errorLayout.lines(font,error,width-24,.65);for(int i=0;i<Math.min(2,lines.size());i++){dev.riserecode.ui.menu.ScreenText.draw(g,font,lines.get(i),12,height-64+i*dev.riserecode.ui.menu.ScreenText.lineHeight(.65),.65,0xFFFF7788);}}
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
