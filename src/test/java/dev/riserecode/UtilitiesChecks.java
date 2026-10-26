package dev.riserecode;

import dev.riserecode.map.*;
import dev.riserecode.config.JsonStorage;
import java.nio.file.*;
import java.util.*;

/** Persistence, isolation primitives and derived layout boundaries independent of a running game. */
public final class UtilitiesChecks {
    private static int checks;
    private UtilitiesChecks(){ }
    private static void check(boolean value,String label){checks++;if(!value){throw new AssertionError(label);}}
    public static void main(String[] args) throws Exception {
        for(int block:new int[]{-1025,-513,-512,-65,-64,-1,0,1,63,64,511,512}){
            int page=MapPageLayout.globalPageFromBlock(block);check(page*64<=block&&block<page*64+64,"Negative page coordinates");check(MapPageLayout.localPage(page)>=0&&MapPageLayout.localPage(page)<8,"Local page bounds");
        }
        check(MapLodPolicy.branchLevel(1)==0&&MapLodPolicy.branchLevel(0.25f)==2,"Physical density selection");check(MapLodPolicy.stabilizeBranchLevel(2,1,0.249f)==1,"Density hysteresis");
        for(float zoom:new float[]{0.03125f,0.125f,0.25f,1,8}){int lod=MapLodPolicy.viewportLevel(zoom,1920,1080);double span=(16<<lod)*zoom;check((Math.ceil(1920/span)+2)*(Math.ceil(1080/span)+2)<=384,"Wide map viewport stays within residency budget");}
        var store=new WaypointStore();var p=store.add("Home",-401,72,-1,"minecraft:overworld");store.put(p.tracked(true));check(store.all().getFirst().tracked(),"Waypoint tracking");
        store.death(-12,64,-4,"minecraft:overworld");store.death(-30,64,-9,"minecraft:overworld");store.death(1,70,1,"minecraft:the_nether");check(store.all().stream().filter(WaypointStore.Waypoint::death).count()==3,"All deaths retained");
        store.reached(-30,64,-9,"minecraft:the_nether");check(store.all().size()==4,"Death dimension isolation");store.reached(-30,64,-9,"minecraft:overworld");check(store.all().size()==4&&store.all().stream().filter(WaypointStore.Waypoint::reached).count()==1,"Reached death marked without deletion");
        Path dir=Files.createTempDirectory("risecode-utilities-");JsonStorage.write(dir.resolve("waypoints.json"),store.snapshot());var loaded=new WaypointStore();loaded.load(JsonStorage.read(dir.resolve("waypoints.json")));check(loaded.all().equals(store.all()),"Waypoint persistence round trip");loaded.remove(p.id());check(loaded.all().size()==3&&store.all().size()==4,"Independent waypoint stores");
        try{new WaypointStore.Waypoint(UUID.randomUUID(),"bad",Integer.MIN_VALUE,0,0,"minecraft:overworld","",-1,true,false,false);throw new AssertionError("Invalid bounds accepted");}catch(IllegalArgumentException expected){check(true,"Coordinates bounded");}
        short[] blocks=new short[256],heights=new short[256];int[] tint=new int[256],colors=new int[256];byte[] water=new byte[256];Arrays.fill(heights,(short)77);Arrays.fill(tint,0xAACCEE);Arrays.fill(colors,0xFF8899AA);water[0]=12;
        var tile=new SurfaceTile(-3,-2,List.of("{\"Name\":\"minecraft:stone\"}"),blocks,heights,tint,water,colors);blocks[0]=5;heights[0]=3;check(tile.block(0)==0&&tile.height(0)==77,"Snapshot owns immutable arrays");tile.heights()[0]=99;check(tile.height(0)==77,"Snapshot accessors cannot mutate");
        SurfaceTile overview=SurfaceOverview.merge(null,tile,-1,-1);check(java.util.Arrays.stream(overview.colors()).filter(c->(c>>>24)!=0).count()==16,"Overview publishes only observed child cells");
        check(overview.height(8*16+4)==77,"Negative child overview location");
        tile.write(dir.resolve("a/negative.rsm"));var round=SurfaceTile.read(dir.resolve("a/negative.rsm"));check(round.x()==-3&&round.z()==-2&&round.water(0)==12&&round.tint(0)==0xAACCEE&&round.height(0)==77,"Terrain palette/tint/height/water persisted");
        byte[] corrupt=Files.readAllBytes(dir.resolve("a/negative.rsm"));corrupt[0]=0;Files.write(dir.resolve("bad.rsm"),corrupt);try{SurfaceTile.read(dir.resolve("bad.rsm"));throw new AssertionError();}catch(java.io.IOException expected){check(true,"Corrupt terrain rejected");}
        check(SurfaceTile.shade(0xFFFFFF,0xFFFFFF,0,0)==0xFFFFFFFF,"Neutral shading");check((SurfaceTile.shade(0xFFFFFF,0xFFFFFF,-8,0)&255)<255,"Height shading");
        check(!MapService.hash("server:a").equals(MapService.hash("server:b"))&&!MapService.hash("server:a").equals(MapService.hash("local:a")),"World/server namespaces isolated");
        System.out.println("Utilities checks passed ("+checks+" assertions).");
    }
}
