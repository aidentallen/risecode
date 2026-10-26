package dev.riserecode.render;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;

/** Preserve the modern attachment/clearance, but restore the old cape's independent rotation. */
public final class LegacyCapePose {
    private LegacyCapePose() { }
    public static Quaternionf rotation(LegacyCapeMotion.Angles angles) {
        float radians = (float) Math.PI / 180.0F;
        return new Quaternionf().rotateX(angles.pitch() * radians).rotateZ(angles.sway() / 2.0F * radians)
                .rotateY(-angles.sway() / 2.0F * radians).rotateY(180.0F * radians);
    }
    public static void apply(ModelPart body, ModelPart cape, LegacyCapeMotion.Angles angles) {
        Quaternionf parent = new Quaternionf().rotationZYX(body.zRot, body.yRot, body.xRot);
        Quaternionf local = parent.invert().mul(rotation(angles));
        cape.setRotation(0, 0, 0);
        cape.rotateBy(local);
    }
}
