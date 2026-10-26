package dev.riserecode.ui.animation;

import dev.riserecode.util.MathUtil;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * A render-thread animation with explicit delta time, measured in seconds.
 * Retargeting starts at the current rendered value, preserving position.
 * UI instances are scheduled centrally; ordinary instances accept explicit updates.
 */
public final class Animation {
    public enum State {
        IDLE, RUNNING, PAUSED, FINISHED
    }

    private static DoubleSupplier uiSpeed = () -> 1;
    private static Supplier<MathUtil.Easing> uiCurve = () -> null;
    private boolean themed;
    private String appearanceId="";
    private static dev.riserecode.ui.theme.ThemeManager channels;
    private dev.riserecode.ui.theme.ThemeManager.Channel channel;
    private double configuredDuration;
    private DoubleSupplier durationOverride;
    public Animation timing(DoubleSupplier supplier){durationOverride=supplier;return this;}
    public static void configureChannels(dev.riserecode.ui.theme.ThemeManager manager) { channels = manager; }
    public static Animation ui(dev.riserecode.ui.theme.ThemeManager.Channel channel, double initial, double duration, MathUtil.Easing easing) {
        return ui(initial, duration, easing).channel(channel);
    }
    public Animation channel(dev.riserecode.ui.theme.ThemeManager.Channel value) { channel = value; return this; }
    public static double movement(dev.riserecode.ui.theme.ThemeManager.Channel channel) { return channels == null ? 1 : channels.bool("Reduced Motion") ? 0 : channels.channelNumber(channel, "Movement"); }
    public static double scale(dev.riserecode.ui.theme.ThemeManager.Channel channel) { return channels == null ? 1 : channels.bool("Reduced Motion") ? 0 : channels.channelNumber(channel, "Scale"); }
    private double selectedDuration() { if(themed && channels != null && channels.bool("Reduced Motion")){return 0;} if(durationOverride!=null){return Math.max(0,durationOverride.getAsDouble());} return themed && channels != null && channels.bool("Reduced Motion") ? 0 : themed && channel != null && channels != null ? channels.channelNumber(channel, "Duration") : durationSeconds; }

    private MathUtil.Easing activeCurve;
    private double segmentDuration;
    public static void configureUi(DoubleSupplier speed, Supplier<MathUtil.Easing> curve) {
        uiSpeed = Objects.requireNonNull(speed); uiCurve = Objects.requireNonNull(curve);
    }
    /** Ordinary animations stay deterministic; only UI instances adopt theme preferences. */
    public static Animation ui(double initialValue, double duration, MathUtil.Easing easing) {
        Animation result = new Animation(initialValue, duration, easing); result.themed = true; return result;
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
        activeCurve = easing; segmentDuration = durationSeconds;
        value = startValue = targetValue = initialValue;
    }

    /** Repeated requests for the same target do not restart the animation. */
    public Animation animateTo(double target) {
        if(themed){appearanceId=dev.riserecode.ui.theme.ModuleAppearance.context();}
        requireFinite(target, "Target");
        if (!Double.isFinite(target - value)) {
            throw new IllegalArgumentException("Animation range must be finite");
        }
        if (target == targetValue) {
            return this;
        }
        startValue = value;
        configuredDuration = selectedDuration(); segmentDuration = configuredDuration; activeCurve = selectedCurve();
        targetValue = target;
        elapsedSeconds = 0.0;
        state = State.RUNNING;
        if (themed) { AnimationManager.ui().register(this); }
        if (segmentDuration == 0.0 || startValue == targetValue) {
            finish();
        }
        return this;
    }

    public double update(double deltaSeconds) {
        requireFinite(deltaSeconds, "Delta time");
        if (deltaSeconds < 0) { throw new IllegalArgumentException("Delta time cannot be negative"); }
        if (themed && AnimationManager.ui().isDriving()) { return value; }
        return advance(deltaSeconds);
    }

    double advance(double deltaSeconds) {return dev.riserecode.ui.theme.ModuleAppearance.call(appearanceId,()->advanceScoped(deltaSeconds));}
    private double advanceScoped(double deltaSeconds) {
        requireFinite(deltaSeconds, "Delta time");
        if (deltaSeconds < 0.0) {
            throw new IllegalArgumentException("Delta time cannot be negative");
        }
        if (state != State.RUNNING) {
            return value;
        }
        double duration = selectedDuration();
        if (duration != configuredDuration) {
            double remaining = segmentDuration == 0 ? 0 : Math.max(0, 1 - getProgress());
            configuredDuration = duration; segmentDuration = duration * remaining; elapsedSeconds = 0; startValue = value;
            if (segmentDuration == 0) { finish(); return value; }
        }
        MathUtil.Easing selected = selectedCurve();
        if (activeCurve != selected) {
            segmentDuration = Math.max(0, segmentDuration - elapsedSeconds);
            startValue = value; elapsedSeconds = 0; activeCurve = selected;
        }
        double speed = themed ? Math.clamp(uiSpeed.getAsDouble(), 0.1, 40) : 1;
        elapsedSeconds = Math.min(segmentDuration, elapsedSeconds + deltaSeconds * speed);
        // Avoid an extra frame caused only by accumulated floating-point error.
        double tolerance = Math.min(4.0 * Math.ulp(segmentDuration), segmentDuration * 1.0e-12);
        if (segmentDuration - elapsedSeconds <= tolerance) {
            finish();
        } else {
            value = MathUtil.lerp(startValue, targetValue, activeCurve.apply(getProgress()));
        }
        return value;
    }

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            if (themed) { AnimationManager.ui().unregister(this); }
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            if (themed) { AnimationManager.ui().register(this); }
        }
    }

    public void finish() {
        value = targetValue;
        elapsedSeconds = segmentDuration;
        state = State.FINISHED;
        if (themed) { AnimationManager.ui().unregister(this); }
    }

    /** Immediately establishes a new idle value and discards the old target. */
    public void reset(double initialValue) {
        requireFinite(initialValue, "Initial value");
        value = startValue = targetValue = initialValue;
        elapsedSeconds = 0.0;
        state = State.IDLE;
        if (themed) { AnimationManager.ui().unregister(this); }
    }

    public double getValue() {
        return value;
    }

    public double getTarget() {
        return targetValue;
    }

    public double getProgress() {
        if (segmentDuration == 0.0) {
            return state == State.FINISHED ? 1.0 : 0.0;
        }
        return elapsedSeconds / segmentDuration;
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

    private MathUtil.Easing selectedCurve() {
        MathUtil.Easing selected = themed && channel != null && channels != null ? channels.channelCurve(channel) : themed ? uiCurve.get() : null;
        return selected == null ? easing : selected;
    }
    private static void requireFinite(double value, String label) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }
}
