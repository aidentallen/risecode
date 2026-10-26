package dev.riserecode.map;

import dev.riserecode.ui.component.ActionButton;
import dev.riserecode.util.RenderUtil;
import dev.riserecode.ui.theme.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Fullscreen terrain or a movable, resizable panel over the world. */
public final class WorldMapScreen extends Screen {
    private final Screen parent;
    private final MapService maps;
    private final List<ActionButton> buttons;
    private String dimension;
    private double centerX,centerZ,zoom=3,targetZoom=3,targetX,targetZ;
    private long last;
    private final long opened=System.nanoTime();
    private double displayScale=1;
    private double localX(double mx){return x+w/2+(mx-x-w/2)/displayScale;}
    private double localY(double my){return y+h/2+(my-y-h/2)/displayScale;}
    private boolean anchored,panel,dragging,resizing;
    private double anchorX,anchorZ,anchorPixelX,anchorPixelZ;
    private double nx=.12,ny=.12,nw=.76,nh=.76,x,y,w,h;
    private int manualAltitude=Integer.MIN_VALUE;
    public WorldMapScreen(Screen parent,MapService maps){
        super(Component.literal("World map"));this.parent=parent;this.maps=maps;dimension=maps.dimension();recenter();
        var saved=maps.preferences();
        try{if(saved.has("manualAltitude")){manualAltitude=saved.get("manualAltitude").getAsInt();}panel=saved.has("panel")&&saved.get("panel").getAsBoolean();if(saved.has("panelX")){nx=saved.get("panelX").getAsDouble();ny=saved.get("panelY").getAsDouble();nw=saved.get("panelW").getAsDouble();nh=saved.get("panelH").getAsDouble();}if(!Double.isFinite(nx+ny+nw+nh)){nx=ny=.12;nw=nh=.76;}}catch(RuntimeException ignored){nx=ny=.12;nw=nh=.76;}
        buttons=List.of(new ActionButton("Recenter",this::recenter),
                new ActionButton(()->dimension.replace("minecraft:",""),()->{var dims=maps.dimensions();dimension=dims.get((dims.indexOf(dimension)+1)%dims.size());}),
                new ActionButton(()->"Layer: "+maps.mode().name(),maps::cycleMode),
                new ActionButton(()->manualAltitude==Integer.MIN_VALUE?"Y: Auto "+maps.altitude():"Y: "+manualAltitude,()->manualAltitude=manualAltitude==Integer.MIN_VALUE?maps.altitude():manualAltitude>=320?Integer.MIN_VALUE:manualAltitude+8),
                new ActionButton("Waypoints",()->minecraft.gui.setScreen(new WaypointScreen(this,maps))),
                new ActionButton("Add",()->add(centerX,centerZ)),
                new ActionButton(()->panel?"Fullscreen":"Panel",()->{panel=!panel;anchored=false;save();}),new ActionButton("Done",this::onClose));
    }
    private void bounds(){
        if(panel){w=Math.clamp(nw*width,Math.min(280,width-8),width-8);h=Math.clamp(nh*height,Math.min(170,height-8),height-8);x=Math.clamp(nx*width,4,width-w-4);y=Math.clamp(ny*height,4,height-h-4);}
        else{x=y=0;w=width;h=height;}
    }
    private void save(){var json=maps.preferences();json.addProperty("manualAltitude",manualAltitude);json.addProperty("panel",panel);json.addProperty("panelX",nx);json.addProperty("panelY",ny);json.addProperty("panelW",nw);json.addProperty("panelH",nh);maps.preferences(json);}
    private void recenter(){anchored=false;var p=maps.minecraft().player;if(p!=null){targetX=centerX=p.getX();targetZ=centerZ=p.getZ();dimension=maps.dimension();}}
    private void add(double wx,double wz){if(!maps.ready()){return;}int py=maps.minecraft().player==null?64:maps.minecraft().player.blockPosition().getY();var p=maps.waypoints().add("Waypoint "+(maps.waypoints().all().size()+1),(int)Math.floor(wx),py,(int)Math.floor(wz),dimension);maps.saveWaypoints();minecraft.gui.setScreen(new WaypointEditScreen(this,maps,p));}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){ }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        bounds();double alpha=dev.riserecode.ui.animation.PopIn.progress((System.nanoTime()-opened)*1e-9,dev.riserecode.ui.animation.PopIn.windowDuration("Map"));displayScale=dev.riserecode.ui.animation.PopIn.windowScale("Map",alpha);
        g.pose().pushMatrix();g.pose().translate((float)(x+w/2),(float)(y+h/2));g.pose().scale((float)displayScale,(float)displayScale);g.pose().translate((float)(-x-w/2),(float)(-y-h/2));
        try{draw(g,(int)localX(mx),(int)localY(my),alpha);}finally{g.pose().popMatrix();}
    }
    private void draw(GuiGraphicsExtractor g,int mx,int my,double alpha){
        long now=System.nanoTime();double dt=last==0?.016:Math.min(.1,(now-last)*1e-9);last=now;double blend=Theme.bool("Reduced Motion",false)?1:1-Math.exp(-dt*18);
        targetZoom=Math.clamp(targetZoom,.03125,24);zoom+=(targetZoom-zoom)*blend;
        if(anchored){centerX=anchorX-anchorPixelX/zoom;centerZ=anchorZ-anchorPixelZ/zoom;targetX=anchorX-anchorPixelX/targetZoom;targetZ=anchorZ-anchorPixelZ/targetZoom;}else{centerX+=(targetX-centerX)*blend;centerZ+=(targetZ-centerZ)*blend;}
        if(panel){RenderUtil.drawDropShadow(g,x,y,w,h,8,0xC0000000);}
        MapCanvas.draw(g,maps,dimension,centerX,centerZ,x,y,w,h,zoom,0,true,true,alpha,maps.mode(),manualAltitude,false,false);
        int columns=Math.max(2,Math.min(8,(int)((w-12)/78)));double bw=(w-12-(columns-1)*3)/columns,top=y+(panel?23:7);
        if(panel){RenderUtil.drawRoundedRect(g,x,y,w,19,4,Theme.SURFACE);RenderUtil.text(g,font,"World map · drag to move",x+8,y+6,.75,Theme.TEXT);}
        for(int i=0;i<buttons.size();i++){var b=buttons.get(i);b.setBounds(x+6+i%columns*(bw+3),top+i/columns*24,bw,21);b.render(g,font,mx,my,dt,alpha);}
        double wx=centerX+(mx-x-w/2)/zoom,wz=centerZ+(my-y-h/2)/zoom;
        String status=maps.connected()?String.format(java.util.Locale.ROOT,"X %.0f  Z %.0f · %s Y %d · %.1f× · Shift+wheel: altitude",wx,wz,maps.layer(maps.mode()).name(),manualAltitude==Integer.MIN_VALUE?maps.altitude():manualAltitude,zoom):"Connect to a world to view explored terrain";
        RenderUtil.drawRoundedRect(g,x+5,y+h-20,w-10,16,4,Theme.opacity(Theme.SURFACE,.9));RenderUtil.text(g,font,RenderUtil.ellipsize(font,status,(w-20)/.65),x+10,y+h-15,.65,Theme.TEXT);
        if(panel){RenderUtil.line(g,x+w-10,y+h-4,x+w-4,y+h-10,1.4,Theme.TEXT);}
    }
    private boolean inside(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        bounds();e=new MouseButtonEvent(localX(e.x()),localY(e.y()),e.buttonInfo());if(!inside(e.x(),e.y())){return true;}
        if(e.button()==0&&panel&&e.x()>x+w-14&&e.y()>y+h-14){resizing=true;return true;}
        if(e.button()==0&&panel&&e.y()<y+20){dragging=true;return true;}
        for(var b:buttons){if(b.mouseClicked(e.x(),e.y(),e.button())){return true;}}
        if(e.button()==1){add(centerX+(e.x()-x-w/2)/zoom,centerZ+(e.y()-y-h/2)/zoom);}return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        e=new MouseButtonEvent(localX(e.x()),localY(e.y()),e.buttonInfo());dx/=displayScale;dy/=displayScale;
        if(e.button()!=0){return false;}
        if(resizing){nw=Math.clamp((w+dx)/width,Math.min(280.0/width,1),1);nh=Math.clamp((h+dy)/height,Math.min(170.0/height,1),1);anchored=false;return true;}
        if(dragging){nx=Math.clamp((x+dx)/width,0,1);ny=Math.clamp((y+dy)/height,0,1);return true;}
        if(inside(e.x(),e.y())){anchored=false;targetX-=dx/zoom;targetZ-=dy/zoom;}return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging||resizing){dragging=resizing=false;save();}return true;}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        mx=localX(mx);my=localY(my);if(!inside(mx,my)){return false;}if((com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow(),340)||com.mojang.blaze3d.platform.InputConstants.isKeyDown(minecraft.getWindow(),344))){manualAltitude=Math.clamp((manualAltitude==Integer.MIN_VALUE?maps.altitude():manualAltitude)+(vertical>0?8:-8),-64,320);return true;}
        double next=Math.clamp(targetZoom*Math.pow(1.2,vertical),.03125,24);double px=mx-x-w/2,pz=my-y-h/2;
        anchorX=centerX+px/zoom;anchorZ=centerZ+pz/zoom;anchored=true;anchorPixelX=px;anchorPixelZ=pz;targetZoom=next;return true;
    }
    @Override public boolean keyPressed(KeyEvent e){if(e.isEscape()){onClose();return true;}return super.keyPressed(e);}
    @Override public void onClose(){save();minecraft.gui.setScreen(parent);}
    @Override public void removed(){save();}
    @Override public boolean isPauseScreen(){return false;}
}
