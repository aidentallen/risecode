package dev.riserecode.module.modules;

import dev.riserecode.event.ChunkLifecycleEvent;
import dev.riserecode.event.ChunkRenderPolicyEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.ChunkMotionTracker;
import dev.riserecode.render.ChunkTerrainRenderer;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;

public final class ChunkAnimationsModule extends Module {
    public final EnumSetting<ChunkMotionTracker.Direction> direction = register(new EnumSetting<>("Direction", "Up rises from below; Down descends from above", ChunkMotionTracker.Direction.UP));
    public final DoubleSetting distance = register(new DoubleSetting("Distance", "Vertical travel in blocks", 8, 0, 64));
    public final DoubleSetting duration = register(new DoubleSetting("Duration", "Movement duration in seconds", 0.5, 0.05, 3));
    public final EnumSetting<ChunkMotionTracker.Curve> easing = register(new EnumSetting<>("Easing", "Movement curve", ChunkMotionTracker.Curve.OUT_QUART));
    private final ChunkMotionTracker motion = new ChunkMotionTracker();
    public ChunkAnimationsModule(EventBus bus) { super("ChunkAnimations", "Smooth upward or downward movement for newly loaded chunks", Category.RENDER, bus); }
    public ChunkMotionTracker motion() { return motion; }
    public String status() { return ChunkTerrainRenderer.compatibilityStatus(); }
    @Override protected void onEnable() {
        listen(ChunkLifecycleEvent.class, event -> {
            switch (event.kind()) {
                case LOAD -> motion.loaded(event.chunk());
                case UNLOAD -> motion.unloaded(event.chunk());
                case RESET -> motion.clear();
            }
        });
        listen(ChunkRenderPolicyEvent.class, event -> event.animate(motion,
                new ChunkMotionTracker.Settings(direction.getValue(), distance.getValue(), duration.getValue(), easing.getValue())));
    }
    @Override protected void onDisable() { motion.clear(); ChunkTerrainRenderer.invalidate(); }
}
