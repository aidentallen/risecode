package dev.riserecode;

import dev.riserecode.render.SectionBlockIndex;
import dev.riserecode.render.IrisTerrainTransform;
import dev.riserecode.ui.render.FramePinnedCache;
import java.util.ArrayList;
import java.util.List;

public final class RemediationChecks {
    private static int checks;
    private RemediationChecks(){ }
    private static void check(boolean ok,String message){checks++;if(!ok){throw new AssertionError(message);}}
    public static void main(String[] args){
        var index=new SectionBlockIndex();
        for(int x=-32;x<32;x++){for(int y=-16;y<16;y++){for(int z=-32;z<32;z++){index.set(x,y,z,true);}}}
        check(index.sectionCount()==32,"common-block storage has one bitset per section, independent of match count");
        var center=new SectionBlockIndex.Position(0,0,0);var search=index.search(center,64);
        int steps=0;long work=0;
        while(!search.step(127,Long.MAX_VALUE)){check(search.work()-work<=127,"each query step respects work cap");work=search.work();steps++;}
        check(steps>1&&search.result().size()==64,"common-block query finishes incrementally with bounded results");
        check(search.result().getLast().equals(center),"nearest result retained");
        long before=search.work();check(search.step(127,Long.MAX_VALUE)&&search.work()==before,"completed query does no more work");
        var moved=index.search(new SectionBlockIndex.Position(31,15,31),16);while(!moved.step(4096,Long.MAX_VALUE)){}
        check(moved.result().getLast().equals(new SectionBlockIndex.Position(31,15,31)),"movement changes nearest results");
        var unloading=index.search(center,4096);unloading.step(20,Long.MAX_VALUE);index.removeChunk(0,0,-1,2);
        while(!unloading.step(4096,Long.MAX_VALUE)){}
        check(!unloading.current()&&index.sectionCount()==30,"unloading invalidates the result revision and removes section storage");
        var fresh=index.search(center,4096);while(!fresh.step(4096,Long.MAX_VALUE)){}
        check(fresh.result().stream().noneMatch(p->p.x()>=0&&p.x()<16&&p.z()>=0&&p.z()<16),"new query excludes unloaded chunks");
        var stale=index.search(center,4096);stale.step(512,Long.MAX_VALUE);index.clear();check(stale.step(1,Long.MAX_VALUE)&&stale.result().isEmpty(),"dimension reset cancels suspended query and releases results");
        var released=new ArrayList<String>();var cache=new FramePinnedCache<Integer,String>(2,released::add);
        cache.put(1,"one");cache.put(2,"two");check(!cache.makeRoom()&&released.isEmpty(),"current extraction pins every referenced texture");
        cache.beginFrame();check(cache.get(1).equals("one"),"cache hit pins texture");check(cache.makeRoom()&&released.equals(List.of("two")),"eviction chooses an unpinned texture");
        cache.put(3,"three");cache.discard();check(!cache.makeRoom(),"retired textures count against the extraction resource cap");check(released.equals(List.of("two")),"discard defers releases past drawing");cache.beginFrame();check(released.containsAll(List.of("one","two","three"))&&cache.size()==0,"next extraction releases retired textures exactly once");
        cache.put(1,"old");cache.discard();cache.put(1,"replacement");cache.beginFrame();
        check(released.contains("old")&&!released.contains("replacement")&&cache.get(1).equals("replacement"),"discard/reopen in one extraction retires only the old resource");
        check(dev.riserecode.render.RendererCapabilities.supportedVersions("0.9.2+mc26.2",null),"verified Sodium version accepted");
        check(dev.riserecode.render.RendererCapabilities.supportedVersions("0.9.2+mc26.2","1.11.4+mc26.2"),"verified Iris/Sodium versions accepted");
        check(!dev.riserecode.render.RendererCapabilities.supportedVersions("0.9.3+mc26.2",null)&&!dev.riserecode.render.RendererCapabilities.supportedVersions("0.9.2+mc26.2","future"),"unknown shader layouts cannot enable dependent policies");
        String[] nodes={"uniform uint u_RegionID;","return _get_relative_chunk_coord(pos) * vec3(16.0f);","mc_chunkFade = (chunkFade < 0) ? 1.0 : fade;"};
        check(IrisTerrainTransform.patch(nodes)!=nodes,"all supported shader anchors permit patch");
        for(int missing=0;missing<nodes.length;missing++){
            String[] bad=nodes.clone();bad[missing]="changed";check(IrisTerrainTransform.patch(bad)==bad,"missing anchor fails atomically");
            bad=nodes.clone();bad[missing]+=bad[missing];check(IrisTerrainTransform.patch(bad)==bad,"ambiguous anchor fails atomically");
        }
        String[] noFade=nodes.clone();noFade[2]="const float mc_chunkFade = -1.0;";
        check(IrisTerrainTransform.patch(noFade)!=noFade,"Iris native fade-disabled branch supports motion without enabling fade");
        String[] patched=IrisTerrainTransform.patch(nodes);check(IrisTerrainTransform.patch(patched)==patched,"shader cannot be patched twice");
        System.out.println("Remediation checks passed ("+checks+" assertions).");
    }
}
