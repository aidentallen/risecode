package dev.riserecode.cosmetics;

import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Tick-based bounded springs and trails; render sampling never advances simulation. */
public final class CosmeticsMotion {
    private final ArrayDeque<Vec3> trail=new ArrayDeque<>();
    private Vec3 position;
    private List<Vec3> trailSnapshot=List.of();
    private float oldSway,sway,velocity;
    public void clear(){trail.clear();position=null;oldSway=sway=velocity=0;trailSnapshot=List.of();}
    public void tick(Vec3 next){
        if(position==null||next.distanceToSqr(position)>64){clear();position=next;trail.add(next);trailSnapshot=List.copyOf(trail);return;}
        oldSway=sway;float target=(float)Math.min(1,next.subtract(position).horizontalDistance()*3);
        velocity=(velocity+(target-sway)*.22F)*.68F;sway=Math.clamp(sway+velocity,-.25F,1.25F);
        boolean moving=next.distanceToSqr(position)>.000025;position=next;if(moving){trail.addFirst(next);}else if(!trail.isEmpty()){trail.removeLast();}while(trail.size()>160){trail.removeLast();}trailSnapshot=List.copyOf(trail);
    }
    public float sample(float partial){return oldSway+(sway-oldSway)*Math.clamp(partial,0,1);}
    public List<Vec3> trail(){return trailSnapshot;}
}
