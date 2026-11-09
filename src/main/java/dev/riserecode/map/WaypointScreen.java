package dev.riserecode.map;

import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.MathUtil;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class WaypointScreen extends Screen implements dev.riserecode.ui.component.KeyboardNavigation.ManagedScreen {
    private final dev.riserecode.ui.component.KeyboardNavigation navigation = new dev.riserecode.ui.component.KeyboardNavigation();
    private final Screen parent;private final MapService maps;private final List<ActionButton> rows=new ArrayList<>(),toolbar;
    private final Animation scroll=Animation.ui(ThemeManager.Channel.SCROLLING,0,.18,MathUtil.Easing.OUT_QUART);
    private long revision=-1,last;private double maximum;private int page=-1,request;private boolean confirmClear;
    private List<WaypointStore.Waypoint> history=List.of();
    private final Map<UUID,WaypointStore.Waypoint> current=new LinkedHashMap<>();
    private final Map<UUID,List<ActionButton>> cache=new HashMap<>();
    private Object returnFocus;
    private double bodyTop=56,rowHeight=54;
    @Override public dev.riserecode.ui.component.KeyboardNavigation riserecode$keyboardNavigation(){return navigation;}
    public String focusedControlLabel(){return navigation.focusedLabel();}
    public List<ActionButton> getRowActions(){return List.copyOf(rows);}
    @Override protected void init(){if(returnFocus!=null){navigation.focus(returnFocus);returnFocus=null;}}
    private WaypointStore.Waypoint point(UUID id){return current.get(id);}
    private List<ActionButton> makeActions(UUID id){
        return List.of(new ActionButton(()->{var p=point(id);return p.name()+" · "+p.x()+", "+p.y()+", "+p.z()+" · "+p.dimension().replace("minecraft:","");},()->{returnFocus=cache.get(id).get(0);minecraft.gui.setScreen(new WaypointEditScreen(this,maps,point(id)));}).enabled(()->page<0).accessibleLabel(()->point(id).name()+", edit waypoint"),
            new ActionButton(()->point(id).visible()?"Visible":"Hidden",()->{var p=point(id);maps.waypoints().put(p.visible(!p.visible()));}).enabled(()->page<0).accessibleLabel(()->point(id).name()+", visibility, "+(point(id).visible()?"visible":"hidden")),
            new ActionButton(()->point(id).tracked()?"Tracking":"Track",()->{var p=point(id);maps.waypoints().put(p.tracked(!p.tracked()));}).enabled(()->page<0).accessibleLabel(()->point(id).name()+", tracking, "+point(id).tracked()),
            new ActionButton("Delete",()->{var p=point(id);minecraft.gui.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(confirmed->{Object target=cache.get(id).get(3);if(confirmed){var ids=new ArrayList<>(current.keySet());int index=ids.indexOf(id);maps.waypoints().remove(id);maps.saveWaypoints();rebuild();target=rows.isEmpty()?toolbar.getLast():rows.get(Math.min(index,rows.size()/4-1)*4);}returnFocus=target;minecraft.gui.setScreen(this);},Component.literal("Delete waypoint?"),Component.literal(p.name())));}).enabled(()->page<0).accessibleLabel(()->point(id).name()+", delete waypoint"));
    }
    public WaypointScreen(Screen parent,MapService maps){
        super(Component.literal("Waypoints"));this.parent=parent;this.maps=maps;
        toolbar=List.of(new ActionButton("Add",()->{var player=maps.minecraft().player;var point=player==null||!maps.ready()?null:new WaypointStore.Waypoint(UUID.randomUUID(),"Waypoint "+(maps.waypoints().all().size()+1),player.blockPosition().getX(),player.blockPosition().getY(),player.blockPosition().getZ(),maps.dimension(),"",0xFF64D8C4,true,false,false);if(point!=null){minecraft.gui.setScreen(new WaypointEditScreen(this,maps,point));}}),
                new ActionButton(()->maps.waypoints().hideReached()?"Show reached":"Hide reached",()->maps.waypoints().hideReached(!maps.waypoints().hideReached())),
                new ActionButton(()->page<0?"History":"Recent",this::history),
                new ActionButton("Next page",this::next),
                new ActionButton(()->confirmClear?"Confirm clear":"Clear deaths",()->{if(confirmClear){maps.clearDeaths();page=-1;history=List.of();confirmClear=false;}else{confirmClear=true;}}),new ActionButton("Done",this::onClose));
    }
    private void history(){if(page>=0){page=-1;history=List.of();request++;rebuild();}else if(maps.waypoints().archivePages()>0){load(0);}}
    private void next(){if(maps.waypoints().archivePages()>0){load(page<0?0:(page+1)%maps.waypoints().archivePages());}}
    private void load(int next){page=next;history=List.of();int stamp=++request;maps.loadDeathPage(page,loaded->{if(stamp==request){history=loaded;rebuild();}});scroll.reset(0);rebuild();}
    private void rebuild(){
        rows.clear();List<WaypointStore.Waypoint> points=page<0?maps.waypoints().displayed():history.stream().filter(p->!maps.waypoints().hideReached()||!p.reached()).toList();
        current.clear();for(var p:points){current.put(p.id(),p);rows.addAll(cache.computeIfAbsent(p.id(),this::makeActions));}cache.keySet().retainAll(current.keySet());
        revision=maps.waypoints().revision();
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){dev.riserecode.ui.menu.MenuBackdrop.draw(g);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        navigation.begin(); try {
        if(revision!=maps.waypoints().revision()){rebuild();}long now=System.nanoTime();double dt=last==0?.016:Math.min(.1,(now-last)*1e-9);last=now;
        var flow=dev.riserecode.ui.component.ButtonMetrics.flow(font,toolbar.stream().map(ActionButton::label).toList(),width-24);bodyTop=27+dev.riserecode.ui.component.ButtonMetrics.flowHeight(flow)+8;rowHeight=Math.max(54,9*Theme.number("Font Scale",1)+40);
        maximum=Math.max(0,rows.size()/4*rowHeight-(height-bodyTop-12));if(scroll.getValue()>maximum){scroll.reset(maximum);}scroll.animateTo(Math.clamp(scroll.getTarget(),0,maximum));double offset=scroll.update(dt);
        RenderUtil.text(g,font,page<0?"Waypoints · recent deaths":("Death history · page "+(page+1)+" / "+maps.waypoints().archivePages()),12,10,1,Theme.TEXT);
        for(int i=0;i<toolbar.size();i++){var b=toolbar.get(i);var slot=flow.get(i);b.setBounds(12+slot.x(),27+slot.y(),slot.width(),slot.height());b.render(g,font,mx,my,dt,1);}
        dev.riserecode.ui.component.KeyboardNavigation.pushScrollRegion(bounds->{double shift=bounds.y()<bodyTop?bounds.y()-bodyTop:Math.max(0,bounds.y()+bounds.height()-height+10);if(shift!=0){scroll.reset(Math.clamp(scroll.getValue()+shift,0,maximum));}});
        RenderUtil.scissorPush(g,10,bodyTop,width-20,Math.max(0,height-bodyTop-10));
        try{for(int i=0;i<rows.size();i++){int row=i/4,col=i%4;var b=rows.get(i);double top=bodyTop+2+row*rowHeight-offset,left=col==0?12:12+(col-1)*90;
            b.setBounds(left,col==0?top:top+rowHeight-26,col==0?width-24:86,21);if(dev.riserecode.ui.component.KeyboardNavigation.collectingOffscreen()||b.getBounds().y()+21>=bodyTop&&b.getBounds().y()<height-10){b.render(g,font,mx,my,dt,1);}
        }}finally{RenderUtil.scissorPop(g);dev.riserecode.ui.component.KeyboardNavigation.popScrollRegion();}
        } finally { navigation.end(); }
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){for(var b:toolbar){if(b.mouseClicked(e.x(),e.y(),e.button())){return true;}}if(e.y()>=bodyTop&&e.y()<height-10){for(var b:List.copyOf(rows)){if(b.mouseClicked(e.x(),e.y(),e.button())){return true;}}}return true;}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll.animateTo(Math.clamp(scroll.getTarget()-v*24,0,maximum));return true;}
    @Override public boolean keyPressed(KeyEvent e){if(navigation.key(e)){return true;}if(e.key()==266||e.key()==267){scroll.animateTo(Math.clamp(scroll.getTarget()+(e.key()==266?-1:1)*(height-90),0,maximum));return true;}return super.keyPressed(e);}
    @Override public void onClose(){maps.saveWaypoints();minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
