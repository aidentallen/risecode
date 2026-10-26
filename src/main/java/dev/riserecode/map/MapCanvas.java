package dev.riserecode.map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.riserecode.ui.menu.BackgroundAssets;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;
import dev.riserecode.util.RenderUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;

/** Both maps share detail, altitude selection, coarse fallback, texture sampling and markers. */
public final class MapCanvas {
    private static final com.mojang.blaze3d.pipeline.RenderPipeline PIPELINE=UiMesh.pipeline("map","map");
    private static final Identifier WHITE=UiMesh.id("textures/gui/white.png");
    private MapCanvas(){ }
    public static void draw(GuiGraphicsExtractor g,MapService maps,String dimension,double cx,double cz,double x,double y,double w,double h,double zoom,double rotation,boolean markers,boolean compass){draw(g,maps,dimension,cx,cz,x,y,w,h,zoom,rotation,markers,compass,1);}
    public static void draw(GuiGraphicsExtractor g,MapService maps,String dimension,double cx,double cz,double x,double y,double w,double h,double zoom,double rotation,boolean markers,boolean compass,double opacity){draw(g,maps,dimension,cx,cz,x,y,w,h,zoom,rotation,markers,compass,opacity,maps.mode(),Integer.MIN_VALUE,false,false);}
    private static void image(GuiGraphicsExtractor g,MapService maps,Identifier texture,double x,double y,double w,double h,int color,int[] clip,boolean linear){
        var view=maps.minecraft().getTextureManager().getTexture(texture).getTextureView();
        UiMesh.quad(g,PIPELINE,TextureSetup.singleTexture(view,RenderSystem.getSamplerCache().getClampToEdge(linear?FilterMode.LINEAR:FilterMode.NEAREST)),x,y,w,h,0,0,1,1,color,color,clip[0],clip[1],clip[2],clip[3]);
    }
    public static void draw(GuiGraphicsExtractor g,MapService maps,String dimension,double cx,double cz,double x,double y,double w,double h,double zoom,double rotation,boolean markers,boolean compass,double opacity,MapLayerPolicy.Mode mode,int altitude,boolean circle,boolean minimap){
        Vector2f center=g.pose().transformPosition((float)(x+w/2),(float)(y+h/2),new Vector2f());
        double sx=Math.hypot(g.pose().m00(),g.pose().m01()),sy=Math.hypot(g.pose().m10(),g.pose().m11());
        int[] clip=circle?new int[]{(int)(center.x/g.guiWidth()*16000),(int)(center.y/g.guiHeight()*16000),(int)(w*sx/2/g.guiWidth()*16000),(int)(h*sy/2/g.guiHeight()*16000)}:new int[4];
        var background=Theme.manager().choice(minimap?"Minimap Background":"World Map Background",ThemeManager.MapBackground.class);
        if(background==ThemeManager.MapBackground.SOLID){image(g,maps,WHITE,x,y,w,h,Theme.opacity(Theme.manager().color("Map Solid Color"),opacity),clip,true);}
        else if(background!=ThemeManager.MapBackground.TRANSPARENT_WORLD){
            var kind=Theme.manager().choice(background==ThemeManager.MapBackground.IMAGE?(minimap?"Minimap Image":"World Map Image"):"Background",ThemeManager.Background.class);
            if(kind==ThemeManager.Background.NONE){}
            else if(dev.riserecode.ui.menu.AnimatedPreview.animated(kind)){dev.riserecode.ui.menu.AnimatedPreview.draw(g,kind,x,y,w,h,Theme.opacity(Theme.manager().color("Background Tint"),opacity*Theme.number("Background Opacity",1)),circle);}
            else if(background==ThemeManager.MapBackground.INHERIT&&(kind==ThemeManager.Background.SOLID||kind==ThemeManager.Background.GRADIENT)){
                var view=maps.minecraft().getTextureManager().getTexture(WHITE).getTextureView();
                int top=Theme.manager().color(kind==ThemeManager.Background.SOLID?"Background Color":"Gradient Start"),bottom=kind==ThemeManager.Background.SOLID?top:Theme.manager().color("Gradient End");
                UiMesh.quad(g,PIPELINE,TextureSetup.singleTexture(view,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),x,y,w,h,0,0,1,1,Theme.opacity(top,opacity),Theme.opacity(bottom,opacity),clip[0],clip[1],clip[2],clip[3]);
            }else{image(g,maps,BackgroundAssets.asset(kind),x,y,w,h,Theme.opacity(Theme.manager().color("Background Tint"),opacity*Theme.number("Background Opacity",1)),clip,true);}
        }
        image(g,maps,WHITE,x,y,w,h,Theme.opacity(Theme.manager().color("Unexplored Color"),opacity*Theme.number("Unexplored Opacity",.55)),clip,true);
        RenderUtil.scissorPush(g,x,y,w,h);
        try{
            g.pose().pushMatrix();g.pose().translate((float)(x+w/2),(float)(y+h/2));g.pose().rotate((float)rotation);
            double bw=Math.abs(Math.cos(rotation))*w+Math.abs(Math.sin(rotation))*h,bh=Math.abs(Math.sin(rotation))*w+Math.abs(Math.cos(rotation))*h;
            int target=MapLodPolicy.viewportLevel((float)zoom,bw,bh);
            // Draw valid coarse coverage first; loading details cannot turn explored areas blank.
            for(int lod=Math.min(8,target+2);lod>=target;lod-=2){
                int span=16<<lod,minX=(int)Math.floor((cx-bw/2/zoom)/span),maxX=(int)Math.floor((cx+bw/2/zoom)/span),minZ=(int)Math.floor((cz-bh/2/zoom)/span),maxZ=(int)Math.floor((cz+bh/2/zoom)/span);
                int tx=(int)Math.floor(cx/span),tz=(int)Math.floor(cz/span),rings=Math.min(20,Math.max(Math.max(tx-minX,maxX-tx),Math.max(tz-minZ,maxZ-tz)));
                for(int ring=0;ring<=rings;ring++){for(int dz=-ring;dz<=ring;dz++){for(int dx=-ring;dx<=ring;dx++){
                    if(Math.max(Math.abs(dx),Math.abs(dz))!=ring){continue;}int ix=tx+dx,iz=tz+dz;if(ix<minX||ix>maxX||iz<minZ||iz>maxZ){continue;}
                    var key=maps.key(dimension,ix,iz,lod,mode,altitude);var tile=maps.tile(key);if(tile==null){continue;}
                    var texture=maps.textures().texture(key,tile,(float)(zoom*(1<<lod)*maps.minecraft().getWindow().getGuiScale()));if(texture==null){continue;}
                    image(g,maps,texture,(ix*(double)span-cx)*zoom,(iz*(double)span-cz)*zoom,span*zoom,span*zoom,Theme.opacity(-1,opacity),clip,zoom<1);
                }}}
            }
            var player=maps.minecraft().player;
            if(player!=null&&dimension.equals(maps.dimension())){
                double px=(player.getX()-cx)*zoom,pz=(player.getZ()-cz)*zoom;
                if(!circle||Math.hypot(px,pz)<w/2-8){RenderUtil.drawRoundedRect(g,px-2,pz-2,4,4,2,Theme.opacity(-1,opacity));double yaw=Math.toRadians(player.getYRot());RenderUtil.line(g,px,pz,px-Math.sin(yaw)*8,pz+Math.cos(yaw)*8,1.5,Theme.opacity(-1,opacity));}
            }
            if(markers){for(var p:maps.waypoints().displayed()){
                if(!p.visible()||!p.dimension().equals(dimension)){continue;}double px=(p.x()-cx)*zoom,pz=(p.z()-cz)*zoom;if(Math.hypot(px,pz)>(circle?Math.min(w,h)/2-25:Math.hypot(w,h)/2+20)){continue;}
                RenderUtil.drawRoundedRect(g,px-2,pz-2,4,4,1,Theme.opacity(p.color(),opacity));g.pose().pushMatrix();g.pose().translate((float)(px+4),(float)pz);g.pose().rotate((float)-rotation);RenderUtil.text(g,maps.minecraft().font,RenderUtil.ellipsize(maps.minecraft().font,p.name(),circle?40:180),0,0,.65,Theme.opacity(p.color(),opacity));g.pose().popMatrix();
            }}
            if(compass){g.pose().pushMatrix();g.pose().translate(0,(float)(-Math.min(w,h)/2+12));g.pose().rotate((float)-rotation);RenderUtil.text(g,maps.minecraft().font,"N",0,0,.8,Theme.opacity(-1,opacity));g.pose().popMatrix();}
            g.pose().popMatrix();
        }finally{RenderUtil.scissorPop(g);}
    }
}
