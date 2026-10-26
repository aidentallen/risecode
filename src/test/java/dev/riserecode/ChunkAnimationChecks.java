package dev.riserecode;

import dev.riserecode.config.ConfigManager;
import dev.riserecode.event.ChunkLifecycleEvent;
import dev.riserecode.event.ChunkRenderPolicyEvent;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.FogEvent;
import dev.riserecode.module.ModuleManager;
import dev.riserecode.module.modules.ChunkAnimationsModule;
import dev.riserecode.module.modules.NoRenderModule;
import dev.riserecode.render.ChunkMotionTracker;
import java.nio.file.Files;
import org.slf4j.LoggerFactory;

public final class ChunkAnimationChecks {
    private static int checks;
    private ChunkAnimationChecks() { }
    private static void check(boolean value, String message) { checks++; if (!value) { throw new AssertionError(message); } }
    private static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < 1e-5, message + " actual=" + actual); }
    public static void main(String[] args) throws Exception {
        long time = 1_000_000_000L;
        for (var direction : ChunkMotionTracker.Direction.values()) {
            for (var curve : ChunkMotionTracker.Curve.values()) {
                var tracker = new ChunkMotionTracker();
                var settings = new ChunkMotionTracker.Settings(direction, 8, 0.5, curve);
                tracker.loaded(ChunkMotionTracker.key(-17, 123)); tracker.frame(time, settings);
                near(tracker.offset(-17, 123), settings.initialOffset(), "Pending initial offset");
                tracker.frame(time + 10_000_000_000L, settings);
                near(tracker.drawable(-17, 123), settings.initialOffset(), "Clock starts on drawable, not packet");
                check(tracker.starts() == 1, "Start exactly once");
                tracker.drawable(-17, 123); tracker.loaded(ChunkMotionTracker.key(-17, 123));
                check(tracker.starts() == 1, "Sections and duplicate packets share clock");
                tracker.frame(time + 10_250_000_000L, new ChunkMotionTracker.Settings(ChunkMotionTracker.Direction.DOWN, 64, 3, ChunkMotionTracker.Curve.IN_OUT_CUBIC));
                near(tracker.offset(-17, 123), settings.initialOffset() * (1 - curve.apply(0.5)), "Captured settings and half progress");
                near(tracker.offset(123, -17), 0, "Other chunk stationary");
                tracker.frame(time + 10_500_000_000L, settings);
                near(tracker.offset(-17, 123), 0, "Exact final native position");
                check(tracker.pendingCount() == 0, "Completed flight retired");
                tracker.loaded(ChunkMotionTracker.key(-17, 123)); tracker.frame(time + 11_000_000_000L, settings);
                check(tracker.pendingCount() == 0, "Resident mesh updates never restart");
                tracker.unloaded(ChunkMotionTracker.key(-17, 123)); tracker.loaded(ChunkMotionTracker.key(-17, 123)); tracker.frame(time, settings);
                near(tracker.drawable(-17, 123), settings.initialOffset(), "Unloaded then reloaded animates");
                tracker.unloaded(ChunkMotionTracker.key(-17, 123)); near(tracker.offset(-17, 123), 0, "Unload clears current snapshot");
                tracker.clear(); check(tracker.pendingCount() == 0, "World lifecycle clears flights");
            }
        }
        var bus = new EventBus(); var chunks = new ChunkAnimationsModule(bus); var noRender = new NoRenderModule(bus);
        check(!chunks.isEnabled() && chunks.getKeybind() == -1 && chunks.direction.getValue() == ChunkMotionTracker.Direction.UP
                && chunks.distance.getValue() == 8 && chunks.duration.getValue() == 0.5 && chunks.easing.getValue() == ChunkMotionTracker.Curve.OUT_QUART, "Module defaults");
        check(noRender.chunkFade.getValue() && noRender.chunkFade.isVisible(), "Fade independent and visible under All Fog");
        check(!bus.post(new ChunkRenderPolicyEvent()).fadeSuppressed(), "Disabled modules preserve fade");
        noRender.allFog.setValue(false); noRender.setEnabled(true);
        check(bus.post(new ChunkRenderPolicyEvent()).fadeSuppressed(), "NoRender fade-only suppression");
        var fog = new net.minecraft.client.renderer.fog.FogData(); fog.environmentalEnd = 48;
        bus.post(new FogEvent(FogEvent.Type.WATER, fog)); near(fog.environmentalEnd, 48, "Fade-only preserves water fog");
        noRender.chunkFade.setValue(false); check(!bus.post(new ChunkRenderPolicyEvent()).fadeSuppressed(), "Fade toggle restores native policy");
        chunks.setEnabled(true);
        for (double travel : new double[] {chunks.distance.getMin(), chunks.distance.getMax()}) {
            for (double seconds : new double[] {chunks.duration.getMin(), chunks.duration.getMax()}) {
                chunks.distance.setValue(travel); chunks.duration.setValue(seconds);
                var boundary = bus.post(new ChunkRenderPolicyEvent());
                check(boundary.motion() == chunks.motion() && boundary.settings().distance() == travel && boundary.settings().duration() == seconds,
                        "Every exposed boundary constructs the runtime policy");
            }
        }
        chunks.distance.reset(); chunks.duration.reset();
        var policy = bus.post(new ChunkRenderPolicyEvent());
        check(policy.fadeSuppressed() && policy.motion() == chunks.motion(), "Movement automatically removes fade");
        bus.post(new ChunkLifecycleEvent(ChunkLifecycleEvent.Kind.LOAD, ChunkMotionTracker.key(2, 3)));
        chunks.motion().frame(time, policy.settings()); near(chunks.motion().drawable(2, 3), -8, "Bus lifecycle starts motion");
        bus.post(new ChunkLifecycleEvent(ChunkLifecycleEvent.Kind.RESET, 0)); check(chunks.motion().pendingCount() == 0, "Reset through bus");
        var manager = new ModuleManager(bus, LoggerFactory.getLogger("ChunkAnimationChecks")); manager.register(chunks); manager.register(noRender);
        var directory = Files.createTempDirectory("risecode-chunks-check-");
        try (var config = new ConfigManager(directory, manager, () -> true, ignored -> { }, LoggerFactory.getLogger("ChunkAnimationChecks"))) {
            chunks.direction.setValue(ChunkMotionTracker.Direction.DOWN); chunks.distance.setValue(12.5); chunks.duration.setValue(1.25);
            chunks.easing.setValue(ChunkMotionTracker.Curve.OUT_ELASTIC); manager.setKeybind(chunks, 295);
            check(config.save(), "Config save"); var saved = config.profileSnapshot();
            chunks.setEnabled(false); chunks.direction.setValue(ChunkMotionTracker.Direction.UP); noRender.chunkFade.setValue(true);
            config.load(); check(chunks.isEnabled() && chunks.direction.getValue() == ChunkMotionTracker.Direction.DOWN && chunks.distance.getValue() == 12.5
                    && chunks.duration.getValue() == 1.25 && chunks.easing.getValue() == ChunkMotionTracker.Curve.OUT_ELASTIC && chunks.getKeybind() == 295 && !noRender.chunkFade.getValue(), "Config round trip");
            chunks.setEnabled(false); config.applyProfile(saved); check(chunks.isEnabled() && chunks.motion().pendingCount() == 0, "Profile restores controls without stale flights");
        } finally {
            chunks.setEnabled(false); noRender.setEnabled(false);
            try (var files = Files.walk(directory)) { for (var file : files.sorted(java.util.Comparator.reverseOrder()).toList()) { Files.delete(file); } }
        }
        check(bus.getListenerCount() == 0 && !bus.post(new ChunkRenderPolicyEvent()).fadeSuppressed(), "Disable releases everything");
        System.out.println("Chunk animation checks passed (" + checks + " assertions).");
    }
}
