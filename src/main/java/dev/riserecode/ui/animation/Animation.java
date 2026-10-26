package dev.riserecode.ui.animation;

import dev.riserecode.util.MathUtil;
import java.util.Objects;

/**
 * A render-thread animation with explicit delta time, measured in seconds.
 * Retargeting starts at the current rendered value, preserving position.
 * Call update once per frame and read getValue freely between updates.
 */
public final class Animation {
    public enum State {
        IDLE, RUNNING, PAUSED, FINISHED
    }

    private final double durationSeconds;
    private final MathUtil.Easing easing;
    private double value;
    private double startValue;
    private double targetValue;
    private double elapsedSeconds;
    private State state = State.IDLE;

    public Animation(double initialValue, double durationSeconds, MathUtil.Easing easing) {
        requireFinite(initialValue, "Initial value");
        requireFinite(durationSeconds, "Duration");
        if (durationSeconds < 0.0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        this.durationSeconds = durationSeconds;
        this.easing = Objects.requireNonNull(easing, "easing");
        value = startValue = targetValue = initialValue;
    }

    /** Repeated requests for the same target do not restart the animation. */
    public Animation animateTo(double target) {
        requireFinite(target, "Target");
        if (!Double.isFinite(target - value)) {
            throw new IllegalArgumentException("Animation range must be finite");
        }
        if (target == targetValue) {
            return this;
        }
        startValue = value;
        targetValue = target;
        elapsedSeconds = 0.0;
        state = State.RUNNING;
        if (durationSeconds == 0.0 || startValue == targetValue) {
            finish();
        }
        return this;
    }

    public double update(double deltaSeconds) {
        requireFinite(deltaSeconds, "Delta time");
        if (deltaSeconds < 0.0) {
            throw new IllegalArgumentException("Delta time cannot be negative");
        }
        if (state != State.RUNNING) {
            return value;
        }
        elapsedSeconds = Math.min(durationSeconds, elapsedSeconds + deltaSeconds);
        // Avoid an extra frame caused only by accumulated floating-point error.
        double tolerance = Math.min(4.0 * Math.ulp(durationSeconds), durationSeconds * 1.0e-12);
        if (durationSeconds - elapsedSeconds <= tolerance) {
            finish();
        } else {
            value = MathUtil.lerp(startValue, targetValue, easing.apply(getProgress()));
        }
        return value;
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

    public void finish() {
        value = targetValue;
        elapsedSeconds = durationSeconds;
        state = State.FINISHED;
    }

    /** Immediately establishes a new idle value and discards the old target. */
    public void reset(double initialValue) {
        requireFinite(initialValue, "Initial value");
        value = startValue = targetValue = initialValue;
        elapsedSeconds = 0.0;
        state = State.IDLE;
    }

    public double getValue() {
        return value;
    }

    public double getTarget() {
        return targetValue;
    }

    public double getProgress() {
        if (durationSeconds == 0.0) {
            return state == State.FINISHED ? 1.0 : 0.0;
        }
        return elapsedSeconds / durationSeconds;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public State getState() {
        return state;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    private static void requireFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }
}
