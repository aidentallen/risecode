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

public final class WaypointScreen extends Screen {
    private final Screen parent;private final MapService maps;private final List<ActionButton> rows=new ArrayList<>(),toolbar;
    private final Animation scroll=Animation.ui(ThemeManager.Channel.SCROLLING,0,.18,MathUtil.Easing.OUT_QUART);
    private long revision=-1,last;private double maximum;private int page=-1,request;private boolean confirmClear;
    private List<WaypointStore.Waypoint> history=List.of();
    public WaypointScreen(Screen parent,MapService maps){
        super(Component.literal("Waypoints"));this.parent=parent;this.maps=maps;
        toolbar=List.of(new ActionButton("Add",()->{var point=maps.addAtPlayer();if(point!=null){minecraft.gui.setScreen(new WaypointEditScreen(this,maps,point));}}),
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
        for(var p:points){
            String name=(maps.waypoints().latest(p)?"Latest · ":"")+p.name()+(p.reached()?" · Reached":"")+" · "+p.x()+", "+p.y()+", "+p.z()+" · "+p.dimension().replace("minecraft:","");
            rows.add(new ActionButton(name,()->{
                minecraft.gui.setScreen(new WaypointEditScreen(this,maps,p));
            }).enabled(()->page<0));
            rows.add(new ActionButton(p.visible()?"Visible":"Hidden",()->maps.waypoints().put(p.visible(!p.visible()))).enabled(()->page<0));
            rows.add(new ActionButton(p.tracked()?"Tracking":"Track",()->maps.waypoints().put(p.tracked(!p.tracked()))).enabled(()->page<0));
            rows.add(new ActionButton("Delete",()->maps.waypoints().remove(p.id())).enabled(()->page<0));
        }revision=maps.waypoints().revision();
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){dev.riserecode.ui.menu.MenuBackdrop.draw(g);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(revision!=maps.waypoints().revision()){rebuild();}long now=System.nanoTime();double dt=last==0?.016:Math.min(.1,(now-last)*1e-9);last=now;
        maximum=Math.max(0,rows.size()/4*54-(height-78));if(scroll.getValue()>maximum){scroll.reset(maximum);}scroll.animateTo(Math.clamp(scroll.getTarget(),0,maximum));double offset=scroll.update(dt);
        RenderUtil.text(g,font,page<0?"Waypoints · recent deaths":("Death history · page "+(page+1)+" / "+maps.waypoints().archivePages()),12,10,1,Theme.TEXT);
        double bw=(width-24-15)/6.0;for(int i=0;i<toolbar.size();i++){var b=toolbar.get(i);b.setBounds(12+i*(bw+3),27,bw,21);b.render(g,font,mx,my,dt,1);}
        RenderUtil.scissorPush(g,10,56,width-20,height-66);
        try{for(int i=0;i<rows.size();i++){int row=i/4,col=i%4;var b=rows.get(i);double top=58+row*54-offset,left=col==0?12:12+(col-1)*90;
            b.setBounds(left,col==0?top:top+24,col==0?width-24:86,21);if(b.getBounds().y()+21>=56&&b.getBounds().y()<height-10){b.render(g,font,mx,my,dt,1);}
        }}finally{RenderUtil.scissorPop(g);}
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){for(var b:toolbar){if(b.mouseClicked(e.x(),e.y(),e.button())){return true;}}if(e.y()>=56&&e.y()<height-10){for(var b:List.copyOf(rows)){if(b.mouseClicked(e.x(),e.y(),e.button())){return true;}}}return true;}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){scroll.animateTo(Math.clamp(scroll.getTarget()-v*24,0,maximum));return true;}
    @Override public void onClose(){maps.saveWaypoints();minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
