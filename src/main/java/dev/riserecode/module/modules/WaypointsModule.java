package dev.riserecode.module.modules;
import dev.riserecode.Client;
import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.*;
import dev.riserecode.render.WorldLabels;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Waypoints as a screen overlay: a small diamond in the waypoint color that fades out when you are close,
 * a name and distance label that only unfolds near the crosshair, and a quiet arrow at the screen edge when the
 * point is out of view. The older in-world floating labels remain available as the WORLD style.
 */
public final class WaypointsModule extends Module {
    public enum Style { OVERLAY, WORLD }
    public enum LabelMode { NEAR_CROSSHAIR, ALWAYS, NEVER }
    public final EnumSetting<Style> style=register(new EnumSetting<>("Style","Screen overlay markers or floating in-world labels",Style.OVERLAY));
    public final EnumSetting<LabelMode> labelMode=register(new EnumSetting<>("Label Mode","When the name and distance are shown next to a marker",LabelMode.NEAR_CROSSHAIR));
    public final BooleanSetting edgeArrows=register(new BooleanSetting("Edge Arrows","Show a small arrow at the screen edge for waypoints out of view",true));
    public final DoubleSetting opacity=register(DoubleSetting.extended("Opacity","Marker opacity",0.85,0.2,1));
    public final DoubleSetting fadeDistance=register(DoubleSetting.extended("Fade Distance","Markers fade out when you are closer than this many blocks",12,0,64));
    public final BooleanSetting labels=register(new BooleanSetting("Labels","Show destination names",true));
    public final BooleanSetting distance=register(new BooleanSetting("Distance","Show distance in meters",true));
    public final BooleanSetting beams=register(new BooleanSetting("Beams","Show vertical destination beams",false));
    public final BooleanSetting through=register(new BooleanSetting("Through Terrain","Show labels behind terrain",true));
    public final DoubleSetting scale=register(DoubleSetting.extended("Scale","Marker size",1,0.5,3));
    public final DoubleSetting range=register(DoubleSetting.extended("Range","Maximum overlay distance",4096,64,16000));
    // hotkeys; kept in sync with the matching Minecraft key mappings by RiseRecodeClient
    public final KeySetting addKey=register(new KeySetting("Add Waypoint Key","Create a waypoint at your position",66));
    public final KeySetting managerKey=register(new KeySetting("Open Manager Key","Open the waypoint manager",-1));
    public final KeySetting trackKey=register(new KeySetting("Track Nearest Key","Track or untrack the nearest waypoint",-1));
    public final KeySetting clearTrackKey=register(new KeySetting("Stop Tracking Key","Stop tracking all waypoints",-1));
    public final KeySetting markersKey=register(new KeySetting("Toggle Markers Key","Show or hide all waypoint markers",-1));
    private static final double REVEAL_RADIUS = 70, EDGE_MARGIN = 20;
    private final Minecraft mc;
    private final Matrix4f projection = new Matrix4f(), viewRotation = new Matrix4f();
    private Vec3 cameraPosition = Vec3.ZERO;
    private boolean projectionReady;
    private final Map<UUID,Double> reveal = new HashMap<>();

    public WaypointsModule(EventBus bus,Minecraft mc){
        super("Waypoints","In-world destinations from the shared waypoint manager.",Category.WORLD,bus);this.mc=mc;
        labelMode.setVisibleWhen(()->style.getValue()==Style.OVERLAY);
        edgeArrows.setVisibleWhen(()->style.getValue()==Style.OVERLAY);
        opacity.setVisibleWhen(()->style.getValue()==Style.OVERLAY);
        fadeDistance.setVisibleWhen(()->style.getValue()==Style.OVERLAY);
        through.setVisibleWhen(()->style.getValue()==Style.WORLD||beams.getValue());
    }

    @Override protected void onEnable(){
        listen(Render3DEvent.class,this::extract);
        listen(Render2DEvent.class,this::overlay);
    }
    @Override protected void onDisable(){ reveal.clear(); projectionReady=false; }

    /** 3D phase: beams, the old floating labels, and the camera matrices the overlay needs to project points. */
    private void extract(Render3DEvent e){
        var maps=Client.INSTANCE.getMaps();
        if(mc.player==null||maps==null){return;}
        var camera=e.getContext().levelState().cameraRenderState;
        if(camera!=null&&camera.projectionMatrix!=null&&camera.viewRotationMatrix!=null){
            projection.set(camera.projectionMatrix);viewRotation.set(camera.viewRotationMatrix);cameraPosition=camera.pos;projectionReady=true;
        }
        Vec3 eye=e.getContext().camera().position();
        for(var p:maps.waypoints().all()){
            if(!p.visible()||!p.dimension().equals(maps.dimension())){continue;}
            Vec3 position=new Vec3(p.x()+0.5,p.y()+1,p.z()+0.5);double meters=mc.player.position().distanceTo(position);
            if(meters>range.getValue()){continue;}
            if(beams.getValue()){e.addLine(position,position.add(0,80,0),p.color(),p.color(),2,!through.getValue());}
            if(style.getValue()==Style.WORLD&&labels.getValue()){
                Vec3 draw=eye.add(position.subtract(eye).normalize().scale(Math.min(64,eye.distanceTo(position))));
                WorldLabels.label(e,draw,Component.literal(p.name()+(distance.getValue()?" "+Math.round(meters)+"m":"")+(p.tracked()?" ◇":"")),p.color(),scale.getValue()*Math.clamp(eye.distanceTo(position)/12,0.05,1),through.getValue(),List.of());
            }
        }
    }

    /** 2D phase: project each waypoint onto the screen and draw its marker. */
    private void overlay(Render2DEvent event){
        if(style.getValue()!=Style.OVERLAY||!projectionReady){return;}
        var maps=Client.INSTANCE.getMaps();
        if(mc.player==null||maps==null||mc.gui.screen()!=null){return;}
        var g=event.getGraphics();int width=g.guiWidth(),height=g.guiHeight();
        double centerX=width/2.0,centerY=height/2.0,dt=Math.min(0.1,event.getDeltaSeconds());
        var seen=new java.util.HashSet<UUID>();
        for(var p:maps.waypoints().all()){
            if(!p.visible()||!p.dimension().equals(maps.dimension())){continue;}
            Vec3 position=new Vec3(p.x()+0.5,p.y()+1,p.z()+0.5);double meters=mc.player.position().distanceTo(position);
            if(meters>range.getValue()){continue;}
            // quietly disappear when close, so a waypoint you are standing at does not cover the screen
            double near=fadeDistance.getValue()<=0?1:Math.clamp((meters-fadeDistance.getValue()*0.35)/(fadeDistance.getValue()*0.65),0,1);
            double alpha=opacity.getValue()*(p.tracked()?1:near);
            if(alpha<=0.02){continue;}
            Vec3 relative=position.subtract(cameraPosition);
            var view=new Vector4f((float)relative.x,(float)relative.y,(float)relative.z,1);viewRotation.transform(view);
            float viewX=view.x,viewY=view.y;
            var clip=new Vector4f(view);projection.transform(clip);
            boolean inFront=clip.w>0.0001f;
            double ndcX=inFront?clip.x/clip.w:0,ndcY=inFront?clip.y/clip.w:0;
            double sx=centerX+ndcX*centerX,sy=centerY-ndcY*centerY;
            boolean onScreen=inFront&&Math.abs(ndcX)<=1&&Math.abs(ndcY)<=1&&sx>EDGE_MARGIN*0.5&&sx<width-EDGE_MARGIN*0.5&&sy>EDGE_MARGIN*0.5&&sy<height-EDGE_MARGIN*0.5;
            if(onScreen){
                // ease the label open when the crosshair is near the marker
                double toCrosshair=Math.hypot(sx-centerX,sy-centerY);
                double goal=labelMode.getValue()==LabelMode.ALWAYS||p.tracked()&&labelMode.getValue()!=LabelMode.NEVER?1
                        :labelMode.getValue()==LabelMode.NEAR_CROSSHAIR&&toCrosshair<REVEAL_RADIUS*scale.getValue()?1:0;
                double current=reveal.getOrDefault(p.id(),0.0);
                current+=(goal-current)*Math.min(1,dt*12);seen.add(p.id());reveal.put(p.id(),current);
                marker(g,p,sx,sy,meters,alpha,labels.getValue()?current:0);
            }else if(edgeArrows.getValue()){
                double dx=inFront?ndcX:viewX,dy=inFront?ndcY:viewY;
                if(!inFront){dx=viewX;dy=viewY;}
                double length=Math.hypot(dx,dy);if(length<1e-6){dx=0;dy=-1;length=1;}
                dx/=length;dy/=length;
                double halfW=centerX-EDGE_MARGIN,halfH=centerY-EDGE_MARGIN;
                double t=Math.min(Math.abs(dx)>1e-6?halfW/Math.abs(dx):Double.MAX_VALUE,Math.abs(dy)>1e-6?halfH/Math.abs(dy):Double.MAX_VALUE);
                edgeArrow(g,p,centerX+dx*t,centerY-dy*t,Math.atan2(-dy,dx),meters,alpha*0.7);
            }
        }
        reveal.keySet().retainAll(seen);
    }

    private void marker(GuiGraphicsExtractor g,dev.riserecode.map.WaypointStore.Waypoint p,double x,double y,double meters,double alpha,double open){
        double s=scale.getValue();int color=p.color()|0xFF000000;
        // diamond: a rotated rounded square with a dark rim so it reads on any background
        g.pose().pushMatrix();g.pose().translate((float)x,(float)y);g.pose().rotate((float)(Math.PI/4));
        double size=7*s;
        RenderUtil.drawRoundedRect(g,-size/2-1.2,-size/2-1.2,size+2.4,size+2.4,1.5,Theme.opacity(0xFF0B0D10,alpha*0.75));
        RenderUtil.drawRoundedRect(g,-size/2,-size/2,size,size,1,Theme.opacity(color,alpha));
        g.pose().popMatrix();
        if(open<=0.02){return;}
        String name=p.name()+(p.tracked()?" ◇":""),dist=distance.getValue()?formatDistance(meters):"";
        double textScale=0.8*s;var font=mc.font;
        double nameWidth=RenderUtil.textWidth(font,name,false)*textScale,distWidth=dist.isEmpty()?0:RenderUtil.textWidth(font,dist,false)*textScale;
        double padding=5*s,gap=dist.isEmpty()?0:5*s;
        double width=(padding*2+nameWidth+gap+distWidth)*(0.6+0.4*open),height=13*s,left=x-width/2,top=y-size-height-4*s-(1-open)*3;
        double a=alpha*open;
        RenderUtil.drawRoundedRect(g,left,top,width,height,height/2,Theme.opacity(0xFF0E1014,a*0.72));
        RenderUtil.drawRoundedBorder(g,left,top,width,height,height/2,Theme.opacity(color,a*0.55),1);
        double textTop=top+(height-9*textScale)/2+0.5;
        RenderUtil.scissorPush(g,left+2,top,width-4,height);
        try{
            RenderUtil.text(g,font,name,left+padding,textTop,textScale,Theme.opacity(0xFFF0F2F3,a));
            if(!dist.isEmpty()){RenderUtil.text(g,font,dist,left+padding+nameWidth+gap,textTop,textScale,Theme.opacity(color,a));}
        }finally{RenderUtil.scissorPop(g);}
    }

    private void edgeArrow(GuiGraphicsExtractor g,dev.riserecode.map.WaypointStore.Waypoint p,double x,double y,double angle,double meters,double alpha){
        double s=scale.getValue();int color=p.color()|0xFF000000;
        // chevron pointing toward the waypoint
        g.pose().pushMatrix();g.pose().translate((float)x,(float)y);g.pose().rotate((float)angle);
        double arm=6*s;
        RenderUtil.line(g,-arm*0.6,-arm,arm*0.6,0,3.2*s,Theme.opacity(0xFF0B0D10,alpha*0.7));
        RenderUtil.line(g,-arm*0.6,arm,arm*0.6,0,3.2*s,Theme.opacity(0xFF0B0D10,alpha*0.7));
        RenderUtil.line(g,-arm*0.6,-arm,arm*0.6,0,1.8*s,Theme.opacity(color,alpha));
        RenderUtil.line(g,-arm*0.6,arm,arm*0.6,0,1.8*s,Theme.opacity(color,alpha));
        g.pose().popMatrix();
    }

    private static String formatDistance(double meters){
        return meters>=1000?String.format(java.util.Locale.ROOT,"%.1fkm",meters/1000):Math.round(meters)+"m";
    }
}
