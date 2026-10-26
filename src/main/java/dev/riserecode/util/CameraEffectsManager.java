package dev.riserecode.util;

import dev.riserecode.event.EventBus;
import dev.riserecode.event.FovEvent;
import dev.riserecode.event.MouseRotationEvent;
import dev.riserecode.ui.animation.Animation;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;

/** Keeps the camera's exit transition alive after its requesting module is disabled. */
public final class CameraEffectsManager implements AutoCloseable {
    private final Minecraft minecraft;
    private final Animation amount = Animation.ui(1, 0.15, MathUtil.Easing.OUT_QUART);
    private final EventBus.Subscription fov;
    private final EventBus.Subscription input;
    private Object owner;
    private DoubleSupplier strength = () -> 1;
    private BooleanSupplier sensitivity = () -> true;

    public CameraEffectsManager(EventBus bus, Minecraft minecraft) {
        this.minecraft = minecraft;
        fov = bus.subscribe(FovEvent.class, -100, false, event -> {
            if (minecraft.player == null || minecraft.level == null || !minecraft.player.isAlive()) {
                clear(); return;
            }
            amount.animateTo(owner != null && gameplay() ? strength.getAsDouble() : 1);
            event.applyMagnification(magnification());
        });
        input = bus.subscribe(MouseRotationEvent.class, 200, false, event -> {
            if (sensitivity.getAsBoolean()) { event.scaleInput(1 / magnification()); }
        });
    }
    public boolean gameplay() {
        return minecraft.player != null && minecraft.player.isAlive() && minecraft.level != null
                && minecraft.gui.screen() == null && minecraft.gui.overlay() == null && minecraft.isWindowActive();
    }
    public void request(Object token, DoubleSupplier strength, BooleanSupplier sensitivity) {
        owner = Objects.requireNonNull(token); this.strength = Objects.requireNonNull(strength);
        this.sensitivity = Objects.requireNonNull(sensitivity); amount.animateTo(strength.getAsDouble());
    }
    public void release(Object token) {
        if (owner == token) { owner = null; amount.animateTo(1); }
    }
    public double magnification() { return Math.max(1, amount.getValue()); }
    public void clear() { owner = null; amount.reset(1); }
    @Override public void close() { clear(); fov.close(); input.close(); }
}
