package dev.riserecode.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded, frame-rate independent highlight pose. Retargeting samples the displayed pose. */
public final class BlockHighlightMotion {
    public record Part(AABB bounds, double opacity) { }
    public record Frame(AABB bounds, List<Part> parts, double scale) { }
    private Object key;
    private List<AABB> requested = List.of();
    private List<Part> from = List.of(), to = List.of();
    private AABB start, end;
    private long started;
    private double duration, popDuration;
    private boolean popping;
    private static final int MAX_PARTS = 128;

    public void clear() { key = null; requested = List.of(); from = to = List.of(); start = end = null; }
    public boolean isEmpty() { return end == null; }
    public void target(Object id, List<AABB> boxes, long now, double seconds, boolean pop, double popSeconds) {
        target(id, boxes, now, seconds, pop, popSeconds, true);
    }
    public void target(Object id, List<AABB> boxes, long now, double seconds, boolean pop, double popSeconds, boolean popOnSwitch) {
        if (boxes.isEmpty()) { clear(); return; }
        if (Objects.equals(key, id) && requested.equals(boxes)) {duration=Math.max(0,seconds);popping=popping&&pop;popDuration=Math.max(0,popSeconds);return;}
        boolean acquired = isEmpty();
        boolean changedBlock = !Objects.equals(key, id);
        Frame current = sample(now);
        if (current != null && current.scale() < 1) {
            var center=current.bounds().getCenter();
            current=new Frame(scaled(current.bounds(),center,current.scale()),current.parts().stream().map(p -> new Part(scaled(p.bounds(),center,sample(now).scale()),p.opacity())).toList(),1);
        }
        AABB envelope = boxes.getFirst();
        for (AABB b : boxes) { envelope = envelope.minmax(b); }
        start = current == null ? envelope : current.bounds(); end = envelope;
        from = current == null ? List.of() : normalize(current.parts(), current.bounds());
        to = normalize(boxes.stream().map(b -> new Part(b, 1)).toList(), envelope);
        key = id; requested = List.copyOf(boxes); started = now;
        duration = Math.max(0, seconds); popping = pop && (acquired || changedBlock && popOnSwitch); popDuration = Math.max(0, popSeconds);
    }
    public Frame sample(long now) {
        if (end == null) { return null; }
        double age = Math.max(0, (now - started) * 1e-9), progress = eased(age, duration);
        AABB bounds = interpolate(start, end, progress);
        List<Part> parts = new ArrayList<>();
        if (progress < 1 && !from.isEmpty()) { transform(from, bounds, 1 - progress, parts); }
        transform(to, bounds, from.isEmpty() ? 1 : progress, parts);
        double scale = popping ? eased(age, popDuration) : 1;
        return new Frame(bounds, List.copyOf(parts), scale);
    }
    public Frame immediate() { return end == null ? null : new Frame(end, requested.stream().map(b -> new Part(b, 1)).toList(), 1); }
    private static double eased(double age, double duration) { double p = duration <= 0 ? 1 : Math.clamp(age / duration, 0, 1); return 1 - Math.pow(1 - p, 3); }
    private static AABB interpolate(AABB a, AABB b, double p) {
        return new AABB(mix(a.minX,b.minX,p),mix(a.minY,b.minY,p),mix(a.minZ,b.minZ,p),mix(a.maxX,b.maxX,p),mix(a.maxY,b.maxY,p),mix(a.maxZ,b.maxZ,p));
    }
    private static double mix(double a, double b, double p) { return a + (b - a) * p; }
    private static List<Part> normalize(List<Part> parts, AABB envelope) {
        double dx = Math.max(1e-9,envelope.getXsize()), dy = Math.max(1e-9,envelope.getYsize()), dz = Math.max(1e-9,envelope.getZsize());
        return parts.stream().filter(p -> p.opacity() > .001).sorted(Comparator.comparingDouble(Part::opacity).reversed()).limit(MAX_PARTS)
                .map(p -> { AABB b=p.bounds();return new Part(new AABB((b.minX-envelope.minX)/dx,(b.minY-envelope.minY)/dy,(b.minZ-envelope.minZ)/dz,(b.maxX-envelope.minX)/dx,(b.maxY-envelope.minY)/dy,(b.maxZ-envelope.minZ)/dz),p.opacity()); }).toList();
    }
    private static void transform(List<Part> parts,AABB envelope,double alpha,List<Part> result) {
        if(alpha<=.001){return;}
        for(Part p:parts){AABB b=p.bounds();result.add(new Part(new AABB(envelope.minX+b.minX*envelope.getXsize(),envelope.minY+b.minY*envelope.getYsize(),envelope.minZ+b.minZ*envelope.getZsize(),envelope.minX+b.maxX*envelope.getXsize(),envelope.minY+b.maxY*envelope.getYsize(),envelope.minZ+b.maxZ*envelope.getZsize()),p.opacity()*alpha));}
    }
    public static AABB scaled(AABB box, Vec3 center, double scale) {
        return new AABB(center.x+(box.minX-center.x)*scale,center.y+(box.minY-center.y)*scale,center.z+(box.minZ-center.z)*scale,center.x+(box.maxX-center.x)*scale,center.y+(box.maxY-center.y)*scale,center.z+(box.maxZ-center.z)*scale);
    }
    /** The gap is central, leaving equal-length segments at both corners. */
    public static List<Vec3[]> edge(Vec3 a,Vec3 b,double gapPercent){
        double gap=Math.clamp(gapPercent/100,0,1);
        if(gap==0){return List.<Vec3[]>of(new Vec3[]{a,b});}if(gap==1){return List.of();}
        double length=(1-gap)/2;return List.of(new Vec3[]{a,a.lerp(b,length)},new Vec3[]{b.lerp(a,length),b});
    }
}
