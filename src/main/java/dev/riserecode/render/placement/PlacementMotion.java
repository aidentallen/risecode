package dev.riserecode.render.placement;

import dev.riserecode.util.MathUtil;
import org.joml.Matrix4f;

/** A block-local pose; identity at completion, including elastic overshoot. */
public final class PlacementMotion {
    public enum Pivot { CENTER, BOTTOM, TOP }
    public record Settings(double duration, MathUtil.Easing easing, double strength, double scale, double height,
                           double x, double y, double z, double rx, double ry, double rz, Pivot pivot) { }
    private PlacementMotion() { }
    public static Matrix4f pose(Settings s, double progress, double yaw) {
        double remaining=1-s.easing().apply(Math.clamp(progress,0,1));
        float t=(float)(remaining*s.strength());
        float pivot=s.pivot()==Pivot.BOTTOM?0:s.pivot()==Pivot.TOP?1:.5f;
        return new Matrix4f().translation(.5f,pivot,.5f).rotateY((float)yaw)
                .translate((float)s.x()*t,(float)s.y()*t,(float)s.z()*t)
                .rotateXYZ((float)Math.toRadians(s.rx())*t,(float)Math.toRadians(s.ry())*t,(float)Math.toRadians(s.rz())*t)
                .scale(Math.max(.001f,1+((float)s.scale()-1)*t),Math.max(.001f,1+((float)(s.scale()*s.height())-1)*t),Math.max(.001f,1+((float)s.scale()-1)*t))
                .rotateY((float)-yaw).translate(-.5f,-pivot,-.5f);
    }
}
