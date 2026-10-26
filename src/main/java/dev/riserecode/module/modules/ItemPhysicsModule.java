package dev.riserecode.module.modules;

import com.mojang.math.Axis;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.ItemRenderEvent;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.render.ItemPhysicsState;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.util.RandomSource;

/** Pure render transforms: item positions, pickup behavior and server physics remain vanilla. */
public final class ItemPhysicsModule extends Module {
    public enum Spin { OFF, AIRBORNE, ALWAYS }
    public enum SpinAxis { X, Y, Z }
    private final dev.riserecode.setting.BooleanSetting randomYaw = register(new dev.riserecode.setting.BooleanSetting("RandomYaw", "Stable per-item yaw", true));
    private final dev.riserecode.setting.DoubleSetting rotationX = register(dev.riserecode.setting.DoubleSetting.extended("RotationX", "Additional X pose", 0, -180, 180));
    private final dev.riserecode.setting.DoubleSetting rotationY = register(dev.riserecode.setting.DoubleSetting.extended("RotationY", "Additional Y pose", 0, -180, 180));
    private final dev.riserecode.setting.DoubleSetting rotationZ = register(dev.riserecode.setting.DoubleSetting.extended("RotationZ", "Additional Z pose", 0, -180, 180));
    private final dev.riserecode.setting.EnumSetting<Spin> spin = register(new dev.riserecode.setting.EnumSetting<>("Spin", "When to animate rotation", Spin.AIRBORNE));
    private final dev.riserecode.setting.EnumSetting<SpinAxis> axis = register(new dev.riserecode.setting.EnumSetting<>("SpinAxis", "Axis for animated rotation", SpinAxis.Z));
    private final dev.riserecode.setting.DoubleSetting speed = register(dev.riserecode.setting.DoubleSetting.extended("SpinSpeed", "Degrees per second", 68.75494, -360, 360));
    private final RandomSource random = RandomSource.create();
    public ItemPhysicsModule(EventBus bus) { super("ItemPhysics", "Dropped items rest on the ground with stable individual rotations.", Category.WORLD, bus);
        axis.setVisibleWhen(() -> spin.getValue() != Spin.OFF); speed.setVisibleWhen(() -> spin.getValue() != Spin.OFF); }
    @Override protected void onEnable() {
        listen(ItemRenderEvent.class, event -> {
            var state = event.state(); if (state.item.isEmpty()) { return; }
            var physics = (ItemPhysicsState) state; var bounds = state.item.getModelBoundingBox(); var poses = event.poses();
            boolean flat = bounds.getZsize() <= 0.0625;
            poses.pushPose();
            try {
                double height = flat ? bounds.getZsize() / 2 + 0.025 : -bounds.minY + 0.02;
                poses.translate(0, height, 0); if (randomYaw.getValue()) { poses.mulPose(Axis.YP.rotation(physics.riserecode$rotation())); }
                if (flat) { poses.mulPose(Axis.XP.rotationDegrees(90)); }
                poses.mulPose(Axis.XP.rotationDegrees(rotationX.getValue().floatValue()));
                poses.mulPose(Axis.YP.rotationDegrees(rotationY.getValue().floatValue()));
                poses.mulPose(Axis.ZP.rotationDegrees(rotationZ.getValue().floatValue()));
                if (spin.getValue() == Spin.ALWAYS || spin.getValue() == Spin.AIRBORNE && !physics.riserecode$onGround()) {
                    Axis selected = switch (axis.getValue()) { case X -> Axis.XP; case Y -> Axis.YP; case Z -> Axis.ZP; };
                    poses.mulPose(selected.rotationDegrees((float) ((state.ageInTicks / 20.0 * speed.getValue()) % 360)));
                }
                ItemEntityRenderer.submitMultipleFromCount(poses, event.collector(), state.lightCoords, state, random);
                event.cancel();
            } finally { poses.popPose(); }
        });
    }
}
