package dev.riserecode.render;

/** Primitive extraction metadata; the delayed render never reads the live item entity. */
public interface ItemPhysicsState {
    boolean riserecode$onGround();
    float riserecode$rotation();
    void riserecode$physics(boolean onGround, float rotation);
}
