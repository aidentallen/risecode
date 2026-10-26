package dev.riserecode.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;

public final class ItemRenderEvent extends Event {
    private final ItemEntityRenderState state;
    private final PoseStack poses;
    private final SubmitNodeCollector collector;
    public ItemRenderEvent(ItemEntityRenderState state, PoseStack poses, SubmitNodeCollector collector) { super(true); this.state = state; this.poses = poses; this.collector = collector; }
    public ItemEntityRenderState state() { return state; }
    public PoseStack poses() { return poses; }
    public SubmitNodeCollector collector() { return collector; }
}
