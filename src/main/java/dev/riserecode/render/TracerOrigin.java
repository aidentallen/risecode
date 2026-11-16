package dev.riserecode.render;

import dev.riserecode.event.Render3DEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Tracer start point in front of the crosshair, with vanilla view bobbing cancelled so it stays centered while walking. */
public final class TracerOrigin {
    private static final float DISTANCE = 0.3f;
    private TracerOrigin() { }

    public static Vec3 of(Render3DEvent event) {
        var context = event.getContext();
        var camera = context.camera();
        var view = new Vector3f(0, 0, -DISTANCE);
        var state = context.levelState().cameraRenderState;
        var entity = state == null ? null : state.entityRenderState;
        if (entity != null && entity.isPlayer && Minecraft.getInstance().options.bobView().get()) {
            float walk = entity.backwardsInterpolatedWalkDistance, bob = entity.bob;
            // the same transform GameRenderer.bobView applies to the view; its inverse moves the start back to screen center
            var bobbing = new Matrix4f()
                .translate(Mth.sin(walk * Mth.PI) * bob * 0.5f, -Math.abs(Mth.cos(walk * Mth.PI) * bob), 0)
                .rotateZ(Mth.sin(walk * Mth.PI) * bob * 3f * Mth.DEG_TO_RAD)
                .rotateX(Math.abs(Mth.cos(walk * Mth.PI - 0.2f) * bob) * 5f * Mth.DEG_TO_RAD);
            bobbing.invert().transformPosition(view);
        }
        // view space to world space: the camera orientation maps (0, 0, -1) onto the look direction
        camera.rotation().transform(view);
        return camera.position().add(view.x, view.y, view.z);
    }
}
