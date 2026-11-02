package dev.riserecode;

import dev.riserecode.scanner.ChunkHeuristics;
import java.util.*;
import static dev.riserecode.scanner.ChunkHeuristics.*;

/** Deterministic algorithm fixtures: thresholds, topology, noise, structure locality and sparse light. */
public final class BaseFinderChecks {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static Options options(Mode mode,Method method,int threshold,int minimum,boolean filter){return new Options(mode,method,threshold,3,minimum,filter,true,true,true,true,true,true,true,true);}
    private static Finding scan(int[] cells,Options o){return evaluate(cells,-16,null,null,o);}
    private static int[] empty(){int[] a=new int[4096];Arrays.fill(a,AIR);return a;}
    private static void set(int[] a,int x,int y,int z,int flag){a[index(x,y,z)]=flag;}
    private static void nibble(byte[] a,int x,int y,int z,int level){int i=index(x,y,z);a[i>>1]=(byte)((a[i>>1]&~(15<<((i&1)*4)))|(level<<((i&1)*4)));}
    public static void main(String[] args){
        var activity=options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,10,12,true);
        int[] a=empty();check(scan(a,activity)==null,"empty chunk");
        set(a,1,1,1,CONTAINER|ENTITY);set(a,2,1,1,MACHINERY);set(a,3,1,1,REDSTONE);set(a,4,1,1,GROWTH);
        check(scan(a,activity)==null,"weighted sum below threshold");
        var low=options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,8,12,true);
        Finding f=scan(a,low);check(f!=null&&f.score()==8.5,"exact category weights");check(f.dominant()==0&&f.count(4)==1,"dominant highest-weight category");
        int[] copy=f.counts();copy[0]=99;check(f.count(0)==1,"counts immutable");
        var disabled=new Options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,1,3,12,true,false,false,false,false,false,false,false,false);
        check(scan(a,disabled)==null,"all clue toggles disabled");
        a=empty();for(int x=0;x<2;x++)set(a,x,1,1,GROWTH);check(scan(a,options(Mode.CLASSIC,Method.ANBS_PLUS_SCAN,1,12,false))==null,"classic minimum mature count");
        set(a,2,1,1,GROWTH);check(scan(a,options(Mode.CLASSIC,Method.ANBS_PLUS_SCAN,100,12,false))!=null,"classic mature threshold independent of activity score");
        a=empty();for(int x=0;x<3;x++)set(a,x,1,1,CONTAINER|ENTITY);check(scan(a,options(Mode.CLASSIC,Method.ANBS_PLUS_SCAN,100,12,false))!=null,"classic block entity threshold");
        var anbs=options(Mode.ANBS_PLUS,Method.ANBS_PLUS_SCAN,12,12,true);
        check(scan(a,anbs)!=null,"artificial dense row");
        a=empty();set(a,1,1,1,CONTAINER);set(a,7,7,7,CONTAINER);set(a,14,14,14,CONTAINER);check(scan(a,anbs)==null,"spatially scattered same count rejected");
        a=empty();for(int x=0;x<11;x++)set(a,x,1,1,GEODE|(1<<28));check(scan(a,activity)==null,"minimum geode cells rejects eleven-cell spoof");
        set(a,11,1,1,GEODE|BUDDING);f=scan(a,activity);check(f!=null&&f.clusters()==1&&f.count(3)==12,"connected twelve-cell geode accepted");
        a=empty();for(int x=0;x<12;x++)set(a,x,1,1,GEODE|SHELL);check(scan(a,activity)==null,"calcite/basalt alone is not a geode");
        a=empty();set(a,15,1,0,GEODE|BUDDING);set(a,0,1,1,GEODE|BUDDING);check(scan(a,options(Mode.ANBS_PLUS,Method.ANBS_PLUS_SCAN,100,2,false))==null,"flood fill cannot wrap X/Z boundaries");
        a=empty();set(a,3,3,3,SPAWNER);set(a,3,3,4,COBBLE);set(a,3,3,5,MOSSY);set(a,4,3,3,CONTAINER|ENTITY);
        check(scan(a,options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,3,12,true))==null,"dungeon combination suppressed");
        check(scan(a,options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,3,12,false))!=null,"filter toggle restores feature clues");
        set(a,14,3,14,CONTAINER|ENTITY);f=scan(a,options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,3,12,true));check(f!=null&&f.count(0)==1,"distant player stash survives local structure suppression");
        for(int[] feature:new int[][]{{RAIL,WEB},{CITY,SCULK},{TRIAL,TUFF}}){a=empty();set(a,3,3,3,feature[0]);set(a,3,3,4,feature[1]);set(a,4,3,3,CONTAINER);check(scan(a,options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,3,12,true))==null,"structure combination "+feature[0]);}
        a=empty();set(a,1,1,1,CONTAINER|SHULKER);set(a,2,1,1,CONTAINER|CHEST);set(a,3,1,1,CONTAINER|HOPPER);
        var filtered=new Options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,1,3,12,false,true,false,false,false,true,true,true,true);check(scan(a,filtered)==null,"individual container exclusions");
        a=empty();set(a,5,5,5,GEODE|BUDDING);check(scan(a,options(Mode.ACTIVITY_CLUES,Method.BYPASS,100,12,false))!=null,"bypass accepts exposed budding independently of ANBS minimum");
        a=empty();set(a,5,5,5,GEODE|SHELL);check(scan(a,options(Mode.ACTIVITY_CLUES,Method.BYPASS,100,12,false))==null,"bypass ignores lone exposed calcite/basalt noise");
        a=empty();set(a,5,5,5,GROWTH|CLUSTER);var growthOnly=new Options(Mode.ACTIVITY_CLUES,Method.ANBS_PLUS_SCAN,1,3,12,false,false,false,false,false,false,false,false,true);
        f=scan(a,growthOnly);check(f!=null&&f.matureAmethyst()==1,"growth can independently detect mature amethyst");
        a=empty();for(int x=3;x<=8;x++)for(int y=3;y<=8;y++)for(int z=3;z<=8;z++)if(x==3||x==8||y==3||y==8||z==3||z==8)set(a,x,y,z,GEODE|SHELL|AMETHYST);
        f=scan(a,activity);check(f!=null&&f.score()>=1.5&&f.score()<=4.5,"shell and hollow shape clues have bounded geode weights");
        set(a,3,5,5,GEODE|BUDDING);set(a,4,5,5,BUILT);f=scan(a,activity);check(f!=null&&f.score()>=3,"artificial budding neighbor adds geode clue");
        var light=options(Mode.ACTIVITY_CLUES,Method.LIGHT,100,12,true);
        byte[][] b=new byte[3][],s=new byte[3][];b[1]=new byte[2048];s[1]=new byte[2048];nibble(b[1],5,5,5,5);
        f=evaluate(empty(),-16,b,s,light);check(f!=null&&f.lightClues()==1&&f.score()==0&&f.confidence()<=45,"dark subterranean light-five clue is low confidence");
        check(evaluate(empty(),0,b,s,light)==null,"nonnegative Y rejected");check(evaluate(empty(),-16,b,new byte[3][],light)==null,"unknown skylight is not treated as darkness");
        nibble(s[1],5,5,5,1);check(evaluate(empty(),-16,b,s,light)==null,"nonzero ambient sky rejected");nibble(s[1],5,5,5,0);
        nibble(b[1],6,5,5,6);check(evaluate(empty(),-16,b,s,light)==null,"level-five falloff from stronger light rejected");
        a=empty();for(int i=0;i<a.length;i++)a[i]=CONTAINER;f=scan(a,activity);check(f!=null&&f.targets().size()==128&&f.confidence()==100,"bounded target snapshots and score clamp");
        System.out.println("BaseFinder checks passed: "+checks);
    }
}
