package dev.riserecode.event;

import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

public final class CameraInputEvent extends Event {
    private final Input keys;
    private final Vec2 movement;
    public CameraInputEvent(Input keys, Vec2 movement) { super(true); this.keys = keys; this.movement = movement; }
    public Input keys() { return keys; }
    public Vec2 movement() { return movement; }
}
