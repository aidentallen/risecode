package dev.riserecode.event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
/** Only first-person item poses, never player arm geometry. */
public final class HeldItemTransformEvent extends Event {
    private final PoseStack poses;
    private final ItemDisplayContext context;
    public HeldItemTransformEvent(PoseStack poses, ItemDisplayContext context) { this.poses=poses;this.context=context; }
    public PoseStack poses() { return poses; }
    public ItemDisplayContext context() { return context; }
}
