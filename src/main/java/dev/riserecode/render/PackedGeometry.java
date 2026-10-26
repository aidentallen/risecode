package dev.riserecode.render;

import dev.riserecode.event.Render3DEvent;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Immutable packed world primitives. Partition indices preserve insertion order within each depth pass. */
public final class PackedGeometry {
    public static final PackedGeometry EMPTY=new Builder().snapshot();
    private final double[] lines,quads;
    private final int[] lineColors,quadColors;
    private final float[] widths;
    private final boolean[] lineDepth,quadDepth;
    private final int[][] linePass,quadPass;
    private List<Render3DEvent.Line> lineView;
    private List<Render3DEvent.Quad> quadView;
    private PackedGeometry(Builder b){
        lines=Arrays.copyOf(b.lines,b.lineCount*6);quads=Arrays.copyOf(b.quads,b.quadCount*12);
        lineColors=Arrays.copyOf(b.lineColors,b.lineCount*2);quadColors=Arrays.copyOf(b.quadColors,b.quadCount*4);
        widths=Arrays.copyOf(b.widths,b.lineCount);lineDepth=Arrays.copyOf(b.lineDepth,b.lineCount);quadDepth=Arrays.copyOf(b.quadDepth,b.quadCount);
        linePass=partition(lineDepth);quadPass=partition(quadDepth);
    }
    private static int[][] partition(boolean[] flags){int n=0;for(boolean flag:flags){if(flag){n++;}}int[][] result={new int[flags.length-n],new int[n]};int a=0,b=0;for(int i=0;i<flags.length;i++){if(flags[i]){result[1][b++]=i;}else{result[0][a++]=i;}}return result;}
    public int lineCount(){return widths.length;}
    public int quadCount(){return quadDepth.length;}
    public int lineCount(boolean visible){return linePass[visible?1:0].length;}
    public int quadCount(boolean visible){return quadPass[visible?1:0].length;}
    public int lineIndex(boolean visible,int i){return linePass[visible?1:0][i];}
    public int quadIndex(boolean visible,int i){return quadPass[visible?1:0][i];}
    public double line(int index,int component){return lines[index*6+component];}
    public double quad(int index,int component){return quads[index*12+component];}
    public int lineColor(int index,int end){return lineColors[index*2+end];}
    public int quadColor(int index,int corner){return quadColors[index*4+corner];}
    public float width(int index){return widths[index];}
    private static Vec3 point(double[] data,int offset){return new Vec3(data[offset],data[offset+1],data[offset+2]);}
    public List<Render3DEvent.Line> lines(){if(lineView==null){var values=new ArrayList<Render3DEvent.Line>(lineCount());for(int i=0;i<lineCount();i++){values.add(new Render3DEvent.Line(point(lines,i*6),point(lines,i*6+3),lineColor(i,0),widths[i],lineColor(i,1),lineDepth[i]));}lineView=List.copyOf(values);}return lineView;}
    public List<Render3DEvent.Quad> quads(){if(quadView==null){var values=new ArrayList<Render3DEvent.Quad>(quadCount());for(int i=0;i<quadCount();i++){values.add(new Render3DEvent.Quad(point(quads,i*12),point(quads,i*12+3),point(quads,i*12+6),point(quads,i*12+9),quadColor(i,0),quadColor(i,1),quadColor(i,2),quadColor(i,3),quadDepth[i]));}quadView=List.copyOf(values);}return quadView;}
    public static final class Builder {
        private double[] lines=new double[0],quads=new double[0];
        private int[] lineColors=new int[0],quadColors=new int[0];
        private float[] widths=new float[0];
        private boolean[] lineDepth=new boolean[0],quadDepth=new boolean[0];
        private int lineCount,quadCount;
        private static boolean finite(Vec3 p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
        public void line(double ax,double ay,double az,double bx,double by,double bz,int a,int b,float width,boolean depth){
            if(!Double.isFinite(ax)||!Double.isFinite(ay)||!Double.isFinite(az)||!Double.isFinite(bx)||!Double.isFinite(by)||!Double.isFinite(bz)||!Float.isFinite(width)||width<=0||width>32){throw new IllegalArgumentException("Invalid line");}
            if(lineCount==widths.length){int n=Math.max(32,lineCount*2);lines=Arrays.copyOf(lines,n*6);lineColors=Arrays.copyOf(lineColors,n*2);widths=Arrays.copyOf(widths,n);lineDepth=Arrays.copyOf(lineDepth,n);}
            int i=lineCount*6;lines[i]=ax;lines[i+1]=ay;lines[i+2]=az;lines[i+3]=bx;lines[i+4]=by;lines[i+5]=bz;lineColors[lineCount*2]=a;lineColors[lineCount*2+1]=b;widths[lineCount]=width;lineDepth[lineCount++]=depth;
        }
        public void quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int ca,int cb,int cc,int cd,boolean depth){
            if(!finite(a)||!finite(b)||!finite(c)||!finite(d)){throw new IllegalArgumentException("Nonfinite geometry");}
            if(quadCount==quadDepth.length){int n=Math.max(16,quadCount*2);quads=Arrays.copyOf(quads,n*12);quadColors=Arrays.copyOf(quadColors,n*4);quadDepth=Arrays.copyOf(quadDepth,n);}
            int i=quadCount*12;quads[i]=a.x;quads[i+1]=a.y;quads[i+2]=a.z;quads[i+3]=b.x;quads[i+4]=b.y;quads[i+5]=b.z;quads[i+6]=c.x;quads[i+7]=c.y;quads[i+8]=c.z;quads[i+9]=d.x;quads[i+10]=d.y;quads[i+11]=d.z;
            i=quadCount*4;quadColors[i]=ca;quadColors[i+1]=cb;quadColors[i+2]=cc;quadColors[i+3]=cd;quadDepth[quadCount++]=depth;
        }
        public PackedGeometry snapshot(){return new PackedGeometry(this);}
    }
}
