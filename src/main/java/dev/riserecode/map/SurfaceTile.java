package dev.riserecode.map;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Immutable 16x16 client-observed surface, with retained palette/tint/height for recoloring. */
public record SurfaceTile(int x, int z, List<String> palette, short[] blocks, short[] heights, int[] tints, byte[] water, int[] colors, int[] detail, short[] edges) {
    public SurfaceTile(int x,int z,List<String> palette,short[] blocks,short[] heights,int[] tints,byte[] water,int[] colors){this(x,z,palette,blocks,heights,tints,water,colors,new int[0],new short[0]);}
    public SurfaceTile {
        palette = List.copyOf(palette); blocks=blocks.clone();heights=heights.clone();tints=tints.clone();water=water.clone();colors=colors.clone();detail=detail.clone();edges=edges.clone();
        if (blocks.length!=256||heights.length!=256||tints.length!=256||water.length!=256||colors.length!=256||palette.size()>256) { throw new IllegalArgumentException("Tile size"); }
        if(detail.length!=0&&detail.length!=4096||edges.length!=0&&edges.length!=32){throw new IllegalArgumentException("Tile detail size");}
        for(short index:blocks){if(index<0||index>=palette.size()){throw new IllegalArgumentException("Palette index");}}
    }
    @Override public short[] blocks(){return blocks.clone();}
    @Override public short[] heights(){return heights.clone();}
    @Override public int[] tints(){return tints.clone();}
    @Override public byte[] water(){return water.clone();}
    @Override public int[] colors(){return colors.clone();}
    @Override public int[] detail(){return detail.clone();}
    @Override public short[] edges(){return edges.clone();}
    public int detail(int index){return detail.length==0?0:detail[index];}
    public boolean detailed(){return detail.length!=0;}
    public long bytes(){return 4096L+detail.length*4L+edges.length*2L+palette.stream().mapToLong(s->40L+s.length()*2L).sum();}
    public int slope(int index){
        int north=index<16?(edges.length==32?edges[index]:heights[index]):heights[index-16];
        int west=(index&15)==0?(edges.length==32?edges[16+(index>>4)]:heights[index]):heights[index-1];
        return Math.clamp((heights[index]-north)*2+heights[index]-west,-16,16);
    }
    public int block(int index){return blocks[index];}
    public int height(int index){return heights[index];}
    public int tint(int index){return tints[index];}
    public int water(int index){return Byte.toUnsignedInt(water[index]);}
    public static int shade(int rgb,int tint,int heightDelta,int waterDepth){
        double shade=Math.clamp(1.0+heightDelta*0.045-waterDepth*0.018,0.40,1.24);
        int r=(int)((rgb>>16&255)*(tint>>16&255)/255.0*shade),g=(int)((rgb>>8&255)*(tint>>8&255)/255.0*shade),b=(int)((rgb&255)*(tint&255)/255.0*shade);
        return 0xFF000000|Math.clamp(r,0,255)<<16|Math.clamp(g,0,255)<<8|Math.clamp(b,0,255);
    }
    /** Worker-side derivation from an immutable palette/height/tint capture. */
    public SurfaceTile shaded(){
        int[] result=new int[256];for(int i=0;i<256;i++){if((colors[i]>>>24)==0){continue;}result[i]=shade(colors[i],tints[i],slope(i),Byte.toUnsignedInt(water[i]));}
        return new SurfaceTile(x,z,palette,blocks,heights,tints,water,result,detail,edges);
    }
    public void write(Path path) throws IOException {
        Files.createDirectories(path.getParent());Path temporary=Files.createTempFile(path.getParent(),"surface-",".tmp");
        try {
            try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))){
                out.writeInt(0x52534D32);out.writeInt(x);out.writeInt(z);out.writeInt(palette.size());for(String s:palette){out.writeUTF(s);}
                for(int i=0;i<256;i++){out.writeShort(blocks[i]);out.writeShort(heights[i]);out.writeInt(tints[i]);out.writeByte(water[i]);out.writeInt(colors[i]);}
                out.writeInt(detail.length);for(int value:detail){out.writeInt(value);}
                out.writeInt(edges.length);for(short value:edges){out.writeShort(value);}
            }
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
    public static SurfaceTile read(Path path) throws IOException {
        if(Files.size(path)>1_000_000){throw new IOException("Oversized tile");}
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))){
            int version=in.readInt();if(version!=0x52534D31&&version!=0x52534D32){throw new IOException("Unknown tile version");}int x=in.readInt(),z=in.readInt(),count=in.readInt();if(count<1||count>256){throw new IOException("Invalid palette");}
            List<String> palette=new ArrayList<>();for(int i=0;i<count;i++){String s=in.readUTF();if(s.length()>4096){throw new IOException("Invalid state");}palette.add(s);}
            short[] blocks=new short[256],heights=new short[256];int[] tints=new int[256],colors=new int[256];byte[] water=new byte[256];
            for(int i=0;i<256;i++){blocks[i]=in.readShort();heights[i]=in.readShort();tints[i]=in.readInt();water[i]=in.readByte();colors[i]=in.readInt();}
            int[] detail=new int[0];short[] edges=new short[0];
            if(version==0x52534D32){int size=in.readInt();if(size!=0&&size!=4096){throw new IOException("Invalid detail size");}detail=new int[size];for(int i=0;i<size;i++){detail[i]=in.readInt();}
                size=in.readInt();if(size!=0&&size!=32){throw new IOException("Invalid height edge size");}edges=new short[size];for(int i=0;i<size;i++){edges[i]=in.readShort();}}
            return new SurfaceTile(x,z,palette,blocks,heights,tints,water,colors,detail,edges);
        }catch(IllegalArgumentException e){throw new IOException("Invalid tile",e);}
    }
}
