package dev.riserecode.ui.panorama;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Pure seeded voxel description. No level, server, registry or renderer access. */
public final class ForestScene {
    public enum Kind { GRASS, DIRT, MOSS, ROCK, LOG, BIRCH, LEAVES, BIRCH_LEAVES, SPRUCE_LOG, SPRUCE_LEAVES, FERN, GRASS_PLANT, FLOWER, BLUE_FLOWER, VINE, WATER, LILY }
    public record Block(int x,int y,int z,Kind kind) { }
    public record Description(List<Block> blocks,Map<Long,Kind> occupancy) { }
    public static long key(int x,int y,int z){return ((long)(x+512)<<40)|((long)(z+512)<<20)|(y+512L);}
    public static double hash(int x,int z,int salt){long a=x*341873128712L+z*132897987541L+salt*42317861L;a=(a^(a>>>27))*0x3c79ac492ba7b653L;a=(a^(a>>>33))*0x1c69b3f74ac4ae35L;return (a>>>11)*0x1.0p-53;}
    public static boolean lake(double x,double z){double r=x*x/(18*18)+(z+10)*(z+10)/(26*26);return r<1+.065*Math.sin(x*.6+z*.24);}
    public static int ground(int x,int z){
        if(lake(x+.5,z+.5))return -2;
        double edge=Math.max(0,Math.hypot(x*.7,(z+10)*.5)-18);
        return (int)Math.floor(.8+Math.sin(x*.18)*.7+Math.cos(z*.2)*.7+edge*.19+Math.max(0,-z-40)*.18);
    }
    public static Description create(double trees,double plants,boolean low,BooleanSupplier cancelled){
        LinkedHashMap<Long,Block> blocks=new LinkedHashMap<>();
        for(int z=-76;z<=33;z++){
            if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            for(int x=-58;x<=58;x++){
                int y=ground(x,z);put(blocks,x,y,z,lake(x+.5,z+.5)?Kind.DIRT:hash(x,z,1)<.2?Kind.MOSS:Kind.GRASS);
                // Close vertical terrain steps without retaining invisible solid interiors.
                int floor=Math.min(Math.min(ground(x-1,z),ground(x+1,z)),Math.min(ground(x,z-1),ground(x,z+1)));
                for(int a=floor;a<y;a++)put(blocks,x,a,z,Kind.DIRT);
                if(lake(x+.5,z+.5)){put(blocks,x,0,z,Kind.WATER);if(hash(x,z,30)<.012&&Math.abs(x)>8)put(blocks,x,1,z,Kind.LILY);continue;}
                if(hash(x,z,2)<plants*.24&&(!low||z>-35)&&z<24){Kind k=hash(x,z,3)<.68?Kind.FERN:hash(x,z,4)<.55?Kind.GRASS_PLANT:hash(x,z,5)<.5?Kind.FLOWER:Kind.BLUE_FLOWER;put(blocks,x,y+1,z,k);}
                if(hash(x,z,6)<.004&&Math.abs(x)>8){put(blocks,x,y+1,z,Kind.ROCK);if(hash(x,z,7)<.5)put(blocks,x+1,y+1,z,Kind.MOSS);}
            }
        }
        // Principal framing trees are independent of both density sliders.

        for(int z=-69;z<24;z+=5)for(int x=-51;x<53;x+=5){
            if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException();
            int tx=x+(int)(hash(x,z,10)*4),tz=z+(int)(hash(x,z,11)*4);
            if(lake(tx,tz)||lake(tx+3,tz)||lake(tx-3,tz)||Math.abs(tx)<10&&tz>2||hash(x,z,12)>trees*.72)continue;
            if(low&&tz<-38&&hash(x,z,14)<.45)continue;
            tree(blocks,tx,tz,7+(int)(hash(x,z,13)*7),(int)(hash(x,z,14)*3));
        }
        tree(blocks,-13,12,12,0);tree(blocks,14,9,14,1);tree(blocks,-22,-7,15,0);tree(blocks,23,-13,16,2);
        var kinds=new HashMap<Long,Kind>();blocks.forEach((k,b)->kinds.put(k,b.kind));
        return new Description(List.copyOf(blocks.values()),Map.copyOf(kinds));
    }
    private static void put(Map<Long,Block> out,int x,int y,int z,Kind kind){out.put(key(x,y,z),new Block(x,y,z,kind));}
    private static void tree(Map<Long,Block> out,int x,int z,int h,int type){
        int base=ground(x,z)+1;Kind log=type==1?Kind.BIRCH:type==2?Kind.SPRUCE_LOG:Kind.LOG,leaf=type==1?Kind.BIRCH_LEAVES:type==2?Kind.SPRUCE_LEAVES:Kind.LEAVES;
        for(int y=0;y<h;y++)put(out,x,base+y,z,log);
        for(int y=type==2?3:h-5;y<h+2;y++){
            int r=type==2?Math.max(1,(h-y)/3+1):y>=h?2:3;
            for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++)if(Math.abs(dx)+Math.abs(dz)<=r*1.6&&!(dx==0&&dz==0&&y<h)&&hash(x+dx,z+dz,y)<.95)put(out,x+dx,base+y,z+dz,leaf);
        }
        if(type==0){for(int i=0;i<4;i++)for(int y=0;y<2+i%2;y++)put(out,x-3+i,base+h-5-y,z+3,Kind.VINE);}
    }
    public static boolean solid(Kind k){return k==Kind.GRASS||k==Kind.DIRT||k==Kind.MOSS||k==Kind.ROCK||k==Kind.LOG||k==Kind.BIRCH||k==Kind.SPRUCE_LOG;}
    private ForestScene(){}
}
