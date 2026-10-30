package dev.riserecode.module.modules.hud;
import dev.riserecode.event.EventBus;
import dev.riserecode.map.MapMarkers;
import dev.riserecode.setting.*;
import dev.riserecode.ui.hud.HudContent;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
public final class MinimapModule extends HudModule {
    public enum Shape { SQUARE,CIRCLE }
    private static final int LINE_HEIGHT=10,MAX_HEADS=24,MAX_ENTITIES=150,MAX_WAYPOINTS=32;
    private static final double ENLARGE=1.9;
    private static final String[] HEADINGS={"S","SW","W","NW","N","NE","E","SE"};
    public final EnumSetting<Shape> shape=register(new EnumSetting<>("Shape","Minimap outline",Shape.SQUARE));
    public final EnumSetting<dev.riserecode.map.MapLayerPolicy.Mode> layer=register(new EnumSetting<>("Layer","Automatic or manual terrain layer",dev.riserecode.map.MapLayerPolicy.Mode.AUTO));
    public final BooleanSetting autoAltitude=register(new BooleanSetting("Automatic Altitude","Follow player altitude with hysteresis",true));
    public final DoubleSetting altitude=register(DoubleSetting.extended("Cave Altitude","Manual cave altitude",64,-64,320));
    public final DoubleSetting zoom=register(DoubleSetting.extended("Zoom","Map pixels per block (zoom keys change this; low = far out)",1.5,0.05,8));
    public final DoubleSetting size=register(DoubleSetting.extended("Size","Map size in HUD canvas units",130,80,240));
    public final BooleanSetting rotate=register(new BooleanSetting("Rotate","Keep player facing up",true));
    public final BooleanSetting compass=register(new BooleanSetting("Compass","Show N, E, S and W around the map",true));
    public final BooleanSetting border=register(new BooleanSetting("Map Frame","Draw a frame around the map",false));
    public final EnumSetting<MapMarkers.PlayerMarker> marker=register(new EnumSetting<>("Your Marker","Arrow or your head in the center",MapMarkers.PlayerMarker.ARROW));
    public final BooleanSetting chunkGrid=register(new BooleanSetting("Chunk Grid","Draw chunk borders over the map",false));
    public final BooleanSetting slimeChunks=register(new BooleanSetting("Slime Chunks","Tint slime chunks; needs the seed, so singleplayer only",false));
    public final BooleanSetting waypoints=register(new BooleanSetting("Waypoints","Show destinations",true));
    public final BooleanSetting edgeWaypoints=register(new BooleanSetting("Edge Waypoints","Pin waypoints outside the map to its edge",true));
    public final BooleanSetting radar=register(new BooleanSetting("Entity Radar","Show nearby entities",true));
    public final BooleanSetting radarPlayers=register(new BooleanSetting("Radar Players","Show other players",true));
    public final BooleanSetting radarHostile=register(new BooleanSetting("Radar Hostile","Show hostile mobs",true));
    public final BooleanSetting radarPassive=register(new BooleanSetting("Radar Passive","Show animals and villagers",true));
    public final BooleanSetting radarItems=register(new BooleanSetting("Radar Items","Show dropped items",false));
    public final BooleanSetting headFrames=register(new BooleanSetting("Head Frames","Draw a colored frame around each head",false));
    public final DoubleSetting verticalRange=register(DoubleSetting.extended("Vertical Range","Only show entities within this many blocks above or below you",24,4,128));
    public final BooleanSetting heads=register(new BooleanSetting("Head Icons","Show mob and player heads instead of dots",true));
    public final DoubleSetting headSize=register(DoubleSetting.extended("Head Size","Size of the radar heads",11,6,24));
    public final BooleanSetting radarNames=register(new BooleanSetting("Radar Names","Show player names next to their dots",true));
    public final BooleanSetting heightMarks=register(new BooleanSetting("Height Marks","Mark entities well above or below you",true));
    public final BooleanSetting coordinates=register(new BooleanSetting("Coordinates","Show your position below the map",true));
    public final BooleanSetting direction=register(new BooleanSetting("Direction","Show the direction you are facing",false));
    public final BooleanSetting biome=register(new BooleanSetting("Biome","Show the current biome below the map",true));
    public final BooleanSetting dimension=register(new BooleanSetting("Dimension","Show the current dimension",false));
    public final BooleanSetting light=register(new BooleanSetting("Light Level","Show the block light level at your feet",false));
    public final BooleanSetting weather=register(new BooleanSetting("Weather","Show the current weather",false));
    public final BooleanSetting time=register(new BooleanSetting("Time","Show the in-game time below the map",false));
    // hotkeys; kept in sync with the matching Minecraft key mappings by RiseRecodeClient
    public final KeySetting zoomInKey=register(new KeySetting("Zoom In Key","Zoom the minimap in",90));
    public final KeySetting zoomOutKey=register(new KeySetting("Zoom Out Key","Zoom the minimap out",88));
    public final KeySetting enlargeKey=register(new KeySetting("Enlarge Key","Hold to make the minimap bigger",86));
    private boolean enlarged;
    public MinimapModule(EventBus bus,Minecraft mc){
        super("Minimap","Textured explored surface map with entity radar in the HUD editor.",bus,mc);
        headSize.setVisibleWhen(()->radar.getValue()&&heads.getValue());
        altitude.setVisibleWhen(()->!autoAltitude.getValue());
        edgeWaypoints.setVisibleWhen(waypoints::getValue);
        for(var setting:List.of(headFrames,verticalRange,heads,radarPlayers,radarHostile,radarPassive,radarItems,radarNames,heightMarks)){setting.setVisibleWhen(radar::getValue);}
    }
    /** Zoom in or out by one step, clamped to the setting's range. */
    public void zoomBy(int steps){zoom.setValue(Math.clamp(zoom.getValue()*Math.pow(1.25,steps),zoom.getMin(),zoom.getMax()));}
    public void enlarged(boolean value){enlarged=value;}
    @Override protected Object contentKey(){return 0;}
    @Override protected List<HudContent.Line> buildContent(){return List.of();}
    private double mapSize(){return size.getValue()*(enlarged?ENLARGE:1);}
    @Override public double graphicWidth(){return mapSize();}
    private int infoLines(){int lines=0;for(var line:List.of(coordinates,direction,biome,dimension,light,weather,time)){if(line.getValue()){lines++;}}return lines;}
    @Override public double graphicHeight(){return mapSize()+(infoLines()>0?infoLines()*LINE_HEIGHT+8:0);}
    @Override public void drawGraphic(GuiGraphicsExtractor g,double alpha,boolean edit){
        var maps=dev.riserecode.Client.INSTANCE.getMaps();var player=minecraft.player;
        if(player==null){RenderUtil.text(g,minecraft.font,"Minimap",8,8,1,0xFFFFFFFF);return;}
        double w=mapSize(),h=w,z=zoom.getValue(),rotation=rotate.getValue()?Math.toRadians(180-player.getYRot()):0;
        boolean circle=shape.getValue()==Shape.CIRCLE;
        dev.riserecode.map.MapCanvas.draw(g,maps,maps.dimension(),player.getX(),player.getZ(),0,0,w,h,z,rotation,false,false,alpha,layer.getValue(),autoAltitude.getValue()?Integer.MIN_VALUE:altitude.getValue().intValue(),circle,true,true,false);
        if(slimeChunks.getValue()){slimeChunks(g,player,w,h,z,rotation,circle,alpha);}
        if(chunkGrid.getValue()){chunkGrid(g,player,w,h,z,rotation,circle,alpha);}
        if(waypoints.getValue()){waypoints(g,player,w,h,z,rotation,circle,alpha);}
        if(radar.getValue()){radar(g,player,w,h,z,rotation,circle,alpha);}
        ownMarker(g,player,w,h,rotation,alpha);
        if(border.getValue()&&!circle){frame(g,w,h,alpha);}
        if(compass.getValue()){compass(g,w,h,rotation,circle,alpha);}
        info(g,player,w,h,alpha);
    }

    /** Screen offset from the map centre for a world offset, matching the canvas rotation. */
    private static double[] project(double dx,double dz,double zoom,double rotation){
        double px=dx*zoom,pz=dz*zoom,cos=Math.cos(rotation),sin=Math.sin(rotation);
        return new double[]{px*cos-pz*sin,px*sin+pz*cos};
    }
    private static boolean inside(double x,double y,double w,double h,boolean circle,double margin){
        return circle?Math.hypot(x,y)<Math.min(w,h)/2-margin:Math.abs(x)<w/2-margin&&Math.abs(y)<h/2-margin;
    }
    /** Shortens a line to the circle around the centre; null when it stays outside. */
    private static double[] clip(double[] a,double[] b,double radius){
        double dx=b[0]-a[0],dy=b[1]-a[1],qa=dx*dx+dy*dy,qb=a[0]*dx+a[1]*dy,qc=a[0]*a[0]+a[1]*a[1]-radius*radius,root=qb*qb-qa*qc;
        if(qa==0||root<=0){return null;}
        root=Math.sqrt(root);double from=Math.max(0,(-qb-root)/qa),to=Math.min(1,(-qb+root)/qa);
        return from>=to?null:new double[]{a[0]+dx*from,a[1]+dy*from,a[0]+dx*to,a[1]+dy*to};
    }
    private static void gridLine(GuiGraphicsExtractor g,double[] a,double[] b,double radius,boolean circle,int color){
        if(!circle){RenderUtil.line(g,a[0],a[1],b[0],b[1],0.5,color);return;}
        var cut=clip(a,b,radius);
        if(cut!=null){RenderUtil.line(g,cut[0],cut[1],cut[2],cut[3],0.5,color);}
    }

    private void chunkGrid(GuiGraphicsExtractor g,Player player,double w,double h,double zoom,double rotation,boolean circle,double alpha){
        int color=Theme.opacity(0xFFFFFFFF,alpha*0.18);double reach=Math.hypot(w,h)/2/zoom,radius=Math.min(w,h)/2;
        int minX=(int)Math.floor((player.getX()-reach)/16),maxX=(int)Math.floor((player.getX()+reach)/16)+1,minZ=(int)Math.floor((player.getZ()-reach)/16),maxZ=(int)Math.floor((player.getZ()+reach)/16)+1;
        if((long)(maxX-minX)*(maxZ-minZ)>1600){return;}
        RenderUtil.scissorPush(g,0,0,w,h);
        try{g.pose().pushMatrix();g.pose().translate((float)(w/2),(float)(h/2));
            for(int cx=minX;cx<=maxX;cx++){gridLine(g,project(cx*16-player.getX(),minZ*16-player.getZ(),zoom,rotation),project(cx*16-player.getX(),maxZ*16-player.getZ(),zoom,rotation),radius,circle,color);}
            for(int cz=minZ;cz<=maxZ;cz++){gridLine(g,project(minX*16-player.getX(),cz*16-player.getZ(),zoom,rotation),project(maxX*16-player.getX(),cz*16-player.getZ(),zoom,rotation),radius,circle,color);}
            g.pose().popMatrix();
        }finally{RenderUtil.scissorPop(g);}
    }

    private void slimeChunks(GuiGraphicsExtractor g,Player player,double w,double h,double zoom,double rotation,boolean circle,double alpha){
        var server=minecraft.getSingleplayerServer();
        if(server==null||minecraft.level==null||minecraft.level.dimension()!=Level.OVERWORLD||zoom<.4){return;}
        long seed=server.overworld().getSeed();int color=Theme.opacity(0xFF4ADE80,alpha*.32);double reach=Math.hypot(w,h)/2/zoom,span=16*zoom;
        int minX=(int)Math.floor((player.getX()-reach)/16),maxX=(int)Math.floor((player.getX()+reach)/16),minZ=(int)Math.floor((player.getZ()-reach)/16),maxZ=(int)Math.floor((player.getZ()+reach)/16);
        RenderUtil.scissorPush(g,0,0,w,h);
        try{g.pose().pushMatrix();g.pose().translate((float)(w/2),(float)(h/2));g.pose().rotate((float)rotation);
            for(int cz=minZ;cz<=maxZ;cz++){for(int cx=minX;cx<=maxX;cx++){
                if(WorldgenRandom.seedSlimeChunk(cx,cz,seed,987234911L).nextInt(10)!=0){continue;}
                double px=(cx*16-player.getX())*zoom,pz=(cz*16-player.getZ())*zoom;
                if(circle&&Math.hypot(px+span/2,pz+span/2)>Math.min(w,h)/2-span/2){continue;}
                RenderUtil.drawRoundedRect(g,px,pz,span,span,0,color);
            }}
            g.pose().popMatrix();
        }finally{RenderUtil.scissorPop(g);}
    }

    private void waypoints(GuiGraphicsExtractor g,Player player,double w,double h,double zoom,double rotation,boolean circle,double alpha){
        var maps=dev.riserecode.Client.INSTANCE.getMaps();
        var points=new ArrayList<>(maps.waypoints().displayed().stream().filter(p->p.visible()&&p.dimension().equals(maps.dimension())).toList());
        points.sort(Comparator.comparingDouble(p->Math.pow(p.x()-player.getX(),2)+Math.pow(p.z()-player.getZ(),2)));
        double half=Math.min(w,h)/2-7;
        for(var point:points.subList(0,Math.min(MAX_WAYPOINTS,points.size()))){
            var p=project(point.x()+.5-player.getX(),point.z()+.5-player.getZ(),zoom,rotation);
            boolean outside=!inside(p[0],p[1],w,h,circle,7);
            if(outside&&!edgeWaypoints.getValue()){continue;}
            double factor=outside?half/(circle?Math.hypot(p[0],p[1]):Math.max(Math.abs(p[0]),Math.abs(p[1]))):1;
            MapMarkers.waypoint(g,minecraft.font,point,w/2+p[0]*factor,h/2+p[1]*factor,9,alpha*(outside?.75:1));
        }
    }

    private boolean wanted(Entity entity){
        if(entity instanceof Player){return radarPlayers.getValue();}
        if(entity instanceof ItemEntity){return radarItems.getValue();}
        if(!(entity instanceof LivingEntity)||entity.isInvisible()){return false;}
        return entity.getType().getCategory()==MobCategory.MONSTER?radarHostile.getValue():radarPassive.getValue();
    }

    private void radar(GuiGraphicsExtractor g,Player player,double w,double h,double zoom,double rotation,boolean circle,double alpha){
        if(minecraft.level==null){return;}
        g.pose().pushMatrix();g.pose().translate((float)(w/2),(float)(h/2));
        try{int drawn=0,headsDrawn=0;
            for(Entity entity:minecraft.level.entitiesForRendering()){
                if(entity==player||entity.isRemoved()||!wanted(entity)||Math.abs(entity.getY()-player.getY())>verticalRange.getValue()){continue;}
                var p=project(entity.getX()-player.getX(),entity.getZ()-player.getZ(),zoom,rotation);
                if(!inside(p[0],p[1],w,h,circle,3)){continue;}
                if(++drawn>MAX_ENTITIES){break;}
                int color=MapMarkers.entityColor(entity);boolean human=entity instanceof Player,item=entity instanceof ItemEntity;double r=human?2.5:item?1.2:2;
                if(heads.getValue()&&!item&&headsDrawn<MAX_HEADS&&MapMarkers.head(g,entity,p[0],p[1],headSize.getValue(),headFrames.getValue(),alpha)){headsDrawn++;r=headSize.getValue()/2;}
                else{
                    RenderUtil.drawRoundedRect(g,p[0]-r-1,p[1]-r-1,r*2+2,r*2+2,r+1,Theme.opacity(0xFF000000,alpha*0.7));
                    RenderUtil.drawRoundedRect(g,p[0]-r,p[1]-r,r*2,r*2,r,Theme.opacity(color,alpha));
                }
                double dy=entity.getY()-player.getY();boolean far=Math.abs(dy)>=4;
                if(heightMarks.getValue()&&!item&&far){RenderUtil.text(g,minecraft.font,dy>0?"^":"v",p[0]+r+1.5,p[1]-3,0.55,Theme.opacity(color,alpha));}
                if(human&&radarNames.getValue()){RenderUtil.text(g,minecraft.font,((Player)entity).getGameProfile().name(),p[0]+r+2,p[1]-3+(far?5:0),0.5,Theme.opacity(0xFFFFFFFF,alpha*0.9));}
            }
        }finally{g.pose().popMatrix();}
    }

    private void ownMarker(GuiGraphicsExtractor g,Player player,double w,double h,double rotation,double alpha){
        if(marker.getValue()==MapMarkers.PlayerMarker.HEAD&&MapMarkers.head(g,player,w/2,h/2,headSize.getValue(),true,alpha)){return;}
        MapMarkers.arrow(g,w/2,h/2,Math.toRadians(player.getYRot()+180)+rotation,5.5,Theme.accent(),alpha);
    }

    private void frame(GuiGraphicsExtractor g,double w,double h,double alpha){
        int color=Theme.opacity(Theme.accent(),alpha*0.9);
        RenderUtil.line(g,0,0.5,w,0.5,1.5,color);RenderUtil.line(g,0,h-0.5,w,h-0.5,1.5,color);
        RenderUtil.line(g,0.5,0,0.5,h,1.5,color);RenderUtil.line(g,w-0.5,0,w-0.5,h,1.5,color);
    }

    private void compass(GuiGraphicsExtractor g,double w,double h,double rotation,boolean circle,double alpha){
        String[] names={"N","E","S","W"};double[][] directions={{0,-1},{1,0},{0,1},{-1,0}};
        for(int i=0;i<4;i++){
            var v=project(directions[i][0],directions[i][1],1,rotation);double longest=Math.max(Math.abs(v[0]),Math.abs(v[1]));
            double reach=Math.min(w,h)/2-7,factor=circle?1:1/Math.max(longest,1e-6);
            double x=w/2+v[0]*reach*factor,y=h/2+v[1]*reach*factor;
            double textWidth=RenderUtil.textWidth(minecraft.font,names[i],false)*0.7;
            RenderUtil.text(g,minecraft.font,names[i],x-textWidth/2,y-3,0.7,Theme.opacity(i==0?0xFFFF5D5D:0xFFFFFFFF,alpha));
        }
    }

    private List<String> lines(Player player){
        var out=new ArrayList<String>();var level=minecraft.level;
        if(coordinates.getValue()){out.add(player.getBlockX()+", "+player.getBlockY()+", "+player.getBlockZ());}
        if(direction.getValue()){
            int yaw=Math.floorMod(Math.round(player.getYRot()),360);
            out.add(HEADINGS[Math.floorMod(Math.round(yaw/45f),8)]+" "+yaw+"°");
        }
        if(level!=null){
            if(biome.getValue()){var name=MapMarkers.biomeAt(minecraft,player.getBlockX(),player.getBlockY(),player.getBlockZ());out.add(name==null?"Unknown":name);}
            if(dimension.getValue()){out.add(dev.riserecode.map.WaypointManagerScreen.dimensionName(level.dimension().identifier().toString()));}
            if(light.getValue()){out.add("Light "+level.getMaxLocalRawBrightness(player.blockPosition()));}
            if(weather.getValue()){out.add(level.isThundering()?"Thunder":level.isRaining()?"Rain":"Clear");}
            if(time.getValue()){long day=(level.getOverworldClockTime()+6000)%24000;out.add(String.format(java.util.Locale.ROOT,"%02d:%02d",day/1000,day%1000*60/1000));}
        }
        return out;
    }

    private void info(GuiGraphicsExtractor g,Player player,double w,double h,double alpha){
        var lines=lines(player);
        if(lines.isEmpty()){return;}
        double top=h+3,scale=.8;
        RenderUtil.drawRoundedRect(g,0,top,w,lines.size()*LINE_HEIGHT+4,4,Theme.opacity(Theme.SURFACE,alpha*.72));
        for(int i=0;i<lines.size();i++){
            String text=lines.get(i);double width=RenderUtil.textWidth(minecraft.font,text,false)*scale;
            RenderUtil.text(g,minecraft.font,text,(w-width)/2,top+3+i*LINE_HEIGHT,scale,Theme.opacity(i==0&&coordinates.getValue()?Theme.TEXT:Theme.SECONDARY,alpha));
        }
    }
}
