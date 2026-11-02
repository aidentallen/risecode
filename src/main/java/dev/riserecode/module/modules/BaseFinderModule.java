package dev.riserecode.module.modules;

import dev.riserecode.event.*;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.scanner.*;
import dev.riserecode.setting.*;
import dev.riserecode.render.EspGeometry;
import dev.riserecode.util.*;
import dev.riserecode.ui.theme.Theme;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** Loaded-only packet clues, bounded worker analysis, and immutable visual extraction. */
public final class BaseFinderModule extends Module {
    public enum Outline { BOUNDING_BOX, SURFACE_GRID, CORNERS_ONLY, NONE }
    public enum Edge { TOP_RIGHT, BOTTOM_RIGHT }
    public enum Tint { DEEP_SLATE_GLASS, CRYSTAL_CLEAR, FROSTED_VIOLET }
    public enum CardKey {
        NONE(-1), P(GLFW.GLFW_KEY_P), O(GLFW.GLFW_KEY_O), K(GLFW.GLFW_KEY_K), L(GLFW.GLFW_KEY_L), INSERT(GLFW.GLFW_KEY_INSERT), DELETE(GLFW.GLFW_KEY_DELETE), HOME(GLFW.GLFW_KEY_HOME), END(GLFW.GLFW_KEY_END);
        final int code;CardKey(int code){this.code=code;}
    }
    private final Map<Setting<?>,String> groups=new IdentityHashMap<>();
    private String group="General";
    private <S extends Setting<?>> S option(S s){groups.put(s,group);return register(s);}
    private DoubleSetting number(String name,String description,double value,double min,double max){return option(new DoubleSetting(name,description,value,min,max));}
    private BooleanSetting toggle(String name,String description){return option(new BooleanSetting(name,description,true));}
    private final EnumSetting<ChunkHeuristics.Mode> mode;
    private final DoubleSetting range,sensitivity,classicThreshold,minCells,width,fill,timeout,maxCards;
    private final BooleanSetting filter,containers,shulkers,chests,hoppers,machinery,redstone,geodes,growth,tracers,blockEsp,cards;
    private final EnumSetting<ChunkHeuristics.Method> method;
    private final EnumSetting<Outline> outline;
    private final EnumSetting<Edge> edge;
    private final EnumSetting<Tint> tint;
    private final EnumSetting<CardKey> pinKey,clearKey;
    private final ColorSetting tracerColor;
    private final ColorSetting[] colors=new ColorSetting[5];
    private final Minecraft mc;
    private ScannerBlocks classifier;
    private BaseScanner scanner;
    private net.minecraft.client.multiplayer.ClientLevel world;
    private ChunkHeuristics.Options activeOptions;
    private int cursor,scanRadius;
    private final Map<Long,Long> dismissed=new HashMap<>();
    private final Set<Long> pinned=new HashSet<>();
    private List<BaseScanner.Discovery> discoveries=List.of();
    private Long firstCard;
    private int visibleCards;
    public int visibleCards(){return visibleCards;}
    public int pinnedCards(){return pinned.size();}

    public BaseFinderModule(EventBus bus,Minecraft mc){
        super("BaseFinder","Suspicious chunk, stash, growth and amethyst clues from loaded server data.",Category.RENDER,bus);this.mc=mc;
        mode=option(new EnumSetting<>("Mode","Heuristic evaluation mode",ChunkHeuristics.Mode.ACTIVITY_CLUES));
        range=number("Scan Range","Loaded chunk radius; never requests terrain",8,1,16);
        sensitivity=number("Sensitivity","Required weighted score; lower detects more clues",30,1,100);
        classicThreshold=number("Classic Threshold","Mature blocks or block entities required",8,1,100);
        classicThreshold.setVisibleWhen(()->mode.getValue()==ChunkHeuristics.Mode.CLASSIC);
        filter=toggle("False Positive Filter","Locally suppress vanilla structure combinations");
        group="Clue Toggles & Filters";
        containers=toggle("Containers","Storage and utility containers");shulkers=toggle("Shulkers","Include shulker boxes");chests=toggle("Chests","Include chests and trapped chests");hoppers=toggle("Hoppers","Include hoppers");
        shulkers.setVisibleWhen(containers::getValue);chests.setVisibleWhen(containers::getValue);hoppers.setVisibleWhen(containers::getValue);
        machinery=toggle("Machinery","Pistons, observers and container/redstone combinations");redstone=toggle("Redstone","Wiring, switches and logic");geodes=toggle("Geodes","Geode-family and amethyst light clues");growth=toggle("Growth","Mature crops, amethyst and tall aquatic/sugar growth");
        group="Amethyst Scanner";
        method=option(new EnumSetting<>("Amethyst Method","Light maxima are ambiguous; no guaranteed anti-Xray bypass",ChunkHeuristics.Method.ANBS_PLUS_SCAN));
        minCells=number("Min Cells","Minimum six-connected geode-family cluster size",12,1,64);
        tracers=toggle("Show Tracers","Depth-tested lines from camera to discovery centroid");
        tracerColor=option(new ColorSetting("Tracer Color","Discovery tracer gradient",0xFFA335EE));
        group="Visuals & Outlines";
        outline=option(new EnumSetting<>("Chunk Outline Mode","Depth-tested chunk highlight style",Outline.CORNERS_ONLY));
        width=number("Outline Width","Outline width in pixels",1.5,1,5);fill=number("Fill Alpha","Translucent chunk faces",.035,0,1);
        blockEsp=toggle("Block ESP","Highlight up to 256 clue blocks across nearby discoveries");
        colors[0]=option(new ColorSetting("Container Color","Storage clues",0xFFFF4D4D));
        colors[1]=option(new ColorSetting("Machinery Color","Machinery and redstone clues",0xFFFF9900));colors[2]=colors[1];
        colors[3]=option(new ColorSetting("Geode Color","Amethyst and light clues",0xFFB966FF));colors[4]=option(new ColorSetting("Growth Color","Mature growth clues",0xFF00FF7F));
        group="Discovery Cards";
        cards=toggle("Discovery Cards","Frosted cards; counts and scores describe clues, not confirmed bases");
        edge=option(new EnumSetting<>("Card Position","Screen edge",Edge.TOP_RIGHT));tint=option(new EnumSetting<>("Card Tint","Frosted base tint",Tint.DEEP_SLATE_GLASS));
        timeout=number("Card Timeout","Seconds before dismissal; zero keeps cards until out of range",30,0,300);maxCards=number("Maximum Cards","Visible discovery cards",3,1,6);
        pinKey=option(new EnumSetting<>("Pin Card Key","Pin/unpin the first visible card",CardKey.P));clearKey=option(new EnumSetting<>("Clear Cards Key","Dismiss current cards; new discoveries can still appear",CardKey.DELETE));
    }
    @Override public String settingGroup(Setting<?> setting){return groups.getOrDefault(setting,"");}
    public String status(){return scanner==null?"Idle":discoveries.size()+" clues · "+scanner.queued()+" queued · "+scanner.rejected()+" dropped · "+scanner.failures()+" errors";}
    public List<BaseScanner.Discovery> discoveries(){return discoveries;}
    private ChunkHeuristics.Options options(){return new ChunkHeuristics.Options(mode.getValue(),method.getValue(),sensitivity.getValue().intValue(),classicThreshold.getValue().intValue(),minCells.getValue().intValue(),filter.getValue(),containers.getValue(),shulkers.getValue(),chests.getValue(),hoppers.getValue(),machinery.getValue(),redstone.getValue(),geodes.getValue(),growth.getValue());}
    @Override protected void onEnable(){
        if(classifier==null)classifier=new ScannerBlocks();reset();
        listen(UpdateEvent.class,this::tick);listen(ScannerPacketEvent.class,this::packet);
        listen(SeedWorldEvent.class,e->reset());listen(ChunkLifecycleEvent.class,e->{if(e.kind()==ChunkLifecycleEvent.Kind.RESET)reset();else if(e.kind()==ChunkLifecycleEvent.Kind.UNLOAD&&scanner!=null)scanner.forget(e.chunk());});
        listen(Render3DEvent.class,this::renderWorld);listen(Render2DEvent.class,e->dev.riserecode.ui.theme.ModuleAppearance.with(capabilityId(),()->renderCards(e)));listen(KeyPressEvent.class,this::key);
    }
    @Override protected void onDisable(){if(scanner!=null)scanner.close();scanner=null;world=null;discoveries=List.of();dismissed.clear();pinned.clear();firstCard=null;visibleCards=0;}
    private void reset(){if(scanner!=null)scanner.close();scanner=new BaseScanner(classifier);world=mc.level;activeOptions=options();cursor=0;scanRadius=range.getValue().intValue();discoveries=List.of();dismissed.clear();pinned.clear();firstCard=null;visibleCards=0;}
    private boolean inRange(int x,int z){return mc.player!=null&&Math.max(Math.abs(x-(mc.player.getBlockX()>>4)),Math.abs(z-(mc.player.getBlockZ()>>4)))<=range.getValue().intValue();}
    private void packet(ScannerPacketEvent e){
        if(mc.level!=world||!options().equals(activeOptions))reset();
        if(world==null||scanner==null)return;
        if(e.packet instanceof ClientboundForgetLevelChunkPacket){scanner.forget(ChunkPos.pack(e.x,e.z));return;}
        if(!inRange(e.x,e.z))return;
        scanner.packet(e.x,e.z,e.packet,world.getMinY(),world.getSectionsCount(),world.palettedContainerFactory(),activeOptions);
    }
    private void tick(UpdateEvent e){
        if(mc.level!=world||!options().equals(activeOptions)||scanRadius!=range.getValue().intValue())reset();
        if(world==null||mc.player==null)return;
        // Backfill resident chunks gradually, including movement into range and dropped work.
        if(e.getTick()%2==0&&scanner.queued()<4){
            int r=scanRadius,side=r*2+1;
            for(int attempt=0;attempt<32;attempt++){
                int offset=Math.floorMod(cursor++,side*side),x=(mc.player.getBlockX()>>4)+offset%side-r,z=(mc.player.getBlockZ()>>4)+offset/side-r;
                long key=ChunkPos.pack(x,z);if(scanner.hasBase(key))continue;
                var chunk=world.getChunkSource().getChunk(x,z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);if(chunk==null)continue;
                var sections=chunk.getSections();var copies=new LevelChunkSection[sections.length];for(int i=0;i<sections.length;i++)copies[i]=sections[i].copy();
                var light=new ClientboundLightUpdatePacketData(chunk.getPos(),world.getLightEngine(),null,null);
                scanner.snapshot(x,z,world.getMinY(),copies,light,activeOptions);break;
            }
        }
        var found=new ArrayList<BaseScanner.Discovery>();
        for(var d:scanner.discoveries()){
            var pos=ChunkPos.unpack(d.chunk());if(!inRange(pos.x(),pos.z())||!world.hasChunk(pos.x(),pos.z())){scanner.forget(d.chunk());continue;}found.add(d);
        }
        found.sort(Comparator.comparingDouble(this::distance));discoveries=List.copyOf(found);
        var live=new HashSet<Long>();discoveries.forEach(d->live.add(d.chunk()));dismissed.keySet().retainAll(live);pinned.retainAll(live);
    }
    private Vec3 center(BaseScanner.Discovery d){var p=ChunkPos.unpack(d.chunk());var f=d.finding();return new Vec3(p.x()*16+f.x(),f.y(),p.z()*16+f.z());}
    private double distance(BaseScanner.Discovery d){return mc.player==null?0:mc.player.position().distanceTo(center(d));}
    private void key(KeyPressEvent e){
        if(!e.isPress()||mc.gui.screen()!=null||mc.player==null)return;
        if(clearKey.getValue()!=CardKey.NONE&&e.getKey()==clearKey.getValue().code){for(var d:discoveries)dismissed.put(d.chunk(),d.discoveredNanos());pinned.clear();firstCard=null;e.cancel();}
        else if(pinKey.getValue()!=CardKey.NONE&&e.getKey()==pinKey.getValue().code&&firstCard!=null){if(!pinned.remove(firstCard))pinned.add(firstCard);e.cancel();}
    }
    private void renderWorld(Render3DEvent e){
        if(mc.level!=world||mc.player==null)return;
        int count=0,blocks=0;
        for(var d:discoveries){var pos=ChunkPos.unpack(d.chunk());if(!inRange(pos.x(),pos.z())||count++>=32)continue;
            var f=d.finding();var color=colors[f.dominant()];int argb=color.getValue();var b=new AABB(pos.x()*16,d.minY(),pos.z()*16,pos.x()*16+16,d.minY()+d.height(),pos.z()*16+16);
            switch(outline.getValue()){
                case BOUNDING_BOX -> {EspGeometry.box(e,b,color,width.getValue(),true);faces(e,b,(argb&0xFFFFFF)|((int)(fill.getValue()*255)<<24));}
                case CORNERS_ONLY -> {corners(e,b,argb);faces(e,b,(argb&0xFFFFFF)|((int)(fill.getValue()*255)<<24));}
                case SURFACE_GRID -> {double y=world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.x()*16+8,pos.z()*16+8)+.05;for(int i=0;i<=16;i++){line(e,b.minX+i,y,b.minZ,b.minX+i,y,b.maxZ,argb);line(e,b.minX,y,b.minZ+i,b.maxX,y,b.minZ+i,argb);}faces(e,new AABB(b.minX,y-.01,b.minZ,b.maxX,y,b.maxZ),(argb&0xFFFFFF)|((int)(fill.getValue()*255)<<24));}
                case NONE -> { }
            }
            if(blockEsp.getValue())for(var target:f.targets()){if(blocks++>=256)break;int i=target.index();EspGeometry.box(e,new AABB(b.minX+(i&15),d.minY()+(i>>8),b.minZ+((i>>4)&15),b.minX+(i&15)+1,d.minY()+(i>>8)+1,b.minZ+((i>>4)&15)+1),colors[target.category()],width.getValue(),true);}
            if(tracers.getValue()){var camera=e.getContext().camera();var v=camera.forwardVector();var start=camera.position().add(v.x()*.3,v.y()*.3,v.z()*.3);e.addLine(start,center(d),Theme.opacity(tracerColor.getValue(),.18),tracerColor.getValue(),width.getValue().floatValue(),true);}
        }
    }
    private void line(Render3DEvent e,double ax,double ay,double az,double bx,double by,double bz,int color){e.addLine(ax,ay,az,bx,by,bz,color,color,width.getValue().floatValue(),true);}
    private void corners(Render3DEvent e,AABB b,int color){for(int x=0;x<2;x++)for(int y=0;y<2;y++)for(int z=0;z<2;z++){double px=x==0?b.minX:b.maxX,py=y==0?b.minY:b.maxY,pz=z==0?b.minZ:b.maxZ;line(e,px,py,pz,px+(x==0?2:-2),py,pz,color);line(e,px,py,pz,px,py+(y==0?2:-2),pz,color);line(e,px,py,pz,px,py,pz+(z==0?2:-2),color);}}
    private static void faces(Render3DEvent e,AABB b,int color){
        if((color>>>24)==0)return;
        Vec3[] v={new Vec3(b.minX,b.minY,b.minZ),new Vec3(b.maxX,b.minY,b.minZ),new Vec3(b.maxX,b.maxY,b.minZ),new Vec3(b.minX,b.maxY,b.minZ),new Vec3(b.minX,b.minY,b.maxZ),new Vec3(b.maxX,b.minY,b.maxZ),new Vec3(b.maxX,b.maxY,b.maxZ),new Vec3(b.minX,b.maxY,b.maxZ)};
        for(int[] q:new int[][]{{0,1,2,3},{5,4,7,6},{4,0,3,7},{1,5,6,2},{3,2,6,7},{4,5,1,0}})e.addQuad(v[q[0]],v[q[1]],v[q[2]],v[q[3]],color);
    }
    private void renderCards(Render2DEvent e){
        visibleCards=0;firstCard=null;if(!cards.getValue()||mc.level!=world||mc.player==null||mc.gui.hud.isHidden())return;
        var g=e.getGraphics();long now=System.nanoTime();double cardW=Math.min(248,mc.getWindow().getGuiScaledWidth()-16),cardH=90;int shown=0;
        String[] titles={"Containers","Machinery","Redstone","Amethyst / Geode","Mature Growth"};
        var icons=new net.minecraft.world.item.Item[]{Items.CHEST,Items.PISTON,Items.REPEATER,Items.AMETHYST_CLUSTER,Items.WHEAT};
        for(var d:discoveries){
            if(shown>=maxCards.getValue().intValue()||(shown+1)*(cardH+6)+8>mc.getWindow().getGuiScaledHeight())break;
            if(!inRange(ChunkPos.unpack(d.chunk()).x(),ChunkPos.unpack(d.chunk()).z()))continue;
            if(Objects.equals(dismissed.get(d.chunk()),d.discoveredNanos()))continue;
            double age=(now-d.discoveredNanos())*1e-9;boolean pin=pinned.contains(d.chunk());if(!pin&&timeout.getValue()>0&&age>timeout.getValue())continue;
            if(firstCard==null)firstCard=d.chunk();
            double progress=dev.riserecode.ui.theme.Theme.bool("Reduced Motion",false)?1:Math.clamp(age/Math.max(.001,popDuration()),0,1);double alpha=progress;
            if(!pin&&timeout.getValue()>0)alpha*=Math.clamp((timeout.getValue()-age)/.4,0,1);
            double x=mc.getWindow().getGuiScaledWidth()-cardW-8+(1-progress)*(1-progress)*28;
            double y=edge.getValue()==Edge.TOP_RIGHT?8+shown*(cardH+6):mc.getWindow().getGuiScaledHeight()-8-cardH-shown*(cardH+6);shown++;
            int tintColor=switch(tint.getValue()){case DEEP_SLATE_GLASS->0xB0182230;case CRYSTAL_CLEAR->0x507C899B;case FROSTED_VIOLET->0xA035234F;};
            int accent=colors[d.finding().dominant()].getValue();
            RenderUtil.drawDropShadow(g,x,y,cardW,cardH,7,Theme.opacity(0x60000000,alpha));BlurUtil.drawBox(g,x,y,cardW,cardH,7,alpha*.85);
            RenderUtil.drawRoundedRect(g,x,y,cardW,cardH,7,Theme.opacity(tintColor,alpha));RenderUtil.drawRoundedBorder(g,x,y,cardW,cardH,7,Theme.opacity(accent,alpha*.45),1);
            g.item(icons[d.finding().dominant()].getDefaultInstance(),(int)x+7,(int)y+7);
            text(g,"Suspicious Chunk"+(pin?" · Pinned":""),x+28,y+7,.75,Theme.opacity(0xFFFFFFFF,alpha));
            text(g,titles[d.finding().dominant()]+" · Score "+d.finding().confidence()+"/100",x+28,y+19,.65,Theme.opacity(accent,alpha));
            Vec3 c=center(d),delta=c.subtract(mc.player.position());String[] compass={"S","SW","W","NW","N","NE","E","SE"};String direction=compass[Math.floorMod((int)Math.round(Math.atan2(-delta.x,delta.z)/(Math.PI/4)),8)];
            text(g,String.format(Locale.ROOT,"[%d, %d, %d]  ~%.0fm %s",(int)Math.floor(c.x),(int)Math.floor(c.y),(int)Math.floor(c.z),distance(d),direction),x+9,y+34,.7,Theme.opacity(0xFFE1E8F2,alpha));
            var f=d.finding();text(g,"Containers: "+f.count(0)+"   Machinery: "+f.count(1)+"   Redstone: "+f.count(2),x+9,y+48,.6,Theme.opacity(0xFFD7E0EC,alpha));
            text(g,"Amethyst Cl. 4: "+f.matureAmethyst()+"   Growth: "+f.count(4),x+9,y+60,.6,Theme.opacity(0xFFD7E0EC,alpha));
            text(g,"Geode cells: "+f.count(3)+"   Light clues: "+f.lightClues(),x+9,y+72,.6,Theme.opacity(0xFFD7E0EC,alpha));
        }
        visibleCards=shown;
    }
    private void text(net.minecraft.client.gui.GuiGraphicsExtractor g,String text,double x,double y,double scale,int color){RenderUtil.text(g,mc.font,RenderUtil.ellipsize(mc.font,text,Math.max(20,(mc.getWindow().getGuiScaledWidth()-x-16)/scale)),x,y,scale,color);}
}
