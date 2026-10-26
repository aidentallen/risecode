package dev.riserecode.util.gameplay;

import dev.riserecode.mixin.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.*;
import java.util.*;

/** Native 26.2 integration order, deterministic centerline (launch spread is intentionally excluded). */
public final class TrajectoryPrediction {
    public record Path(List<Vec3> points,HitResult.Type impact){public Path{points=List.copyOf(points);}}
    private TrajectoryPrediction(){ }
    private static boolean loaded(ClientLevel level,Vec3 pos){return level.getChunkSource().getChunk((int)Math.floor(pos.x/16),(int)Math.floor(pos.z/16),net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false)!=null;}
    public static boolean supported(Projectile p){return p instanceof AbstractArrow||p instanceof ThrowableProjectile||p instanceof AbstractWindCharge||p instanceof FireworkRocketEntity;}
    public static Path predict(ClientLevel level,Projectile p,int ticks){
        Vec3 pos=p.position(),motion=p.getDeltaMovement();List<Vec3> points=new ArrayList<>();points.add(pos);HitResult.Type impact=HitResult.Type.MISS;
        if(p instanceof AbstractArrow&&((ArrowPhysicsAccessor)p).risecode$grounded()){return new Path(points,impact);}
        for(int i=0;i<ticks;i++){
            if(!loaded(level,pos)||pos.y<level.getMinY()-32){break;}
            boolean water=level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER);
            double gravity=p.isNoGravity()?0:p.getGravity();
            if(p instanceof ThrowableProjectile){motion=motion.add(0,-gravity,0).scale(water?0.8:((ThrowablePhysicsAccessor)p).risecode$airDrag());}
            else if(p instanceof FireworkRocketEntity rocket&&!rocket.isShotAtAngle()){motion=new Vec3(motion.x*1.15,motion.y+0.04,motion.z*1.15);}
            Vec3 next=pos.add(motion);
            if(!loaded(level,next)){break;}
            var block=level.clipIncludingBorder(new ClipContext(pos,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            Vec3 end=block.getType()==HitResult.Type.MISS?next:block.getLocation();Entity owner=p.getOwner();
            float margin=Math.clamp((p.tickCount+i-2)/20.0f,0,0.3f);
            java.util.function.Predicate<Entity> eligible=e->e.canBeHitByProjectile()&&!e.isSpectator()&&e!=owner;
            AABB search=new AABB(pos,end).inflate(1);EntityHitResult entityHit;
            if(p instanceof AbstractArrow){
                Vec3 start=pos;
                entityHit=net.minecraft.world.entity.projectile.ProjectileUtil.getManyEntityHitResult(level,p,pos,end,search,eligible,margin,ClipContext.Block.COLLIDER,false)
                        .stream().min(Comparator.comparingDouble(hit->start.distanceToSqr(hit.getLocation()))).orElse(null);
            }else{entityHit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level,p,pos,end,search,eligible,margin);}
            if(entityHit!=null){end=entityHit.getLocation();impact=HitResult.Type.ENTITY;}
            if(impact!=HitResult.Type.ENTITY&&block.getType()!=HitResult.Type.MISS){impact=HitResult.Type.BLOCK;}
            points.add(end);if(impact!=HitResult.Type.MISS){break;}
            pos=next;
            if(p instanceof AbstractArrow){motion=motion.scale(water?((ArrowPhysicsAccessor)p).risecode$waterDrag():((ArrowPhysicsAccessor)p).risecode$airDrag()).add(0,-gravity,0);}
            else if(p instanceof AbstractWindCharge){motion=motion.add(0,-gravity,0);}
            if(motion.lengthSqr()<1e-8){break;}
        }
        return new Path(points,impact);
    }
}
