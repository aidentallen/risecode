package dev.riserecode.map;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** Marker drawing shared by the minimap and the world map, so both show waypoints and entities the same way. */
public final class MapMarkers {
    public enum PlayerMarker { ARROW, HEAD }
    public static final int PLAYER=0xFFF2F2F2,HOSTILE=0xFFE5484D,PASSIVE=0xFF4ADE80,OTHER=0xFFFFD166,ITEM=0xFF7DD3FC;
    /** Pixels-per-block factor that makes a mob head fill its frame. */
    private static final double HEAD_SCALE=1.0;
    private MapMarkers(){}

    public static int entityColor(Entity entity){
        if(entity instanceof Player){return PLAYER;}
        if(entity instanceof ItemEntity){return ITEM;}
        return entity.getType().getCategory()==MobCategory.MONSTER?HOSTILE:entity instanceof LivingEntity?PASSIVE:OTHER;
    }

    /** Colored square with the first letter of the name; death points show an x, tracked ones a white ring. */
    public static void waypoint(GuiGraphicsExtractor g,Font font,WaypointStore.Waypoint point,double x,double y,double size,double alpha){
        int color=point.color()|0xFF000000;double half=size/2;
        RenderUtil.drawRoundedRect(g,x-half-1,y-half-1,size+2,size+2,3,Theme.opacity(point.tracked()?0xFFFFFFFF:0xFF000000,alpha*.8));
        RenderUtil.drawRoundedRect(g,x-half,y-half,size,size,2.5,Theme.opacity(color,alpha));
        String letter=point.death()?"x":point.name().isBlank()?"?":point.name().substring(0,1).toUpperCase(java.util.Locale.ROOT);
        double scale=size/11,width=RenderUtil.textWidth(font,letter,false)*scale;
        int luminance=(color>>16&255)*299+(color>>8&255)*587+(color&255)*114;
        RenderUtil.text(g,font,letter,x-width/2,y-4*scale,scale,Theme.opacity(luminance>140000?0xFF101216:0xFFFFFFFF,alpha));
    }

    /** Name tag pill; x and y are its top left corner. */
    public static void tag(GuiGraphicsExtractor g,Font font,String text,double x,double y,double alpha){
        double scale=.7,width=RenderUtil.textWidth(font,text,false)*scale;
        RenderUtil.drawRoundedRect(g,x,y,width+8,12,4,Theme.opacity(0xFF101216,alpha*.82));
        RenderUtil.text(g,font,text,x+4,y+3,scale,Theme.opacity(0xFFFFFFFF,alpha));
    }

    /** Arrow around x and y; angle 0 points up the screen. */
    public static void arrow(GuiGraphicsExtractor g,double x,double y,double angle,double size,int color,double alpha){
        g.pose().pushMatrix();g.pose().translate((float)x,(float)y);g.pose().rotate((float)angle);
        try{
            double wide=size*.72,tail=size*.85,notch=size*.3;
            int outline=Theme.opacity(0xFF000000,alpha*.75),fill=Theme.opacity(color,alpha);
            RenderUtil.line(g,0,-size,-wide,tail,2.6,outline);RenderUtil.line(g,0,-size,wide,tail,2.6,outline);
            RenderUtil.line(g,-wide,tail,0,notch,2.6,outline);RenderUtil.line(g,wide,tail,0,notch,2.6,outline);
            for(int i=0;i<=8;i++){double t=i/8.0,tx=-wide+2*wide*t,ty=tail+(notch-tail)*(1-Math.abs(2*t-1));RenderUtil.line(g,0,-size,tx,ty,1.2,fill);}
        }finally{g.pose().popMatrix();}
    }

    /** Skin face for players, the cropped real model for mobs. False when the entity has no head to show. */
    public static boolean head(GuiGraphicsExtractor g,Entity entity,double x,double y,double size,boolean frame,double alpha){
        double half=size/2;
        if(frame){RenderUtil.drawRoundedRect(g,x-half-1,y-half-1,size+2,size+2,2,Theme.opacity(entityColor(entity),alpha));}
        if(entity instanceof AbstractClientPlayer player){
            PlayerFaceExtractor.extractRenderState(g,player.getSkin(),(int)Math.round(x-half),(int)Math.round(y-half),(int)Math.round(size));
            return true;
        }
        if(!(entity instanceof LivingEntity living)){return false;}
        // the entity picture ignores the pose, so it gets screen coordinates and the pose is emptied around it
        var center=g.pose().transformPosition((float)x,(float)y,new org.joml.Vector2f());
        double pixels=size*Math.hypot(g.pose().m00(),g.pose().m01()),head=Math.clamp(entity.getBbWidth()*.9,.2,3),scale=living.getScale();
        float yOffset=(float)((living.getEyeHeight()-living.getBbHeight()/2)/scale);
        g.pose().pushMatrix();g.pose().identity();
        RenderUtil.scissorPush(g,center.x-pixels/2,center.y-pixels/2,pixels,pixels);
        try{
            int cx=Math.round(center.x),cy=Math.round(center.y),box=(int)Math.ceil(pixels*2);
            InventoryScreen.extractEntityInInventoryFollowsMouse(g,cx-box,cy-box,cx+box,cy+box,(int)Math.max(1,Math.round(pixels/head*HEAD_SCALE)),yOffset,cx,cy,living);
        }finally{RenderUtil.scissorPop(g);g.pose().popMatrix();}
        return true;
    }

    public static String pretty(String path){
        var out=new StringBuilder();
        for(String part:path.split("_")){if(!part.isEmpty()){out.append(out.length()>0?" ":"").append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));}}
        return out.toString();
    }

    /** Biome name at a block of the loaded world, or null when that chunk is not loaded. */
    public static String biomeAt(Minecraft mc,int x,int y,int z){
        var level=mc.level;
        if(level==null||level.getChunkSource().getChunk(x>>4,z>>4,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)==null){return null;}
        return level.getBiome(new net.minecraft.core.BlockPos(x,y,z)).unwrapKey().map(key->pretty(key.identifier().getPath())).orElse(null);
    }
}
