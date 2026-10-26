package dev.riserecode.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.riserecode.event.CosmeticSubmitEvent;
import dev.riserecode.event.Render3DEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Cosmetic-only, depth-tested geometry through vanilla deferred submissions on every backend. */
public final class CosmeticsRenderer {
    private static final Identifier WHITE=CosmeticsManager.id("white.png");
    private static final int[] SIDES={-1,1};
    private static final float[] UNIT_BOX=boxMesh(), UNIT_BILLBOARD=billboardMesh(), HEART=heartMesh(24), REDUCED_HEART=fitHeart(heartMesh(12));
    private record Wing(int kind,int side,boolean ribs) { }
    private record Ring(float radius,float band,int segments) { }
    private static final java.util.Map<Wing,float[]> WINGS=new java.util.HashMap<>();
    private static final java.util.Map<Ring,float[]> RINGS=new java.util.HashMap<>();
    private CosmeticsRenderer(){ }
    public static int color(int color,double opacity){return (color&0xFFFFFF)|((int)((color>>>24)*opacity)<<24);}
    public static void submit(CosmeticsManager manager,CosmeticSubmitEvent e){
        var bridge=(CosmeticRenderState)e.state;var outfit=bridge.risecode$outfit();
        if(outfit==null||!outfit.visible()||e.state.isInvisible||e.state.isSpectator){return;}
        if(!bridge.risecode$preview()&&Minecraft.getInstance().options.getCameraType().isFirstPerson()){return;}
        float tick=e.state.ageInTicks/20F;
        for(var entry:outfit.items().entrySet()){
            var item=entry.getValue();var def=manager.library().find(item.id());if(def==null||def.category()!=entry.getKey()||item.opacity()==0){continue;}
            var asset=manager.request(item.id());if(asset==null){continue;}var poses=e.poses;poses.pushPose();
            try{
                if(entry.getKey()==CosmeticCategory.HEAD){e.model.head.translateAndRotate(poses);}
                else if(entry.getKey()==CosmeticCategory.WINGS||entry.getKey()==CosmeticCategory.TAIL){e.model.body.translateAndRotate(poses);}
                poses.translate(item.x(),-item.y(),item.z());float scale=(float)item.scale();poses.scale(scale,scale,scale);
                int primary=color(item.primary(),item.opacity()),secondary=color(item.secondary(),item.opacity());float time=tick*(float)item.speed();
                switch(entry.getKey()){
                    case CAPE -> { }
                    case WINGS -> wings(e,item,time,primary,secondary);
                    case HEAD -> head(e,item,primary,secondary);
                    case TAIL -> tail(e,item,time,primary,secondary,manager.motion().sample(e.state.ageInTicks%1));
                    case COMPANION -> {
                        if(asset.model()==null){break;}poses.translate(-.75F,-.25F-(float)Math.sin(time*2)*.06F*(float)item.motion(),.15F+e.state.walkAnimationSpeed*.15F*(float)item.motion());
                        poses.scale(1,-1,-1);poses.mulPose(new Quaternionf().rotateY(.3F));
                        for(var part:asset.model().sampleTick(e.state.ageInTicks,item.speed(),e.state.walkAnimationSpeed>.1F)){
                            Identifier texture=part.texture()>=0&&part.texture()<asset.textures().size()?asset.textures().get(part.texture()):WHITE;
                            geometry(poses,e.collector,texture,part.vertices(),part.indices(),primary,e.light,part.unlit(),true);
                        }
                    }
                    case AURA -> aura(e,item,time,primary,secondary);
                    case PARTICLES -> particles(e,item,time,primary,secondary);
                    case TRAIL -> {if(!dev.riserecode.performance.PerformancePolicy.frame().suppressTrails()&&bridge.risecode$preview()){for(int i=0;i<14;i++){float f=i/14F;billboard(e,WHITE,(float)Math.sin(time-f*3)*.6F,1.4F,(float)Math.cos(time-f*3)*.4F,.045F,color(primary,1-f),true);}}}
                }
            }finally{poses.popPose();}
        }
    }
    private static void wings(CosmeticSubmitEvent e,CosmeticLoadout.Item item,float t,int a,int b){
        boolean folded=e.state.isFallFlying||e.state.chestEquipment.is(Items.ELYTRA);
        float flap=folded?.12F:.35F+(float)Math.sin(t*3)*.22F*(float)item.motion()+Math.min(.3F,e.state.walkAnimationSpeed*.15F)*(float)item.motion();
        for(int side:SIDES){e.poses.pushPose();try{e.poses.translate(side*.15,.12,.2);e.poses.mulPose(new Quaternionf().rotateY(side*(flap+(folded?1.1F:0))));
            float span=item.id().contains("mechanical")?.7F:.85F;
            int kind=item.id().contains("butterfly")?0:item.id().contains("dragon")?1:item.id().contains("mechanical")?2:3;
            float[] wing=WINGS.computeIfAbsent(new Wing(kind,side,false),ignored->{var mesh=new Mesh();
            if(kind==0){mesh.quad(0,0,0,side*span,-.3F,.05F,side*span,.35F,.06F,side*.2F,.5F,0);mesh.quad(0,.2F,0,side*.65F,.55F,.04F,side*.35F,.95F,.04F,0,.65F,0);}
            else if(kind==1){
                float[] edge={side*span,-.3F,side*span*.9F,.08F,side*span*.73F,.04F,side*span*.65F,.4F,side*span*.44F,.31F,side*span*.35F,.7F,side*.15F,.8F};
                for(int i=0;i<edge.length-2;i+=2){mesh.quad(0,0,0,edge[i],edge[i+1],.01F,edge[i+2],edge[i+3],.01F,edge[i+2],edge[i+3],.01F);}
            }else{mesh.quad(0,0,0,side*span,-.3F,0,side*span*.8F,.5F,.03F,side*.15F,.8F,0);}
            return mesh.array();});
            geometry(e.poses,e.collector,WHITE,wing,null,a,e.light,false,false);
            if(item.id().contains("mechanical")){
                box(e,side*.32F-.02F,-.15F,.02F,.04F,.75F,.05F,b);
                for(int i=0;i<4;i++){float x=side*(.1F+i*.18F);box(e,x-.025F,-.1F+i*.07F,.02F,.05F,.55F,.04F,b);}
            }else{
                float[] ribMesh=WINGS.computeIfAbsent(new Wing(kind,side,true),ignored->{var ribs=new Mesh();
                for(float[] tip:new float[][]{{side*span,-.3F},{side*span*.65F,.4F},{side*span*.35F,.7F}}){
                    float dx=tip[0],dy=tip[1],length=(float)Math.sqrt(dx*dx+dy*dy),nx=-dy/length*.012F,ny=dx/length*.012F;
                    ribs.quad(nx,ny,-.012F,tip[0]+nx,tip[1]+ny,-.012F,tip[0]-nx,tip[1]-ny,-.012F,-nx,-ny,-.012F);
                }
                return ribs.array();});
                geometry(e.poses,e.collector,WHITE,ribMesh,null,b,e.light,false,false);
            }
            }finally{e.poses.popPose();}
        }
    }
    private static void head(CosmeticSubmitEvent e,CosmeticLoadout.Item item,int a,int b){
        if(item.id().endsWith("halo")){ring(e,0,-.68F,0,.4F,.035F,a,true);}
        else if(item.id().endsWith("crown")){for(int i=0;i<4;i++){double angle=i*Math.PI/2;float x=(float)Math.cos(angle)*.29F,z=(float)Math.sin(angle)*.29F;box(e,x-.055F,-.65F,z-.055F,.11F,.22F,.11F,b);}ring(e,0,-.46F,0,.34F,.06F,a,false);}
        else if(item.id().endsWith("horns")){for(int s:SIDES){for(int i=0;i<4;i++){float size=.12F-i*.022F;box(e,s*(.2F+i*.035F)-size/2,-.52F-i*.07F,-.02F,size,.1F,size,i%2==0?a:b);}}}
        else{for(int s:SIDES){var mesh=new Mesh();mesh.quad(s*.11F,-.48F,-.08F,s*.32F,-.48F,-.08F,s*.28F,-.82F,-.04F,s*.28F,-.82F,-.04F);geometry(e.poses,e.collector,WHITE,mesh.array(),null,a,e.light,false,false);box(e,s*.23F-.04F,-.65F,-.09F,.08F,.14F,.02F,b);}}
    }
    private static void tail(CosmeticSubmitEvent e,CosmeticLoadout.Item item,float time,int a,int b,float motion){
        e.poses.translate(0,.62,.18);float length=item.id().endsWith("fox")?.85F:.7F;
        e.poses.mulPose(new Quaternionf().rotateX(-.5F-motion*.35F*(float)item.motion()).rotateY((float)Math.sin(time*2)*.3F*(float)item.motion()));
        for(int i=0;i<7;i++){float size=item.id().endsWith("fox")?(.12F+(float)Math.sin(i*Math.PI/7)*.12F):.09F-i*.006F;
            box(e,-size/2,i*.055F,length*i/7,size,size,length/7+.015F,i>=5?b:a);
            if(item.id().endsWith("dragon")){box(e,-.025F,i*.055F-.045F,length*i/7,.05F,.07F,.035F,b);}}
    }
    private static void aura(CosmeticSubmitEvent e,CosmeticLoadout.Item item,float time,int a,int b){
        if(item.density()==0){return;}if(item.id().contains("magic-ring")){ring(e,0,1.48F,0,.65F,.025F,a,true);ring(e,0,1.46F,0,.77F,.014F,b,true);return;}
        int count=Math.min(144,(int)(12*item.density()*dev.riserecode.performance.PerformancePolicy.frame().particleDensity().multiplier()));for(int i=0;i<count;i++){float angle=time+i*6.283F/count;float radius=item.id().endsWith("wisps")?.65F:.5F;
            billboard(e,CosmeticsManager.id(item.id().endsWith("wisps")?"sprites/smoke_03.png":"sprites/light_01.png"),(float)Math.cos(angle)*radius,.7F+(float)Math.sin(angle*1.7F)*.6F,(float)Math.sin(angle)*radius,.07F,i%2==0?a:b,true);}
    }
    private static void particles(CosmeticSubmitEvent e,CosmeticLoadout.Item item,float time,int a,int b){
        int count=Math.min(288,(int)(24*item.density()*dev.riserecode.performance.PerformancePolicy.frame().particleDensity().multiplier()));for(int i=0;i<count;i++){float phase=(time*.35F+i*.618F)%1,angle=i*2.4F;
            float x=(float)Math.cos(angle)*(.35F+(i%3)*.12F),z=(float)Math.sin(angle)*(.35F+(i%3)*.12F),y=1.5F-phase*1.9F;
            int color=color(i%2==0?a:b,Math.sin(phase*Math.PI));
            if(item.id().endsWith("hearts")){e.poses.pushPose();try{e.poses.translate(x,y,z);geometry(e.poses,e.collector,WHITE,dev.riserecode.performance.PerformancePolicy.frame().reducedDetail()?REDUCED_HEART:HEART,null,color,15728880,true,false);}finally{e.poses.popPose();}}
            else{billboard(e,CosmeticsManager.id(item.id().endsWith("embers")?"sprites/flame_01.png":"sprites/star_01.png"),x,y,z,.045F,color,true);}
        }
    }
    private static float[] fitHeart(float[] reduced){
        for(int axis=0;axis<2;axis++){float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY,smallLo=lo,smallHi=hi;
            for(int i=axis;i<HEART.length;i+=8){lo=Math.min(lo,HEART[i]);hi=Math.max(hi,HEART[i]);}for(int i=axis;i<reduced.length;i+=8){smallLo=Math.min(smallLo,reduced[i]);smallHi=Math.max(smallHi,reduced[i]);}
            for(int i=axis;i<reduced.length;i+=8){reduced[i]=lo+(reduced[i]-smallLo)/(smallHi-smallLo)*(hi-lo);}
        }return reduced;
    }
    private static float heartY(double a){return(float)(13*Math.cos(a)-5*Math.cos(2*a)-2*Math.cos(3*a)-Math.cos(4*a));}
    private static float[] heartMesh(int segments){var mesh=new Mesh();for(int j=0;j<segments;j++){double u=j*Math.PI*2/segments,v=(j+1)*Math.PI*2/segments;mesh.quad(0,0,0,(float)(16*Math.pow(Math.sin(u),3))*.003F,-heartY(u)*.003F,0,(float)(16*Math.pow(Math.sin(v),3))*.003F,-heartY(v)*.003F,0,(float)(16*Math.pow(Math.sin(v),3))*.003F,-heartY(v)*.003F,0);}return mesh.array();}
    private static float[] billboardMesh(){var mesh=new Mesh();mesh.quad(-1,-1,0,1,-1,0,1,1,0,-1,1,0);return mesh.array();}
    private static void billboard(CosmeticSubmitEvent e,Identifier texture,float x,float y,float z,float size,int color,boolean emissive){
        e.poses.pushPose();try{e.poses.translate(x,y,z);e.poses.scale(size,size,size);geometry(e.poses,e.collector,texture,UNIT_BILLBOARD,null,color,e.light,emissive,false);}finally{e.poses.popPose();}
    }
    private static void ring(CosmeticSubmitEvent e,float x,float y,float z,float radius,float band,int color,boolean emissive){
        int segments=dev.riserecode.performance.PerformancePolicy.frame().reducedDetail()?24:48;
        float[] vertices=RINGS.computeIfAbsent(new Ring(radius,band,segments),ignored->{var mesh=new Mesh();for(int i=0;i<segments;i++){double a=i*Math.PI*2/segments,b=(i+1)*Math.PI*2/segments;mesh.quad((float)Math.cos(a)*radius,0,(float)Math.sin(a)*radius,(float)Math.cos(b)*radius,0,(float)Math.sin(b)*radius,(float)Math.cos(b)*(radius-band),0,(float)Math.sin(b)*(radius-band),(float)Math.cos(a)*(radius-band),0,(float)Math.sin(a)*(radius-band));}return mesh.array();});
        e.poses.pushPose();try{e.poses.translate(x,y,z);geometry(e.poses,e.collector,WHITE,vertices,null,color,e.light,emissive,false);}finally{e.poses.popPose();}
    }
    private static float[] boxMesh(){var m=new Mesh();m.quad(0,0,0,1,0,0,1,1,0,0,1,0);m.quad(1,0,1,0,0,1,0,1,1,1,1,1);m.quad(0,0,1,0,0,0,0,1,0,0,1,1);m.quad(1,0,0,1,0,1,1,1,1,1,1,0);m.quad(0,0,1,1,0,1,1,0,0,0,0,0);m.quad(0,1,0,1,1,0,1,1,1,0,1,1);return m.array();}
    private static void box(CosmeticSubmitEvent e,float x,float y,float z,float w,float h,float d,int color){
        e.poses.pushPose();try{e.poses.translate(x,y,z);e.poses.scale(w,h,d);geometry(e.poses,e.collector,WHITE,UNIT_BOX,null,color,e.light,false,false);}finally{e.poses.popPose();}
    }
    private static final class Mesh {
        private float[] values=new float[128];private int size;
        void quad(float ax,float ay,float az,float bx,float by,float bz,float cx,float cy,float cz,float dx,float dy,float dz){
            if(size+32>values.length){values=java.util.Arrays.copyOf(values,values.length*2);}
            vertex(ax,ay,az,0,0);vertex(bx,by,bz,1,0);vertex(cx,cy,cz,1,1);vertex(dx,dy,dz,0,1);
        }
        private void vertex(float x,float y,float z,float u,float v){values[size++]=x;values[size++]=y;values[size++]=z;values[size++]=0;values[size++]=1;values[size++]=0;values[size++]=u;values[size++]=v;}
        float[] array(){return java.util.Arrays.copyOf(values,size);}
    }
    public static void geometry(PoseStack poses,SubmitNodeCollector collector,Identifier texture,float[] vertices,int[] indices,int color,int light,boolean emissive,boolean triangles){
        var type=emissive?RenderTypes.entityTranslucentEmissive(texture):RenderTypes.entityTranslucent(texture);
        collector.submitCustomGeometry(poses,type,(pose,output)->{
            int count=indices==null?vertices.length/8:indices.length;
            for(int i=0;i<count;i++){
                vertex(output,pose,vertices,indices==null?i:indices[i],color,emissive?15728880:light);
                if(triangles&&i%3==2){vertex(output,pose,vertices,indices==null?i:indices[i],color,emissive?15728880:light);}
            }
        });
    }
    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,PoseStack.Pose pose,float[] v,int index,int color,int light){int i=index*8;out.addVertex(pose,v[i],v[i+1],v[i+2]).setColor(color).setUv(v[i+6],v[i+7]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,v[i+3],v[i+4],v[i+5]);}
    public static void trail(CosmeticsManager manager,Render3DEvent event){
        var outfit=manager.outfit();var mc=Minecraft.getInstance();var item=outfit.items().get(CosmeticCategory.TRAIL);
        if(dev.riserecode.performance.PerformancePolicy.frame().suppressTrails()||!outfit.visible()||item==null||item.density()==0||mc.player==null||mc.player.isInvisible()||mc.player.isSpectator()||mc.options.getCameraType().isFirstPerson()){return;}
        var points=manager.motion().trail();if(points.size()<2){return;}
        // Capture world points and settings now, never a player or mutable history in deferred drawing.
        event.submit(context->{Vec3 camera=context.levelState().cameraRenderState.pos;var poses=context.poseStack();
            for(int i=0;i<Math.min(points.size()-1,Math.max(2,(int)(12*item.density())));i++){
                float fade=1-i/(float)points.size();var offset=new Vec3(item.x(),item.y(),item.z());var a=points.get(i).add(offset).subtract(camera);var b=points.get(i+1).add(offset).subtract(camera);var mesh=new Mesh();float size=(float)(.08*item.scale());
                if(item.id().endsWith("ribbon")){mesh.quad((float)a.x,(float)a.y+.1F,(float)a.z,(float)b.x,(float)b.y+.1F,(float)b.z,(float)b.x,(float)b.y+.1F+size,(float)b.z,(float)a.x,(float)a.y+.1F+size,(float)a.z);}
                else{mesh.quad((float)a.x-size,(float)a.y+.1F-size,(float)a.z,(float)a.x+size,(float)a.y+.1F-size,(float)a.z,(float)a.x+size,(float)a.y+.1F+size,(float)a.z,(float)a.x-size,(float)a.y+.1F+size,(float)a.z);}
                Identifier texture=item.id().endsWith("ribbon")?WHITE:CosmeticsManager.id(item.id().endsWith("stars")?"sprites/star_01.png":"sprites/spark_01.png");
                geometry(poses,context.submitNodeCollector(),texture,mesh.array(),null,color(i%2==0?item.primary():item.secondary(),item.opacity()*fade),15728880,true,false);
            }
        });
    }
}
