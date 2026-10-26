package dev.riserecode.event;
import net.minecraft.world.entity.Entity;
/** Posted only at vanilla entity label extraction; cancellation leaves model rendering intact. */
public final class LabelRenderEvent extends Event {
    private final Entity entity;
    public LabelRenderEvent(Entity entity){super(true);this.entity=entity;}
    public Entity entity(){return entity;}
}
