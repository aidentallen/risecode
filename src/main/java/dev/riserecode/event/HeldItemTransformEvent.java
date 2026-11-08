package dev.riserecode.event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
/** Only first-person item poses, never player arm geometry. */
public final class HeldItemTransformEvent extends Event {
    private final PoseStack poses;
    private final ItemDisplayContext context;
    private final ItemStack stack;
    public HeldItemTransformEvent(PoseStack poses, ItemDisplayContext context) { this(poses,context,ItemStack.EMPTY); }
    public HeldItemTransformEvent(PoseStack poses, ItemDisplayContext context, ItemStack stack) { this.poses=poses;this.context=context;this.stack=stack; }
    public PoseStack poses() { return poses; }
    public ItemDisplayContext context() { return context; }
    public ItemStack stack() { return stack; }
}
