package dev.riserecode.map;

import dev.riserecode.Client;
import dev.riserecode.RiseRecodeClient;
import dev.riserecode.module.modules.WorldMapModule;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Xaero-style world map: drag and wheel to navigate, right click for actions, a layer strip to look at one height
 * band, and a waypoint side list. Everything it can show or hide is on the WorldMap module page.
 */
public final class WorldMapScreen extends Screen {
    private static final double MIN_ZOOM=.03125,MAX_ZOOM=24;
    private static final int TOP_BAND=304,BANDS=(TOP_BAND+64)/MapLayerPolicy.BAND+1,MENU_ROW=18,MENU_WIDTH=164,SIDE_WIDTH=206,SIDE_ROW=26,MAX_ENTITIES=48;
    private record Item(String label,Runnable action){}
    private record Hit(double x,double y,double width,double height,Runnable left,Runnable right){
        boolean contains(double px,double py){return px>=x&&px<x+width&&py>=y&&py<y+height;}
    }
    private final Screen parent;
    private final MapService maps;
    private final WorldMapModule module;
    private final List<Hit> hits=new ArrayList<>();
    private String dimension;
    private double centerX,centerZ,zoom,targetZoom,targetX,targetZ;
    private long last;
    private final long opened=System.nanoTime();
    private double displayScale=1;
    private boolean anchored,panel,dragging,resizing,stripDrag,following,sideOpen,moved;
    private double anchorX,anchorZ,anchorPixelX,anchorPixelZ;
    private double nx=.12,ny=.12,nw=.76,nh=.76,x,y,w,h;
    private MapLayerPolicy.Mode viewMode;
    private int caveY=Integer.MIN_VALUE;
    private List<Item> menu=List.of();
    private double menuX,menuY,sideScroll;
    private WaypointStore.Waypoint hovered;
    private EditBox gotoBox,searchBox;

    public WorldMapScreen(Screen parent,MapService maps){
        super(Component.literal("World map"));this.parent=parent;this.maps=maps;dimension=maps.dimension();
        module=Client.INSTANCE.getModuleManager().getByClass(WorldMapModule.class).orElse(null);
        viewMode=maps.mode();panel=module!=null&&module.window.getValue()==WorldMapModule.Window.PANEL;
        zoom=targetZoom=module==null?3:module.defaultZoom.getValue();
        following=module==null||module.follow.getValue();
        var saved=maps.preferences();
        try{
            if(saved.has("manualAltitude")){caveY=saved.get("manualAltitude").getAsInt();}
            if(saved.has("viewMode")){viewMode=MapLayerPolicy.Mode.valueOf(saved.get("viewMode").getAsString());}
            if(saved.has("sideOpen")){sideOpen=saved.get("sideOpen").getAsBoolean();}
            if(saved.has("mapZoom")&&(module==null||module.rememberZoom.getValue())){zoom=targetZoom=Math.clamp(saved.get("mapZoom").getAsDouble(),MIN_ZOOM,MAX_ZOOM);}
            if(saved.has("panelX")){nx=saved.get("panelX").getAsDouble();ny=saved.get("panelY").getAsDouble();nw=saved.get("panelW").getAsDouble();nh=saved.get("panelH").getAsDouble();}
        }catch(RuntimeException ignored){viewMode=MapLayerPolicy.Mode.AUTO;}
        snapToPlayer();
    }
    @Override protected void init(){
        gotoBox=addRenderableWidget(new EditBox(font,width/2-70,40,140,16,Component.literal("Go to")));
        gotoBox.setHint(Component.literal("Go to x z"));gotoBox.setVisible(false);gotoBox.setMaxLength(40);gotoBox.setBordered(false);
        searchBox=addRenderableWidget(new EditBox(font,0,0,SIDE_WIDTH-24,14,Component.literal("Search")));
        searchBox.setHint(Component.literal("Search waypoints"));searchBox.setVisible(false);searchBox.setMaxLength(40);searchBox.setBordered(false);
    }

    private boolean grid(){return module!=null&&module.chunkGrid.getValue();}
    private void toggleGrid(){if(module!=null){module.chunkGrid.setValue(!module.chunkGrid.getValue());}}
    private boolean typing(){return gotoBox!=null&&gotoBox.isFocused()||searchBox!=null&&searchBox.isFocused();}
    private void bounds(){
        if(panel){w=Math.clamp(nw*width,Math.min(280,width-8),width-8);h=Math.clamp(nh*height,Math.min(170,height-8),height-8);x=Math.clamp(nx*width,4,width-w-4);y=Math.clamp(ny*height,4,height-h-4);}
        else{x=y=0;w=width;h=height;}
    }
    private void save(){
        var json=maps.preferences();
        json.addProperty("manualAltitude",caveY);json.addProperty("viewMode",viewMode.name());json.addProperty("sideOpen",sideOpen);json.addProperty("mapZoom",targetZoom);
        json.addProperty("panelX",nx);json.addProperty("panelY",ny);json.addProperty("panelW",nw);json.addProperty("panelH",nh);maps.preferences(json);
    }
    private void snapToPlayer(){var p=maps.minecraft().player;if(p!=null){targetX=centerX=p.getX();targetZ=centerZ=p.getZ();}}
    private void recenter(){anchored=false;dimension=maps.dimension();snapToPlayer();following=true;}
    private void stopFollowing(){following=false;}
    private void goTo(double wx,double wz){anchored=false;following=false;targetX=wx;targetZ=wz;}
    private void add(double wx,double wz){
        if(!maps.ready()){return;}
        var p=maps.waypoints().add("Waypoint "+(maps.waypoints().all().size()+1),(int)Math.floor(wx),surfaceY(wx,wz),(int)Math.floor(wz),dimension);
        maps.saveWaypoints();minecraft.gui.setScreen(new WaypointEditScreen(this,maps,p));
    }
    private int surfaceY(double wx,double wz){
        Integer known=height(wx,wz);if(known!=null){return known+1;}
        return maps.minecraft().player==null?64:maps.minecraft().player.blockPosition().getY();
    }
    private Integer height(double wx,double wz){return maps.knownHeight(maps.key(dimension,(int)Math.floor(wx)>>4,(int)Math.floor(wz)>>4,0,viewMode,caveY),(int)Math.floor(wx),(int)Math.floor(wz));}
    private double sx(double wx){return x+w/2+(wx-centerX)*zoom;}
    private double sz(double wz){return y+h/2+(wz-centerZ)*zoom;}
    private double worldX(double mx){return centerX+(mx-x-w/2)/zoom;}
    private double worldZ(double my){return centerZ+(my-y-h/2)/zoom;}
    private double localX(double mx){return x+w/2+(mx-x-w/2)/displayScale;}
    private double localY(double my){return y+h/2+(my-y-h/2)/displayScale;}
    private boolean inside(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private double stripX(){return x+w-22-(sideOpen?SIDE_WIDTH+6:0);}
    private double stripTop(){return y+60;}
    private double stripBottom(){return y+h-30;}
    private double stripCell(){return (stripBottom()-stripTop())/BANDS;}
    private boolean overStrip(double mx,double my){return mx>=stripX()-2&&mx<stripX()+16&&my>=stripTop()-34&&my<stripBottom();}
    private static int bandIndex(int altitude){return Math.clamp((TOP_BAND-MapLayerPolicy.band(altitude))/MapLayerPolicy.BAND,0,BANDS-1);}
    private int selectedBand(){return caveY==Integer.MIN_VALUE?maps.altitude():MapLayerPolicy.band(caveY);}

    private void setLayer(MapLayerPolicy.Mode mode){viewMode=mode;save();}
    private void pickStrip(double my){
        if(my<stripTop()-17){viewMode=MapLayerPolicy.Mode.AUTO;caveY=Integer.MIN_VALUE;return;}
        if(my<stripTop()){viewMode=MapLayerPolicy.Mode.SURFACE;return;}
        viewMode=MapLayerPolicy.Mode.CAVE;
        caveY=TOP_BAND-MapLayerPolicy.BAND*Math.clamp((int)((my-stripTop())/stripCell()),0,BANDS-1);
    }
    private String layerName(){
        return switch(viewMode){
            case AUTO->"Auto"+(maps.layer(MapLayerPolicy.Mode.AUTO)==MapLayerPolicy.Layer.CAVE?" (Y "+maps.altitude()+")":"");
            case SURFACE->"Surface";
            case CAVE->"Y "+selectedBand()+" to "+(selectedBand()+MapLayerPolicy.BAND-1);
        };
    }

    // ---- context menus ----
    private void open(double mx,double my,List<Item> items){menu=items;menuX=Math.min(mx,x+w-MENU_WIDTH-4);menuY=Math.max(y+4,Math.min(my,y+h-items.size()*MENU_ROW-10));}
    private void teleport(int bx,int by,int bz){
        var player=maps.minecraft().player;
        if(player==null){return;}
        player.connection.sendCommand("tp "+bx+" "+by+" "+bz);minecraft.gui.setScreen(null);
    }
    private List<Item> waypointItems(WaypointStore.Waypoint point){
        var items=new ArrayList<Item>();
        items.add(new Item("Edit waypoint",()->minecraft.gui.setScreen(new WaypointEditScreen(this,maps,point))));
        items.add(new Item(point.tracked()?"Stop tracking":"Track waypoint",()->{maps.waypoints().put(point.tracked(!point.tracked()));maps.saveWaypoints();}));
        if(point.dimension().equals(maps.dimension())){items.add(new Item("Teleport here",()->teleport(point.x(),point.y(),point.z())));}
        items.add(new Item("Copy coordinates",()->minecraft.keyboardHandler.setClipboard(point.x()+" "+point.y()+" "+point.z())));
        items.add(new Item("Hide waypoint",()->{maps.waypoints().put(point.visible(false));maps.saveWaypoints();}));
        items.add(new Item("Delete waypoint",()->{maps.waypoints().remove(point.id());maps.saveWaypoints();}));
        return items;
    }
    private void openContext(double mx,double my){
        var point=waypointAt(mx,my);
        if(point!=null){open(mx,my,waypointItems(point));return;}
        double wx=worldX(mx),wz=worldZ(my);int bx=(int)Math.floor(wx),bz=(int)Math.floor(wz),by=surfaceY(wx,wz);
        var items=new ArrayList<Item>();
        items.add(new Item("Create waypoint here",()->add(wx,wz)));
        if(dimension.equals(maps.dimension())){items.add(new Item("Teleport here",()->teleport(bx,by,bz)));}
        items.add(new Item("Center map here",()->goTo(wx,wz)));
        items.add(new Item("Copy coordinates",()->minecraft.keyboardHandler.setClipboard(bx+" "+by+" "+bz)));
        var player=maps.minecraft().player;
        if(player!=null){items.add(new Item("Share in chat",()->{player.connection.sendChat(bx+" "+by+" "+bz);minecraft.gui.setScreen(null);}));}
        items.add(new Item(grid()?"Hide chunk grid":"Show chunk grid",this::toggleGrid));
        open(mx,my,items);
    }
    private void drawMenu(GuiGraphicsExtractor g,int mx,int my){
        if(menu.isEmpty()){return;}
        double height=menu.size()*MENU_ROW+6;
        RenderUtil.drawDropShadow(g,menuX,menuY,MENU_WIDTH,height,6,0xA0000000);
        RenderUtil.drawRoundedRect(g,menuX,menuY,MENU_WIDTH,height,6,Theme.SURFACE);
        for(int i=0;i<menu.size();i++){
            double row=menuY+3+i*MENU_ROW;
            if(mx>=menuX+3&&mx<menuX+MENU_WIDTH-3&&my>=row&&my<row+MENU_ROW){RenderUtil.drawRoundedRect(g,menuX+3,row,MENU_WIDTH-6,MENU_ROW,4,Theme.opacity(Theme.accent(),.45));}
            RenderUtil.text(g,font,menu.get(i).label(),menuX+10,row+5,.8,Theme.TEXT);
        }
    }
    private boolean clickMenu(double mx,double my){
        if(menu.isEmpty()){return false;}
        var items=menu;menu=List.of();
        for(int i=0;i<items.size();i++){
            double row=menuY+3+i*MENU_ROW;
            if(mx>=menuX+3&&mx<menuX+MENU_WIDTH-3&&my>=row&&my<row+MENU_ROW){items.get(i).action().run();break;}
        }
        return true;
    }

    // ---- world overlay ----
    private WaypointStore.Waypoint waypointAt(double mx,double my){
        if(module!=null&&!module.waypoints.getValue()){return null;}
        WaypointStore.Waypoint best=null;double distance=9;
        for(var p:maps.waypoints().displayed()){
            if(!p.visible()||!p.dimension().equals(dimension)){continue;}
            double d=Math.hypot(mx-sx(p.x()+.5),my-sz(p.z()+.5));
            if(d<distance){distance=d;best=p;}
        }
        return best;
    }
    private void grid(GuiGraphicsExtractor g){
        double left=worldX(x),right=worldX(x+w),top=worldZ(y),bottom=worldZ(y+h);
        if(zoom>=1){
            for(int c=(int)Math.floor(left/16);c<=Math.ceil(right/16);c++){double px=sx(c*16.0);RenderUtil.line(g,px,y,px,y+h,.5,0x24FFFFFF);}
            for(int c=(int)Math.floor(top/16);c<=Math.ceil(bottom/16);c++){double pz=sz(c*16.0);RenderUtil.line(g,x,pz,x+w,pz,.5,0x24FFFFFF);}
        }
        if(zoom>=.06){
            for(int c=(int)Math.floor(left/512);c<=Math.ceil(right/512);c++){double px=sx(c*512.0);RenderUtil.line(g,px,y,px,y+h,1,0x55FFFFFF);}
            for(int c=(int)Math.floor(top/512);c<=Math.ceil(bottom/512);c++){double pz=sz(c*512.0);RenderUtil.line(g,x,pz,x+w,pz,1,0x55FFFFFF);}
        }
    }
    private boolean onScreen(double px,double pz,double margin){return px>=x-margin&&px<=x+w+margin&&pz>=y-margin&&pz<=y+h+margin;}
    private void overlay(GuiGraphicsExtractor g,int mx,int my,double alpha){
        RenderUtil.scissorPush(g,x,y,w,h);
        try{
            if(grid()){grid(g);}
            hovered=menu.isEmpty()?waypointAt(mx,my):null;
            var self=maps.minecraft().player;
            if(module==null||module.waypoints.getValue()){
                var names=module==null?WorldMapModule.Names.HOVER:module.waypointNames.getValue();
                for(var p:maps.waypoints().displayed()){
                    if(!p.visible()||!p.dimension().equals(dimension)){continue;}
                    double px=sx(p.x()+.5),pz=sz(p.z()+.5);
                    if(!onScreen(px,pz,30)){continue;}
                    MapMarkers.waypoint(g,font,p,px,pz,10,alpha);
                    if(names==WorldMapModule.Names.ALWAYS&&!Objects.equals(p,hovered)){MapMarkers.tag(g,font,p.name(),px+9,pz-6,alpha*.9);}
                }
                if(hovered!=null&&names!=WorldMapModule.Names.NEVER){
                    String text=hovered.name();
                    if(self!=null&&hovered.dimension().equals(maps.dimension())){text+=" · "+Math.round(self.position().distanceTo(new Vec3(hovered.x()+.5,hovered.y()+.5,hovered.z()+.5)))+"m";}
                    MapMarkers.tag(g,font,text,sx(hovered.x()+.5)+9,sz(hovered.z()+.5)-6,alpha);
                }
            }
            var level=maps.minecraft().level;
            if(level==null||!dimension.equals(maps.dimension())){return;}
            if(module!=null&&module.entities.getValue()){
                int drawn=0;
                for(var entity:level.entitiesForRendering()){
                    if(!(entity instanceof LivingEntity)||entity instanceof Player||entity.isInvisible()){continue;}
                    double px=sx(entity.getX()),pz=sz(entity.getZ());
                    if(!onScreen(px,pz,8)){continue;}
                    if(++drawn>MAX_ENTITIES){break;}
                    if(zoom>=.75){MapMarkers.head(g,entity,px,pz,9,false,alpha);}
                    else{RenderUtil.drawRoundedRect(g,px-2,pz-2,4,4,2,Theme.opacity(MapMarkers.entityColor(entity),alpha));}
                }
            }
            for(var other:level.players()){
                if(other==self||module!=null&&!module.players.getValue()){continue;}
                double px=sx(other.getX()),pz=sz(other.getZ());
                if(!onScreen(px,pz,20)){continue;}
                MapMarkers.head(g,other,px,pz,10,true,alpha);
                if(module==null||module.playerNames.getValue()){String name=other.getGameProfile().name();MapMarkers.tag(g,font,name,px-RenderUtil.textWidth(font,name,false)*.35-4,pz+7,alpha);}
            }
            if(self!=null&&onScreen(sx(self.getX()),sz(self.getZ()),20)){
                if(module!=null&&module.marker.getValue()==MapMarkers.PlayerMarker.HEAD){MapMarkers.head(g,self,sx(self.getX()),sz(self.getZ()),12,true,alpha);}
                else{MapMarkers.arrow(g,sx(self.getX()),sz(self.getZ()),Math.toRadians(self.getYRot()+180),6.5,Theme.accent(),alpha);}
            }
        }finally{RenderUtil.scissorPop(g);}
    }

    // ---- interface ----
    private void pill(GuiGraphicsExtractor g,double px,double py,double pw,double ph,double alpha){RenderUtil.drawRoundedRect(g,px,py,pw,ph,6,Theme.opacity(Theme.SURFACE,alpha*.86));}
    private void button(GuiGraphicsExtractor g,double bx,double by,double bw,double bh,String label,String hint,boolean active,int mx,int my,double alpha,Runnable action){
        boolean hover=mx>=bx&&mx<bx+bw&&my>=by&&my<by+bh;
        RenderUtil.drawRoundedRect(g,bx,by,bw,bh,5,active?Theme.opacity(Theme.accent(),alpha*(hover?.75:.55)):Theme.opacity(hover?Theme.HOVER:Theme.SURFACE,alpha*.86));
        RenderUtil.text(g,font,label,bx+8,by+(bh-7)/2,.8,Theme.opacity(Theme.TEXT,alpha));
        if(hint!=null){RenderUtil.text(g,font,hint,bx+bw-7-RenderUtil.textWidth(font,hint,false)*.7,by+(bh-6)/2,.7,Theme.opacity(active?Theme.TEXT:Theme.SECONDARY,alpha*(active?.75:1)));}
        hits.add(new Hit(bx,by,bw,bh,action,null));
    }
    private void toolbar(GuiGraphicsExtractor g,int mx,int my,double alpha){
        if(module!=null&&!module.toolbar.getValue()){return;}
        record Tool(String label,String hint,boolean active,Runnable action){}
        var tools=List.of(
            new Tool("Center","H",following,this::recenter),
            new Tool("Waypoints","L",sideOpen,this::toggleSide),
            new Tool("Go to","J",gotoBox.isVisible(),this::toggleGoTo),
            new Tool("Chunk grid","G",grid(),this::toggleGrid),
            new Tool("Settings",null,false,this::openSettings),
            new Tool("Close","Esc",false,this::onClose));
        double bw=112,bh=19,left=x+7,top=y+h-30-tools.size()*(bh+3);
        for(int i=0;i<tools.size();i++){var tool=tools.get(i);button(g,left,top+i*(bh+3),bw,bh,tool.label(),tool.hint(),tool.active(),mx,my,alpha,tool.action());}
    }
    private void cursorInfo(GuiGraphicsExtractor g,int mx,int my,double alpha){
        if(!inside(mx,my)||module!=null&&!module.coordinates.getValue()){return;}
        double wx=worldX(mx),wz=worldZ(my);int bx=(int)Math.floor(wx),bz=(int)Math.floor(wz);Integer known=height(wx,wz);
        String coords=bx+", "+(known==null?"~":known)+", "+bz;
        String biome=(module==null||module.biome.getValue())&&dimension.equals(maps.dimension())?MapMarkers.biomeAt(maps.minecraft(),bx,known==null?64:known,bz):null;
        double top=y+(panel?23:7),width=Math.max(RenderUtil.textWidth(font,coords,false),biome==null?0:RenderUtil.textWidth(font,biome,false)*.75)+16,left=x+w/2-width/2;
        pill(g,left,top,width,biome==null?16:27,alpha);
        RenderUtil.text(g,font,coords,x+w/2-RenderUtil.textWidth(font,coords,false)/2.0,top+4,1,Theme.opacity(Theme.TEXT,alpha));
        if(biome!=null){RenderUtil.text(g,font,biome,x+w/2-RenderUtil.textWidth(font,biome,false)*.375,top+16,.75,Theme.opacity(Theme.SECONDARY,alpha));}
    }
    private void topRight(GuiGraphicsExtractor g,int mx,int my,double alpha){
        double top=y+(panel?23:7),right=x+w-(sideOpen?SIDE_WIDTH+11:34);
        String name=WaypointManagerScreen.dimensionName(dimension);double dw=RenderUtil.textWidth(font,name,false)*.8+24;
        button(g,right-dw,top,dw,19,name,null,false,mx,my,alpha,()->{
            var items=new ArrayList<Item>();
            for(String id:maps.dimensions()){items.add(new Item(WaypointManagerScreen.dimensionName(id),()->{dimension=id;following=false;}));}
            open(right-MENU_WIDTH,top+21,items);
        });
    }
    private void strip(GuiGraphicsExtractor g,int mx,int my,double alpha){
        double sx=stripX(),top=stripTop(),cell=stripCell();
        pill(g,sx-3,top-38,18,stripBottom()-top+41,alpha);
        for(int i=0;i<2;i++){
            boolean active=(i==0?MapLayerPolicy.Mode.AUTO:MapLayerPolicy.Mode.SURFACE)==viewMode;
            RenderUtil.drawRoundedRect(g,sx,top-35+i*17,12,14,3,active?Theme.opacity(Theme.accent(),alpha*.85):0x30FFFFFF);
            RenderUtil.text(g,font,i==0?"A":"S",sx+3.5,top-31+i*17,.7,Theme.opacity(Theme.TEXT,alpha));
        }
        int selected=viewMode==MapLayerPolicy.Mode.CAVE?bandIndex(selectedBand()):-1;
        var player=maps.minecraft().player;int here=player==null?-1:bandIndex(player.blockPosition().getY());
        for(int i=0;i<BANDS;i++){
            int color=i==selected?Theme.opacity(Theme.accent(),alpha*.95):i==here?Theme.opacity(0xFFFFFFFF,alpha*.7):i%2==0?0x38FFFFFF:0x22FFFFFF;
            RenderUtil.drawRoundedRect(g,sx,top+i*cell+.5,12,Math.max(1,cell-1),1.5,color);
        }
        if(overStrip(mx,my)){
            String label=my<top-17?"Auto: follow your layer":my<top?"Surface":"Y "+(TOP_BAND-MapLayerPolicy.BAND*Math.clamp((int)((my-top)/cell),0,BANDS-1))+" to "+(TOP_BAND-MapLayerPolicy.BAND*Math.clamp((int)((my-top)/cell),0,BANDS-1)+MapLayerPolicy.BAND-1);
            double width=RenderUtil.textWidth(font,label,false)*.7+8;
            MapMarkers.tag(g,font,label,sx-width-6,my-6,alpha);
        }
    }
    private void statusBar(GuiGraphicsExtractor g,double alpha){
        var player=maps.minecraft().player;
        String text=!maps.connected()?"Connect to a world to view explored terrain":WaypointManagerScreen.dimensionName(dimension)+" · "+layerName()+" · "+String.format(Locale.ROOT,zoom<1?"%.2fx":"%.1fx",zoom)+(player==null?"":" · You "+player.getBlockX()+", "+player.getBlockY()+", "+player.getBlockZ());
        double width=RenderUtil.textWidth(font,text,false)*.75+14,left=x+w/2-width/2,top=y+h-21;
        if(module==null||module.scaleBar.getValue()){
            double blocks=niceLength(110/zoom),bar=blocks*zoom;String unit=blocks>=1000?(blocks/1000)+"km":(long)blocks+"m";
            double barX=x+(module==null||module.toolbar.getValue()?127:10),barY=y+h-10;
            RenderUtil.line(g,barX,barY,barX+bar,barY,1.4,Theme.opacity(0xFFFFFFFF,alpha*.9));
            RenderUtil.line(g,barX,barY-3,barX,barY+3,1.4,Theme.opacity(0xFFFFFFFF,alpha*.9));RenderUtil.line(g,barX+bar,barY-3,barX+bar,barY+3,1.4,Theme.opacity(0xFFFFFFFF,alpha*.9));
            RenderUtil.text(g,font,unit,barX+2,barY-10,.7,Theme.opacity(0xFFFFFFFF,alpha*.9));
        }
        pill(g,left,top,width,15,alpha);
        RenderUtil.text(g,font,text,left+7,top+4.5,.75,Theme.opacity(Theme.TEXT,alpha));
    }
    private static double niceLength(double maximum){
        double power=Math.pow(10,Math.floor(Math.log10(maximum)));
        for(double step:new double[]{5,2,1}){if(step*power<=maximum){return step*power;}}
        return power;
    }

    // ---- waypoint side list ----
    private void toggleSide(){sideOpen=!sideOpen;if(!sideOpen&&searchBox!=null){searchBox.setFocused(false);searchBox.setValue("");}save();}
    private void toggleGoTo(){
        boolean show=!gotoBox.isVisible();gotoBox.setVisible(show);gotoBox.setValue("");
        if(show){setFocused(gotoBox);gotoBox.setFocused(true);}else{gotoBox.setFocused(false);}
    }
    private void submitGoTo(){
        var parts=gotoBox.getValue().trim().split("[\\s,]+");
        try{
            if(parts.length==2||parts.length==3){goTo(Double.parseDouble(parts[0]),Double.parseDouble(parts[parts.length-1]));toggleGoTo();}
        }catch(NumberFormatException ignored){gotoBox.setTextColor(0xFFFF6B6B);}
    }
    private void openSettings(){
        var gui=Client.INSTANCE.getClickGui();
        if(gui!=null){gui.selectCategory(dev.riserecode.module.Category.HUD);gui.open(this);}
    }
    private List<WaypointStore.Waypoint> sideRows(){
        String query=searchBox==null?"":searchBox.getValue().trim().toLowerCase(Locale.ROOT);var player=maps.minecraft().player;
        var rows=new ArrayList<>(maps.waypoints().displayed().stream().filter(p->p.dimension().equals(dimension)&&p.name().toLowerCase(Locale.ROOT).contains(query)).toList());
        if(player!=null&&dimension.equals(maps.dimension())){rows.sort(java.util.Comparator.comparingDouble(p->Math.hypot(p.x()-player.getX(),p.z()-player.getZ())));}
        else{rows.sort(java.util.Comparator.comparing(WaypointStore.Waypoint::name,String.CASE_INSENSITIVE_ORDER));}
        return rows;
    }
    private void side(GuiGraphicsExtractor g,int mx,int my,double alpha){
        double left=x+w-SIDE_WIDTH-5,top=y+5,height=h-10;
        pill(g,left,top,SIDE_WIDTH,height,alpha);
        RenderUtil.text(g,font,"Waypoints",left+10,top+8,1,Theme.opacity(Theme.TEXT,alpha));
        double listTop=top+42,listHeight=height-42-30;
        var rows=sideRows();var player=maps.minecraft().player;
        sideScroll=Math.clamp(sideScroll,0,Math.max(0,rows.size()*SIDE_ROW-listHeight));
        RenderUtil.scissorPush(g,left,listTop,SIDE_WIDTH,listHeight);
        try{
            if(rows.isEmpty()){RenderUtil.text(g,font,"No waypoints here",left+12,listTop+8,.8,Theme.opacity(Theme.SECONDARY,alpha));}
            for(int i=0;i<rows.size();i++){
                double row=listTop+i*SIDE_ROW-sideScroll;
                if(row+SIDE_ROW<listTop||row>listTop+listHeight){continue;}
                var p=rows.get(i);boolean hover=mx>=left+4&&mx<left+SIDE_WIDTH-4&&my>=row&&my<row+SIDE_ROW-1&&my>=listTop&&my<listTop+listHeight;
                if(hover){RenderUtil.drawRoundedRect(g,left+4,row,SIDE_WIDTH-8,SIDE_ROW-1,5,0x18FFFFFF);}
                double fade=p.visible()?alpha:alpha*.45;
                MapMarkers.waypoint(g,font,p,left+17,row+12.5,11,fade);
                RenderUtil.text(g,font,RenderUtil.ellipsize(font,p.name(),150),left+30,row+4,.85,Theme.opacity(Theme.TEXT,fade));
                String where=p.x()+", "+p.y()+", "+p.z();
                if(player!=null&&p.dimension().equals(maps.dimension())){double m=player.position().distanceTo(new Vec3(p.x()+.5,p.y()+.5,p.z()+.5));where+=" · "+(m>=1000?String.format(Locale.ROOT,"%.1fkm",m/1000):Math.round(m)+"m");}
                RenderUtil.text(g,font,where,left+30,row+14.5,.7,Theme.opacity(Theme.SECONDARY,fade));
                hits.add(new Hit(left+4,Math.max(row,listTop),SIDE_WIDTH-8,Math.min(row+SIDE_ROW-1,listTop+listHeight)-Math.max(row,listTop),()->goTo(p.x()+.5,p.z()+.5),()->open(mx,my,waypointItems(p))));
            }
        }finally{RenderUtil.scissorPop(g);}
        double foot=top+height-25,half=(SIDE_WIDTH-20-4)/2.0;
        button(g,left+10,foot,half,19,"Add here",null,false,mx,my,alpha,()->{var player2=maps.minecraft().player;if(player2!=null){add(player2.getX(),player2.getZ());}});
        button(g,left+10+half+4,foot,half,19,"Manager",null,false,mx,my,alpha,()->minecraft.gui.setScreen(new WaypointManagerScreen(this,maps)));
    }

    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){ }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        bounds();double alpha=dev.riserecode.ui.animation.PopIn.progress((System.nanoTime()-opened)*1e-9,dev.riserecode.ui.animation.PopIn.windowDuration("Map"));displayScale=dev.riserecode.ui.animation.PopIn.windowScale("Map",alpha);
        g.pose().pushMatrix();g.pose().translate((float)(x+w/2),(float)(y+h/2));g.pose().scale((float)displayScale,(float)displayScale);g.pose().translate((float)(-x-w/2),(float)(-y-h/2));
        try{draw(g,(int)localX(mx),(int)localY(my),alpha);}finally{g.pose().popMatrix();}
        boolean showSearch=sideOpen;searchBox.setVisible(showSearch);
        if(showSearch){searchBox.setX((int)(x+w-SIDE_WIDTH+8));searchBox.setY((int)(y+29));RenderUtil.drawRoundedRect(g,x+w-SIDE_WIDTH+2,y+24,SIDE_WIDTH-14,16,5,0x18FFFFFF);}
        gotoBox.setX((int)(x+w/2-62));gotoBox.setY((int)(y+(panel?23:7)+35));
        if(gotoBox.isVisible()){RenderUtil.drawRoundedRect(g,x+w/2-70,y+(panel?23:7)+30,140,16,5,Theme.opacity(Theme.SURFACE,.92));}
        super.extractRenderState(g,mx,my,delta);
    }
    private void pan(double dt){
        if(typing()){return;}
        var window=minecraft.getWindow();double step=dt*420/zoom,dx=0,dz=0;
        if(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,263)){dx-=step;}
        if(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,262)){dx+=step;}
        if(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,265)){dz-=step;}
        if(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,264)){dz+=step;}
        if(dx!=0||dz!=0){stopFollowing();anchored=false;targetX+=dx;targetZ+=dz;}
    }
    private void draw(GuiGraphicsExtractor g,int mx,int my,double alpha){
        hits.clear();
        long now=System.nanoTime();double dt=last==0?.016:Math.min(.1,(now-last)*1e-9);last=now;double blend=Theme.bool("Reduced Motion",false)?1:1-Math.exp(-dt*18);
        pan(dt);
        var player=maps.minecraft().player;
        if(following&&player!=null&&dimension.equals(maps.dimension())){targetX=player.getX();targetZ=player.getZ();}
        targetZoom=Math.clamp(targetZoom,MIN_ZOOM,MAX_ZOOM);zoom+=(targetZoom-zoom)*blend;
        if(anchored){centerX=anchorX-anchorPixelX/zoom;centerZ=anchorZ-anchorPixelZ/zoom;targetX=anchorX-anchorPixelX/targetZoom;targetZ=anchorZ-anchorPixelZ/targetZoom;}else{centerX+=(targetX-centerX)*blend;centerZ+=(targetZ-centerZ)*blend;}
        if(panel){RenderUtil.drawDropShadow(g,x,y,w,h,8,0xC0000000);}
        boolean themed=module==null||module.backdrop.getValue()==WorldMapModule.Backdrop.THEME;
        MapCanvas.draw(g,maps,dimension,centerX,centerZ,x,y,w,h,zoom,0,false,false,alpha,viewMode,caveY,false,false,themed,false);
        overlay(g,mx,my,alpha);
        if(panel){RenderUtil.drawRoundedRect(g,x,y,w,19,4,Theme.SURFACE);RenderUtil.text(g,font,"World map · drag to move",x+8,y+6,.75,Theme.TEXT);}
        cursorInfo(g,mx,my,alpha);
        topRight(g,mx,my,alpha);
        strip(g,mx,my,alpha);
        toolbar(g,mx,my,alpha);
        statusBar(g,alpha);
        if(sideOpen){side(g,mx,my,alpha);}
        if(panel){RenderUtil.line(g,x+w-10,y+h-4,x+w-4,y+h-10,1.4,Theme.TEXT);}
        drawMenu(g,mx,my);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        bounds();
        if(gotoBox.isVisible()&&gotoBox.isMouseOver(event.x(),event.y())||searchBox.isVisible()&&searchBox.isMouseOver(event.x(),event.y())){return super.mouseClicked(event,twice);}
        if(getFocused()!=null){setFocused(null);gotoBox.setFocused(false);searchBox.setFocused(false);}
        var e=new MouseButtonEvent(localX(event.x()),localY(event.y()),event.buttonInfo());
        if(clickMenu(e.x(),e.y())){return true;}
        if(!inside(e.x(),e.y())){return true;}
        if(e.button()==0&&panel&&e.x()>x+w-14&&e.y()>y+h-14){resizing=true;return true;}
        if(e.button()==0&&panel&&e.y()<y+20){dragging=true;return true;}
        for(int i=hits.size()-1;i>=0;i--){
            var hit=hits.get(i);
            if(!hit.contains(e.x(),e.y())){continue;}
            if(e.button()==0&&hit.left()!=null){hit.left().run();return true;}
            if(e.button()==1&&hit.right()!=null){hit.right().run();return true;}
            if(e.button()==0||e.button()==1){return true;}
        }
        if(e.button()==0&&overStrip(e.x(),e.y())){stripDrag=true;pickStrip(e.y());return true;}
        if(e.button()==1){openContext(e.x(),e.y());}
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        var e=new MouseButtonEvent(localX(event.x()),localY(event.y()),event.buttonInfo());dx/=displayScale;dy/=displayScale;
        if(e.button()!=0){return false;}
        if(stripDrag){pickStrip(e.y());return true;}
        if(resizing){nw=Math.clamp((w+dx)/width,Math.min(280.0/width,1),1);nh=Math.clamp((h+dy)/height,Math.min(170.0/height,1),1);anchored=false;return true;}
        if(dragging){nx=Math.clamp((x+dx)/width,0,1);ny=Math.clamp((y+dy)/height,0,1);return true;}
        if(inside(e.x(),e.y())){stopFollowing();anchored=false;targetX-=dx/zoom;targetZ-=dy/zoom;}return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(stripDrag||dragging||resizing){stripDrag=dragging=resizing=false;save();}return true;}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        mx=localX(mx);my=localY(my);if(!inside(mx,my)){return false;}
        menu=List.of();
        if(sideOpen&&mx>=x+w-SIDE_WIDTH-5){sideScroll=Math.max(0,sideScroll-vertical*SIDE_ROW);return true;}
        var window=minecraft.getWindow();
        if(com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,340)||com.mojang.blaze3d.platform.InputConstants.isKeyDown(window,344)){
            viewMode=MapLayerPolicy.Mode.CAVE;
            caveY=Math.clamp(selectedBand()+(vertical>0?MapLayerPolicy.BAND:-MapLayerPolicy.BAND),-64,TOP_BAND);save();return true;
        }
        double next=Math.clamp(targetZoom*Math.pow(1.2,vertical),MIN_ZOOM,MAX_ZOOM);
        if(following){targetZoom=next;return true;}
        double px=mx-x-w/2,pz=my-y-h/2;
        anchorX=centerX+px/zoom;anchorZ=centerZ+pz/zoom;anchored=true;anchorPixelX=px;anchorPixelZ=pz;targetZoom=next;return true;
    }
    @Override public boolean keyPressed(KeyEvent e){
        if(typing()){
            if(e.isEscape()){if(gotoBox.isFocused()){toggleGoTo();}else{searchBox.setFocused(false);setFocused(null);}return true;}
            if(gotoBox.isFocused()&&(e.key()==257||e.key()==335)){submitGoTo();return true;}
            return super.keyPressed(e);
        }
        if(e.isEscape()){if(!menu.isEmpty()){menu=List.of();return true;}onClose();return true;}
        if(RiseRecodeClient.mapKey().matches(e)){onClose();return true;}
        if(RiseRecodeClient.waypointKey().matches(e)){
            double mx=localX(minecraft.mouseHandler.getScaledXPos(minecraft.getWindow())),my=localY(minecraft.mouseHandler.getScaledYPos(minecraft.getWindow()));
            add(worldX(mx),worldZ(my));return true;
        }
        switch(e.key()){
            case 61,334,266->{targetZoom*=1.25;return true;}
            case 45,333,267->{targetZoom/=1.25;return true;}
            case 71->{toggleGrid();return true;}
            case 72->{recenter();return true;}
            case 74->{toggleGoTo();return true;}
            case 76->{toggleSide();return true;}
            default->{return super.keyPressed(e);}
        }
    }
    @Override public void onClose(){save();minecraft.gui.setScreen(parent);}
    @Override public void removed(){save();}
    @Override public boolean isPauseScreen(){return false;}
}
