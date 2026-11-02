package dev.riserecode.scanner;

import java.util.*;

/** Original, deterministic heuristics over packet-owned cells. No live world access. */
public final class ChunkHeuristics {
    public enum Mode { CLASSIC, ANBS_PLUS, ACTIVITY_CLUES }
    public enum Method { BYPASS, LIGHT, ANBS_PLUS_SCAN }
    public static final int CONTAINER=1, MACHINERY=2, REDSTONE=4, GEODE=8, GROWTH=16,
        AIR=32, TRANSPARENT=64, BUDDING=128, SHELL=256, CLUSTER=512,
        COBBLE=1024, MOSSY=2048, SPAWNER=4096, RAIL=8192, WEB=16384,
        SCULK=32768, CITY=65536, TRIAL=131072, TUFF=262144, ENTITY=524288,
        SHULKER=1048576, CHEST=2097152, HOPPER=4194304, CROP=8388608,
        TALL=1<<24, STEM=1<<25, FRUIT=1<<26, DERIVED_GROWTH=1<<27,
        AMETHYST=1<<28, CLOCK=1<<29, CLOCK_PART=1<<30, BUILT=1<<31;
    public record Options(Mode mode, Method method, int threshold, int classicThreshold, int minCells,
                          boolean filter, boolean containers, boolean shulkers, boolean chests, boolean hoppers,
                          boolean machinery, boolean redstone, boolean geodes, boolean growth) { }
    public record Target(int index, int category) { }
    public record Finding(int[] counts, int clusters, int lightClues, int matureAmethyst, double score, int confidence,
                          int dominant, double x, double y, double z, List<Target> targets) {
        public Finding { counts=counts.clone(); targets=List.copyOf(targets); }
        @Override public int[] counts() { return counts.clone(); }
        public int count(int category) { return counts[category]; }
    }
    private ChunkHeuristics() { }
    public static int index(int x,int y,int z) { return (y<<8)|(z<<4)|x; }
    private static int neighbor(int i,int d,int height) {
        return switch(d) {case 0 -> (i&15)>0?i-1:-1;case 1 -> (i&15)<15?i+1:-1;
            case 2 -> ((i>>4)&15)>0?i-16:-1;case 3 -> ((i>>4)&15)<15?i+16:-1;
            case 4 -> (i>>8)>0?i-256:-1;default -> (i>>8)+1<height?i+256:-1;};
    }
    public static Finding evaluate(int[] cells, int minY, byte[][] blockLight, byte[][] skyLight, Options o) {
        int height=cells.length/256;
        boolean[] acceptedGeodes=new boolean[cells.length],visited=new boolean[cells.length], suppressed=new boolean[cells.length];
        int clusters=0,anomalies=0;
        BitSet structureCells=new BitSet();
        if(o.geodes()) {
            int[] queue=new int[cells.length];
            for(int root=0;root<cells.length;root++) {
                if((cells[root]&GEODE)==0||visited[root])continue;
                int head=0,tail=1,amethyst=0;queue[0]=root;visited[root]=true;
                while(head<tail){if((head&4095)==0&&Thread.currentThread().isInterrupted())return null;int i=queue[head++];if((cells[i]&(BUDDING|(AMETHYST)))!=0)amethyst++;for(int dir=0;dir<6;dir++){int n=neighbor(i,dir,height);if(n>=0&&!visited[n]&&(cells[n]&GEODE)!=0){visited[n]=true;queue[tail++]=n;}}}
                if(tail>=o.minCells()&&amethyst>0){clusters++;anomalies+=geodeAnomalies(cells,queue,tail,height);for(int j=0;j<tail;j++)acceptedGeodes[queue[j]]=true;}
            }
        }
        // Suppress only the feature's local neighborhood, never a whole chunk containing a base.
        if(o.filter())for(int i=0;i<cells.length;i++) {
            int f=cells[i]; if(suppressed[i]||(f&(SPAWNER|RAIL|CITY|TRIAL))==0)continue;
            int x=i&15,z=(i>>4)&15,y=i>>8,combo=0;
            int featureKind=(f&SPAWNER)!=0?0:(f&RAIL)!=0?1:(f&CITY)!=0?2:3;
            int anchor=(((y>>2)*16+(z>>2)*4+(x>>2))*4)+featureKind;
            if(structureCells.get(anchor))continue;structureCells.set(anchor);
            for(int dy=-4;dy<=4;dy++)for(int dz=-4;dz<=4;dz++)for(int dx=-4;dx<=4;dx++) {
                int xx=x+dx,yy=y+dy,zz=z+dz;if(xx>=0&&xx<16&&zz>=0&&zz<16&&yy>=0&&yy<height)combo|=cells[index(xx,yy,zz)];
            }
            boolean feature=(f&SPAWNER)!=0&&(combo&(COBBLE|MOSSY))==(COBBLE|MOSSY)
                ||(f&RAIL)!=0&&(combo&WEB)!=0 ||(f&CITY)!=0&&(combo&SCULK)!=0 ||(f&TRIAL)!=0&&(combo&TUFF)!=0;
            if(feature)for(int dy=-5;dy<=5;dy++)for(int dz=-5;dz<=5;dz++)for(int dx=-5;dx<=5;dx++){
                int xx=x+dx,yy=y+dy,zz=z+dz;if(xx>=0&&xx<16&&zz>=0&&zz<16&&yy>=0&&yy<height)suppressed[index(xx,yy,zz)]=true;
            }
        }
        int[] counts=new int[5];int mature=0,entities=0,light=0,amethystGrowth=0;
        double sx=0,sy=0,sz=0;int total=0;var targets=new ArrayList<Target>();
        Map<Integer,Integer> density=new HashMap<>();Set<Integer> rows=new HashSet<>();
        for(int i=0;i<cells.length;i++) {
            if((i&4095)==0&&Thread.currentThread().isInterrupted())return null;
            int f=cells[i],category=-1,x=i&15,z=(i>>4)&15,y=i>>8;
            if(!suppressed[i]) {
                if(o.containers()&&(f&CONTAINER)!=0&&((f&SHULKER)==0||o.shulkers())&&((f&CHEST)==0||o.chests())&&((f&HOPPER)==0||o.hoppers()))category=0;
                else if(o.machinery()&&(f&(MACHINERY|(CLOCK)))!=0)category=1;
                else if(o.redstone()&&(f&REDSTONE)!=0)category=2;
                else if(o.geodes()&&(f&GEODE)!=0) {
                    boolean exposed=false;
                    for(int dir=0;dir<6;dir++){int n=neighbor(i,dir,height);if(n>=0){if((cells[n]&(AIR|TRANSPARENT))!=0)exposed=true;}}
                    if(o.method()==Method.ANBS_PLUS_SCAN&&acceptedGeodes[i]||o.method()==Method.BYPASS&&exposed&&(acceptedGeodes[i]||(f&(BUDDING|(AMETHYST)))!=0))category=3;
                }
                if(o.growth()&&(f&GROWTH)!=0){counts[4]++;mature++;if((f&CLUSTER)!=0)amethystGrowth++;if(category<0)category=4;}
                if((f&ENTITY)!=0&&(category>=0||o.containers()&&(f&CONTAINER)==0))entities++;
            }
            if(o.geodes()&&o.method()==Method.LIGHT&&minY+y<0&&nibble(blockLight,y,x,z)==5&&nibble(skyLight,y,x,z)==0) {
                boolean peak=true;for(int dir=0;dir<6;dir++){int n=neighbor(i,dir,height);if(n>=0&&nibble(blockLight,n>>8,n&15,(n>>4)&15)>5){peak=false;break;}}
                if(peak){light++;if(category<0)category=3;}
            }
            if(category<0)continue;
            if(category!=4)counts[category]++;
            if(category==0&&o.machinery()&&(f&(CLOCK))!=0)counts[1]++;
            sx+=x+.5;sy+=minY+y+.5;sz+=z+.5;total++;
            if(targets.size()<128)targets.add(new Target(i,category));
            if(category!=3){density.merge((x>>2)|((z>>2)<<2)|((y>>2)<<4),1,Integer::sum);rows.add((y<<4)|z);}
        }
        double score=counts[0]*3.+counts[1]*2.5+counts[2]*2.+anomalies*1.5+counts[4];
        int dense=density.values().stream().mapToInt(v->v>=3?v:0).sum();
        boolean pattern=dense>=3||counts[0]>=2&&rows.size()<=Math.max(2,counts[0]/2);
        boolean flagged=switch(o.mode()) {
            case CLASSIC -> mature>=o.classicThreshold()||entities>=o.classicThreshold();
            case ACTIVITY_CLUES -> score>=o.threshold();
            case ANBS_PLUS -> pattern&&score*1.5>=o.threshold()||clusters>0&&counts[3]>=o.minCells();
        };
        // A light maximum is a clue, not proof of an amethyst emitter. Never scores as containers.
        flagged|=light>0 || o.geodes()&&(o.method()==Method.BYPASS&&counts[3]>0||o.method()==Method.ANBS_PLUS_SCAN&&clusters>0&&counts[3]>=o.minCells());
        if(!flagged||total==0)return null;
        int dominant=0;double best=-1;double[] weights={3,2.5,2,1.5,1};
        for(int c=0;c<5;c++)if(counts[c]>0&&weights[c]>best){dominant=c;best=weights[c];}
        int confidence=(int)Math.min(100,Math.round(score));if(score==0)confidence=Math.min(45,15+light);
        return new Finding(counts,clusters,light,amethystGrowth,score,confidence,dominant,sx/total,sy/total,sz/total,targets);
    }
    /** Shell openings, artificial neighbors of budding blocks, and sampled hollow volume.
     * Natural geodes can match these too; each feature contributes at most one 1.5-point clue. */
    private static int geodeAnomalies(int[] cells,int[] cluster,int count,int height){
        int minX=15,maxX=0,minZ=15,maxZ=0,minY=height,maxY=0;boolean broken=false,embedded=false;
        for(int j=0;j<count;j++){
            int i=cluster[j],x=i&15,z=(i>>4)&15,y=i>>8;
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            int air=0;for(int dir=0;dir<6;dir++){int n=neighbor(i,dir,height);if(n<0)continue;
                if((cells[n]&AIR)!=0)air++;
                if((cells[i]&BUDDING)!=0&&(cells[n]&(BUILT|CONTAINER|MACHINERY|REDSTONE))!=0)embedded=true;
            }
            if((cells[i]&SHELL)!=0&&air>=3)broken=true;
        }
        int volume=Math.max(0,maxX-minX-1)*Math.max(0,maxY-minY-1)*Math.max(0,maxZ-minZ-1);
        int stride=Math.max(1,(int)Math.ceil(Math.cbrt(volume/4096.0))),air=0,samples=0;
        if(volume>=8)for(int y=minY+1;y<maxY;y+=stride)for(int z=minZ+1;z<maxZ;z+=stride)for(int x=minX+1;x<maxX;x+=stride){samples++;if((cells[index(x,y,z)]&AIR)!=0)air++;}
        boolean hollow=samples>=8&&air/(double)samples>=.8;
        return (broken?1:0)+(embedded?1:0)+(hollow?1:0);
    }
    private static int nibble(byte[][] layers,int y,int x,int z) {
        int section=(y>>4)+1;if(layers==null||section>=layers.length||layers[section]==null)return -1;
        byte[] data=layers[section];int i=((y&15)<<8)|(z<<4)|x;
        return (data[i>>1]>>((i&1)*4))&15;
    }
}
