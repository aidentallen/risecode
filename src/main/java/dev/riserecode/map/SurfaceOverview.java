package dev.riserecode.map;

import java.util.*;

/** Four-by-four density reduction; unknown cells remain transparent rather than inventing explored terrain. */
public final class SurfaceOverview {
    private SurfaceOverview(){ }
    /** The parent image with the child's quarter replaced; empty child pixels keep what was known before. */
    private static int[] image(SurfaceTile existing,SurfaceTile child,boolean overviewChild){
        int[] image=existing!=null&&existing.detailed()?existing.detail():new int[4096];
        int[] source=overviewChild&&child.detailed()?shrink(child.detail()):child.colors();
        int offsetX=Math.floorMod(child.x(),4)*16,offsetZ=Math.floorMod(child.z(),4)*16;
        for(int z=0;z<16;z++){for(int x=0;x<16;x++){int color=source[z*16+x];if((color>>>24)!=0){image[(offsetZ+z)*64+offsetX+x]=color;}}}
        return image;
    }
    private static int[] shrink(int[] image){
        int[] result=new int[256];
        for(int z=0;z<16;z++){for(int x=0;x<16;x++){
            long red=0,green=0,blue=0;int count=0;
            for(int dz=0;dz<4;dz++){for(int dx=0;dx<4;dx++){int c=image[(z*4+dz)*64+x*4+dx];if((c>>>24)==0){continue;}red+=c>>16&255;green+=c>>8&255;blue+=c&255;count++;}}
            if(count>0){result[z*16+x]=0xFF000000|(int)(red/count)<<16|(int)(green/count)<<8|(int)(blue/count);}
        }}
        return result;
    }
    public static SurfaceTile merge(SurfaceTile existing,SurfaceTile child,int parentX,int parentZ){return merge(existing,child,parentX,parentZ,false);}
    /** @param overviewChild the child is itself an overview, so its 64x64 image holds final colors rather than block textures */
    public static SurfaceTile merge(SurfaceTile existing,SurfaceTile child,int parentX,int parentZ,boolean overviewChild){
        String[] states=new String[256];short[] blocks=new short[256],heights=new short[256];int[] tints=new int[256],colors=new int[256];byte[] water=new byte[256];
        Arrays.fill(states,"{\"Name\":\"minecraft:air\"}");Arrays.fill(tints,0xFFFFFF);
        if(existing!=null){for(int i=0;i<256;i++){states[i]=existing.palette().get(existing.block(i));heights[i]=(short)existing.height(i);tints[i]=existing.tint(i);water[i]=(byte)existing.water(i);}colors=existing.colors();}
        int offsetX=Math.floorMod(child.x(),4)*4,offsetZ=Math.floorMod(child.z(),4)*4;int[] source=child.colors();
        for(int z=0;z<4;z++){for(int x=0;x<4;x++){
            long red=0,green=0,blue=0,tr=0,tg=0,tb=0,high=0,depth=0;int count=0;Map<Integer,Integer> counts=new LinkedHashMap<>();
            for(int dz=0;dz<4;dz++){for(int dx=0;dx<4;dx++){int i=(z*4+dz)*16+x*4+dx,c=source[i];if((c>>>24)==0){continue;}red+=c>>16&255;green+=c>>8&255;blue+=c&255;int tint=child.tint(i);tr+=tint>>16&255;tg+=tint>>8&255;tb+=tint&255;high+=child.height(i);depth+=child.water(i);counts.merge(child.block(i),1,Integer::sum);count++;}}
            if(count==0){continue;}int index=(offsetZ+z)*16+offsetX+x;int dominant=counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
            states[index]=child.palette().get(dominant);heights[index]=(short)(high/count);water[index]=(byte)(depth/count);tints[index]=(int)(tr/count)<<16|(int)(tg/count)<<8|(int)(tb/count);colors[index]=0xFF000000|(int)(red/count)<<16|(int)(green/count)<<8|(int)(blue/count);
        }}
        List<String> palette=new ArrayList<>();for(int i=0;i<256;i++){int index=palette.indexOf(states[i]);if(index<0){index=palette.size();palette.add(states[i]);}blocks[i]=(short)index;}
        return new SurfaceTile(parentX,parentZ,palette,blocks,heights,tints,water,colors,image(existing,child,overviewChild),new short[0]);
    }
}
