package dev.riserecode.ui.animation;

import dev.riserecode.ui.theme.Theme;
import dev.riserecode.ui.theme.ThemeManager;

/** Per-control spring. Retargeting preserves both position and velocity; actions never wait. */
public final class ButtonFeedback {
    private double value, velocity, age;
    private boolean running, held;
    private String appearanceId="";
    private static final java.util.Set<ButtonFeedback> INSTANCES=java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    public ButtonFeedback(){INSTANCES.add(this);}
    public boolean isHeld(){return held;}
    public static void releaseAll(){for(var feedback:java.util.List.copyOf(INSTANCES)){feedback.release();}}
    public static void cancelAll(){for(var feedback:java.util.List.copyOf(INSTANCES)){feedback.reset();}}
    public void press(){pressAt(System.nanoTime());}
    public void pressAt(long now){appearanceId=dev.riserecode.ui.theme.ModuleAppearance.context();sample(now);held=true;running=true;age=0;previous=now;}
    public void release(){releaseAt(System.nanoTime());}
    public void releaseAt(long now){if(!held){return;}sample(now);held=false;age=dev.riserecode.ui.theme.ModuleAppearance.call(appearanceId,()->Theme.number("Press Timing",.32)*Theme.number("Press Duration",.2)/.2*.22);previous=now;}

    private long previous;

    public void activate() { activateAt(System.nanoTime()); }
    public void activateAt(long now) {
        appearanceId=dev.riserecode.ui.theme.ModuleAppearance.context();sample(now);held=false;
        age = 0;
        running = true;
        previous = now;
    }
    public void reset() { value = velocity = age = 0; running = false; held=false; previous = 0; }
    public double displacement() { return value; }
    public double velocity() { return velocity; }

    /** Exact damped oscillator steps, split at the compression/release boundary. */
    public void advance(double seconds, double duration, double rebound) {
        if (!Double.isFinite(seconds) || seconds < 0 || !Double.isFinite(duration) || !Double.isFinite(rebound)) {
            throw new IllegalArgumentException("Invalid spring timing");
        }
        if (duration <= 0) { reset(); return; }
        if (!running || seconds == 0) { return; }
        double compressionEnd = duration * .22;
        if(held){integrate(seconds,1,5/compressionEnd,1);age+=seconds;return;}
        if (age < compressionEnd) {
            double step = Math.min(seconds, compressionEnd - age);
            integrate(step, 1, 5 / compressionEnd, 1);
            age += step; seconds -= step;
        }
        if (seconds > 0) {
            double logarithm = Math.log(Math.clamp(rebound, .001, .98));
            double damping = rebound <= 0 ? 1 : -logarithm / Math.sqrt(Math.PI * Math.PI + logarithm * logarithm);
            integrate(seconds, 0, 7 / (damping * duration * .78), damping);
            age += seconds;
        }
        if (age >= duration && Math.abs(value) < .001 && Math.abs(velocity) * duration < .01) { reset(); }
    }
    private void integrate(double dt, double target, double frequency, double damping) {
        double offset = value - target, decay = Math.exp(-damping * frequency * dt);
        if (damping >= 1) {
            double coefficient = velocity + frequency * offset;
            value = target + (offset + coefficient * dt) * decay;
            velocity = (velocity - frequency * coefficient * dt) * decay;
        } else {
            double w = frequency * Math.sqrt(1 - damping * damping), coefficient = (velocity + damping * frequency * offset) / w;
            double cosine = Math.cos(w * dt), sine = Math.sin(w * dt);
            double position = offset * cosine + coefficient * sine;
            value = target + decay * position;
            velocity = decay * (-damping * frequency * position - offset * w * sine + coefficient * w * cosine);
        }
    }
    private void sample() { sample(System.nanoTime()); }
    private void sample(long now) { dev.riserecode.ui.theme.ModuleAppearance.with(appearanceId,()->sampleScoped(now)); }
    private void sampleScoped(long now) {
        double duration = Theme.number("Press Timing", .32) * Theme.number("Press Duration", .2) / .2;
        if (Theme.bool("Reduced Motion", false) || duration <= 0 || Theme.number("Press Strength", 1) == 0
                || Theme.number("Press Compression", .06) == 0 || Animation.scale(ThemeManager.Channel.PRESS) == 0) { reset(); return; }
        if (previous != 0) { advance(Math.max(0, (now - previous) * 1e-9) * Theme.number("Animation Speed", 1), duration, Theme.number("Press Rebound", .28)); }
        previous = now;
    }
    public double scale() {
        sample();
        return dev.riserecode.ui.theme.ModuleAppearance.call(appearanceId,()->Math.clamp(1 - value * Theme.number("Press Compression", .06) * Theme.number("Press Strength", 1)
                * Animation.scale(ThemeManager.Channel.PRESS), .1, 2.0));
    }
    /** Standalone sampled response used by diagnostics. */
    public static double response(double progress, double rebound) {
        if (progress <= 0 || progress >= 1) { return 0; }
        var spring = new ButtonFeedback(); spring.running = true; spring.advance(progress, 1, rebound); return spring.value;
    }
}
