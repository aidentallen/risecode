package dev.riserecode.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Interpolate actual displayed corners, including rotated placement poses, without AABB expansion. */
public final class BlockHighlightGeometry {
    public record Part(HighlightMesh mesh,double opacity) { public List<Vec3> vertices(){return mesh.vertices();} }
    private Object key,shapeKey;
    private List<Part> from=List.of(),shown=List.of();
    private long started;
    private boolean popping;
    private boolean fullScale=true;
    public boolean fullScale(){return fullScale;}
    private List<HighlightMesh> requested=List.of();
    private double duration,popSeconds;
    public void clear(){key=shapeKey=null;from=shown=List.of();requested=List.of();}
    public List<Part> target(Object id,Object shape,List<List<Vec3>> target,long now,double duration,boolean pop,boolean popOnSwitch,double popDuration){
        return targetMeshes(id,shape,target.stream().map(HighlightMesh::box).toList(),now,duration,pop,popOnSwitch,popDuration);
    }
    public List<Part> targetMeshes(Object id,Object shape,List<HighlightMesh> target,long now,double seconds,boolean pop,boolean popOnSwitch,double popDuration){
        if(target.isEmpty()||target.stream().allMatch(m->m.vertices().isEmpty())){clear();return shown;}
        if(!Objects.equals(key,id)||!Objects.equals(shapeKey,shape)){
            boolean acquisition=key==null,switched=!Objects.equals(key,id);
            // Advance the old request to this timestamp before changing its target. Otherwise a
            // different target on every frame continually resets at progress zero and never moves.
            if(!requested.isEmpty())sample(now);
            from=shown.stream().filter(p->p.opacity()>.001).sorted(java.util.Comparator.comparingDouble(Part::opacity).reversed()).limit(128).toList();
            popping=pop&&(acquisition||switched&&popOnSwitch);key=id;shapeKey=shape;started=now;
        }else{popping&=pop;}
        requested=List.copyOf(target);duration=seconds;popSeconds=popDuration;
        return sample(now);
    }
    private List<Part> sample(long now){
        double age=Math.max(0,(now-started)*1e-9),t=ease(age,duration),scale=popping?ease(age,popSeconds):1;
        var output=new ArrayList<Part>();
        boolean matching=from.size()==requested.size();
        for(int i=0;matching&&i<requested.size();i++)matching=from.get(i).mesh().sameTopology(requested.get(i));
        if(!from.isEmpty()&&matching&&t<1){
            for(int i=0;i<requested.size();i++){var target=requested.get(i);var vertices=new ArrayList<Vec3>();
                for(int v=0;v<target.vertices().size();v++)vertices.add(from.get(i).vertices().get(v).lerp(target.vertices().get(v),t));
                output.add(new Part(target.withVertices(vertices),from.get(i).opacity()*(1-t)+t));}
        }else if(t<1&&!from.isEmpty()){
            var source=bounds(from.stream().map(Part::vertices).toList());var destination=bounds(requested.stream().map(HighlightMesh::vertices).toList());
            var envelope=new AABB(source.minX+(destination.minX-source.minX)*t,source.minY+(destination.minY-source.minY)*t,source.minZ+(destination.minZ-source.minZ)*t,
                    source.maxX+(destination.maxX-source.maxX)*t,source.maxY+(destination.maxY-source.maxY)*t,source.maxZ+(destination.maxZ-source.maxZ)*t);
            for(var part:from)if(part.opacity()*(1-t)>.001)output.add(new Part(part.mesh().withVertices(remap(part.vertices(),source,envelope)),part.opacity()*(1-t)));
            if(t>.001)for(var target:requested)output.add(new Part(target.withVertices(remap(target.vertices(),destination,envelope)),t));
        }else for(var target:requested)output.add(new Part(target,1));
        if(scale<1){var center=bounds(requested.stream().map(HighlightMesh::vertices).toList()).getCenter();
            var scaled=new ArrayList<Part>();for(var part:output)scaled.add(new Part(part.mesh().map(p->center.add(p.subtract(center).scale(scale))),part.opacity()));output=scaled;}
        fullScale=scale>=1;shown=List.copyOf(output);return shown;
    }
    private static AABB bounds(List<List<Vec3>> parts){AABB bounds=null;for(var vertices:parts)for(var p:vertices){var b=new AABB(p,p);bounds=bounds==null?b:bounds.minmax(b);}return Objects.requireNonNull(bounds);}
    private static double unit(double value,double min,double size){return size<1e-9?.5:(value-min)/size;}
    private static List<Vec3> remap(List<Vec3> vertices,AABB source,AABB target){return vertices.stream().map(p->new Vec3(
            target.minX+unit(p.x,source.minX,source.getXsize())*target.getXsize(),target.minY+unit(p.y,source.minY,source.getYsize())*target.getYsize(),
            target.minZ+unit(p.z,source.minZ,source.getZsize())*target.getZsize())).toList();}
    private static double ease(double age,double duration){double t=duration<=0?1:Math.clamp(age/duration,0,1);return 1-Math.pow(1-t,3);}
    public static List<Vec3> corners(AABB b){return List.of(new Vec3(b.minX,b.minY,b.minZ),new Vec3(b.maxX,b.minY,b.minZ),new Vec3(b.maxX,b.maxY,b.minZ),new Vec3(b.minX,b.maxY,b.minZ),new Vec3(b.minX,b.minY,b.maxZ),new Vec3(b.maxX,b.minY,b.maxZ),new Vec3(b.maxX,b.maxY,b.maxZ),new Vec3(b.minX,b.maxY,b.maxZ));}
}
