package dev.riserecode.ui.panorama;

import java.util.ArrayList;
import java.util.List;

/** Client-owned interaction; published impulses contain no mutable engine objects. */
public final class PanoramaInteraction {
    public record Impulse(float x,float z,float born,float strength) { }
    public record Snapshot(double yaw,double pitch,double pointerX,double pointerY,List<Impulse> gusts,List<Impulse> ripples) { public Snapshot{gusts=List.copyOf(gusts);ripples=List.copyOf(ripples);} }
    private final ArrayList<Impulse> gusts=new ArrayList<>(),ripples=new ArrayList<>();
    private double yaw,pitch,px,py,downX,downY,lastX,lastY;
    private boolean pressed,dragged;
    public void reset(){yaw=pitch=0;cancel();}
    public void cancel(){pressed=dragged=false;}
    public boolean pressed(){return pressed;}
    public void press(double x,double y){pressed=true;dragged=false;downX=lastX=x;downY=lastY=y;}
    public void drag(double x,double y,double sensitivity){
        if(!pressed)return;
        if(!dragged&&Math.hypot(x-downX,y-downY)>=5){dragged=true;lastX=downX;lastY=downY;}
        if(dragged){yaw=Math.clamp(yaw+(x-lastX)*.16*sensitivity,-60,60);pitch=Math.clamp(pitch+(y-lastY)*.12*sensitivity,-15,15);}
        lastX=x;lastY=y;
    }
    public boolean release(){boolean click=pressed&&!dragged;cancel();return click;}
    public void pointer(double x,double y){px=Math.clamp(x,-1,1);py=Math.clamp(y,-1,1);}
    public void gust(float x,float z,float time,float strength){if(strength<=0)return;if(gusts.size()==8)gusts.removeFirst();gusts.add(new Impulse(x,z,time,strength));}
    public void ripple(float x,float z,float time,float strength){if(strength<=0)return;if(ripples.size()==12)ripples.removeFirst();ripples.add(new Impulse(x,z,time,strength));}
    public void expire(float time){gusts.removeIf(v->time-v.born>3);ripples.removeIf(v->time-v.born>4);}
    public Snapshot snapshot(){return new Snapshot(yaw,pitch,px,py,List.copyOf(gusts),List.copyOf(ripples));}
    public void clear(){reset();gusts.clear();ripples.clear();px=py=0;}
}
