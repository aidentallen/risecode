package dev.riserecode;

import dev.riserecode.event.Event;
import dev.riserecode.event.EventBus;
import dev.riserecode.module.Category;
import dev.riserecode.module.Module;
import dev.riserecode.setting.BooleanSetting;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.setting.DoubleSetting;
import dev.riserecode.setting.EnumSetting;
import dev.riserecode.setting.Setting;
import dev.riserecode.ui.animation.Animation;
import dev.riserecode.util.MathUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free behavioral checks; run with bash scripts/verify.sh. */
public final class FoundationChecks {
    private static int assertions;

    private FoundationChecks() {
    }

    public static void main(String[] args) throws Exception {
        checkEasing();
        checkAnimation();
        checkEventDispatch();
        checkCancellationAndFailures();
        checkConcurrentRegistry();
        checkSettings();
        checkModuleLifecycle();
        System.out.println("Foundation checks passed (" + assertions + " assertions).");
    }

    private static void checkEasing() {
        for (MathUtil.Easing easing : MathUtil.Easing.values()) {
            near(0.0, easing.apply(0.0), "easing start");
            near(1.0, easing.apply(1.0), "easing end");
            near(0.0, easing.apply(-2.0), "easing lower clamp");
            near(1.0, easing.apply(2.0), "easing upper clamp");
        }
        near(0.9375, MathUtil.easeOutQuart(0.5), "quart midpoint");
        near(0.0625, MathUtil.easeInOutCubic(0.25), "cubic first half");
        near(0.9375, MathUtil.easeInOutCubic(0.75), "cubic second half");
        check(MathUtil.easeOutElastic(0.4) > 1.0, "elastic overshoot is retained");
        near(12.0, MathUtil.lerp(0.0, 10.0, 1.2), "interpolation permits overshoot");
        expect(IllegalArgumentException.class, () -> MathUtil.easeOutQuart(Double.NaN));
    }

    private static void checkAnimation() {
        Animation coarse = new Animation(0.0, 1.0, MathUtil.Easing.OUT_QUART).animateTo(1.0);
        Animation fine = new Animation(0.0, 1.0, MathUtil.Easing.OUT_QUART).animateTo(1.0);
        coarse.update(0.5);
        for (int frame = 0; frame < 5; frame++) {
            fine.animateTo(1.0);
            fine.update(0.1);
        }
        near(coarse.getValue(), fine.getValue(), "frame-rate independent midpoint");
        check(fine.getState() == Animation.State.RUNNING, "repeated target keeps running");
        fine.pause();
        double paused = fine.getValue();
        near(paused, fine.update(10.0), "pause preserves current value");
        fine.resume();
        for (int frame = 0; frame < 5; frame++) {
            fine.update(0.1);
        }
        near(1.0, fine.getValue(), "exact endpoint");
        check(fine.getState() == Animation.State.FINISHED, "rounding cannot delay completion");
        fine.animateTo(0.0);
        fine.update(0.25);
        double beforeRetarget = fine.getValue();
        fine.animateTo(2.0);
        near(beforeRetarget, fine.getValue(), "retarget has no position jump");
        fine.update(100.0);
        near(2.0, fine.getValue(), "large delta finishes at target");
        fine.reset(0.25);
        check(fine.getState() == Animation.State.IDLE, "reset returns to idle");
        near(0.0, fine.getProgress(), "reset clears progress");
        Animation instant = new Animation(0.0, 0.0, MathUtil.Easing.OUT_QUART).animateTo(1.0);
        near(1.0, instant.getValue(), "zero duration completes immediately");
        check(instant.getState() == Animation.State.FINISHED, "zero duration state");
        Animation elastic = new Animation(0.0, 1.0, MathUtil.Easing.OUT_ELASTIC).animateTo(1.0);
        check(elastic.update(0.4) > 1.0, "animation preserves bounce");
        expect(IllegalArgumentException.class, () -> fine.update(-0.01));
        expect(IllegalArgumentException.class, () -> fine.update(Double.NaN));
        expect(IllegalArgumentException.class, () -> fine.animateTo(Double.POSITIVE_INFINITY));
        expect(IllegalArgumentException.class,
                () -> new Animation(0.0, -1.0, MathUtil.Easing.OUT_QUART));
    }

    private static void checkEventDispatch() {
        EventBus bus = strictBus();
        List<String> order = new ArrayList<>();
        EventBus.Subscription first = bus.subscribe(UpdateEvent.class, 0, false, e -> order.add("first"));
        bus.subscribe(Event.class, 10, false, e -> order.add("base"));
        bus.subscribe(UpdateEvent.class, 0, false, e -> order.add("second"));
        UpdateEvent event = new UpdateEvent();
        check(bus.post(event) == event, "post returns original event");
        check(order.equals(List.of("base", "first", "second")), "priority and stable order");
        first.close();
        first.close();
        check(!first.isActive(), "subscription close is idempotent");
        order.clear();
        bus.post(new UpdateEvent());
        check(order.equals(List.of("base", "second")), "closed listener leaves cached dispatch");
        check(bus.getListenerCount() == 2, "listener count reflects close");

        EventBus mutating = strictBus();
        AtomicInteger removedCalls = new AtomicInteger();
        EventBus.Subscription removed = mutating.subscribe(UpdateEvent.class, e -> removedCalls.incrementAndGet());
        mutating.subscribe(UpdateEvent.class, 20, false, e -> removed.close());
        mutating.post(new UpdateEvent());
        check(removedCalls.get() == 0, "close during dispatch skips later callback");

        EventBus growing = strictBus();
        AtomicInteger addedCalls = new AtomicInteger();
        EventBus.Subscription installer = growing.subscribe(UpdateEvent.class,
                e -> growing.subscribe(UpdateEvent.class, next -> addedCalls.incrementAndGet()));
        growing.post(new UpdateEvent());
        check(addedCalls.get() == 0, "new subscription waits for next dispatch");
        installer.close();
        growing.post(new UpdateEvent());
        check(addedCalls.get() == 1, "new registry invalidates dispatch cache");
    }

    private static void checkCancellationAndFailures() {
        EventBus bus = strictBus();
        List<String> calls = new ArrayList<>();
        bus.subscribe(PacketEvent.class, 10, false, e -> {
            calls.add("cancel");
            e.cancel();
        });
        bus.subscribe(PacketEvent.class, 0, false, e -> calls.add("skip"));
        bus.subscribe(Event.class, -10, true, e -> calls.add("observe"));
        PacketEvent packet = bus.post(new PacketEvent());
        check(packet.isCancelled(), "packet cancellation reaches hook");
        check(calls.equals(List.of("cancel", "observe")), "cancelled event filters listeners");
        expect(IllegalStateException.class, () -> new UpdateEvent().cancel());

        List<RuntimeException> errors = new ArrayList<>();
        EventBus resilient = new EventBus((event, failure) -> errors.add(failure));
        AtomicInteger laterCalls = new AtomicInteger();
        resilient.subscribe(UpdateEvent.class, e -> {
            throw new IllegalArgumentException("intentional listener failure");
        });
        resilient.subscribe(UpdateEvent.class, e -> laterCalls.incrementAndGet());
        resilient.post(new UpdateEvent());
        check(errors.size() == 1, "listener failure is reported");
        check(laterCalls.get() == 1, "listener failure does not stop later callbacks");
    }

    private static void checkConcurrentRegistry() throws Exception {
        EventBus bus = strictBus();
        AtomicInteger persistentCalls = new AtomicInteger();
        bus.subscribe(UpdateEvent.class, e -> persistentCalls.incrementAndGet());
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var posting = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 2_000; i++) {
                    bus.post(new UpdateEvent());
                }
                return null;
            });
            var changing = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 500; i++) {
                    EventBus.Subscription temporary = bus.subscribe(Event.class, e -> { });
                    temporary.close();
                }
                return null;
            });
            start.countDown();
            posting.get();
            changing.get();
        }
        check(persistentCalls.get() == 2_000, "concurrent registry changes keep persistent listeners");
        check(bus.getListenerCount() == 1, "concurrent temporary subscriptions all removed");
        bus.post(new UpdateEvent());
        check(persistentCalls.get() == 2_001, "final registry cache remains valid");
    }

    private static void checkSettings() {
        BooleanSetting show = new BooleanSetting("show", "Show advanced settings", false);
        DoubleSetting scale = new DoubleSetting("scale", "HUD scale", 1.0, 0.5, 2.0);
        scale.setVisibleWhen(show::getValue);
        check(!scale.isVisible(), "visibility reads current predicate");
        show.toggle();
        check(scale.isVisible(), "visibility changes with controlling setting");
        scale.setValue(1.5);
        expect(IllegalArgumentException.class, () -> scale.setValue(3.0));
        near(1.5, scale.getValue(), "failed validation preserves previous value");
        expect(IllegalArgumentException.class, () -> scale.setValue(Double.NaN));
        expect(IllegalArgumentException.class,
                () -> new DoubleSetting("bad", "Invalid default", 3.0, 0.0, 1.0));
        expect(IllegalArgumentException.class,
                () -> new DoubleSetting("bad", "Invalid bounds", 0.0, 2.0, 1.0));
        scale.reset();
        near(1.0, scale.getValue(), "reset restores typed default");
        expect(NullPointerException.class, () -> show.setValue(null));
        EnumSetting<MathUtil.Easing> easing = new EnumSetting<>("easing", "Panel curve", MathUtil.Easing.OUT_QUART);
        check(easing.getOptions().size() == 5, "enum exposes all options");
        easing.setValue(MathUtil.Easing.IN_OUT_CUBIC);
        easing.reset();
        check(easing.getValue() == MathUtil.Easing.OUT_QUART, "enum reset");
        expect(UnsupportedOperationException.class, () -> easing.getOptions().clear());
        ColorSetting color = new ColorSetting("accent", "Active color", ColorSetting.argb(128, 59, 130, 246));
        check(color.getAlpha() == 128 && color.getRed() == 59
                && color.getGreen() == 130 && color.getBlue() == 246, "ARGB round trip");
        expect(IllegalArgumentException.class, () -> ColorSetting.argb(256, 0, 0, 0));
    }

    private static void checkModuleLifecycle() {
        EventBus bus = strictBus();
        TestModule module = new TestModule(bus);
        check(!module.isEnabled() && module.getKeybind() == Module.UNBOUND, "module defaults");
        check(module.getSettings().size() == 5, "module owns typed setting list");
        expect(UnsupportedOperationException.class, () -> module.getSettings().clear());
        expect(IllegalArgumentException.class,
                () -> module.addSetting(new BooleanSetting("visible", "Duplicate", false)));
        expect(IllegalArgumentException.class, () -> module.setKeybind(-2));
        module.setKeybind(344);
        check(module.getKeybind() == 344, "keybind assignment");
        module.setEnabled(true);
        module.setEnabled(true);
        check(module.enables == 1 && bus.getListenerCount() == 1, "enable is idempotent");
        check(module.updates == 0, "handlers wait for enable to finish");
        bus.post(new UpdateEvent());
        check(module.updates == 1, "enabled module receives event");
        module.setEnabled(false);
        bus.post(new UpdateEvent());
        check(module.updates == 1 && bus.getListenerCount() == 0, "disable releases listeners");
        module.toggle();
        check(bus.getListenerCount() == 1, "re-enable registers once");
        module.toggle();

        module.failEnable = true;
        module.failDisable = true;
        RuntimeException enableFailure = expect(IllegalArgumentException.class, () -> module.setEnabled(true));
        check(enableFailure.getSuppressed().length == 1, "rollback preserves enable failure");
        check(!module.isEnabled() && bus.getListenerCount() == 0, "failed enable releases listeners");
        module.failEnable = false;
        module.failDisable = false;
        module.setEnabled(true);
        module.failDisable = true;
        expect(IllegalStateException.class, () -> module.setEnabled(false));
        check(!module.isEnabled() && bus.getListenerCount() == 0, "failed disable still removes listeners");

        EventBus reentrantBus = strictBus();
        Module reentrant = new Module("recursive", "Reject lifecycle recursion", Category.UTILITY, reentrantBus) {
            @Override
            protected void onEnable() {
                toggle();
            }
        };
        expect(IllegalStateException.class, () -> reentrant.setEnabled(true));
        check(!reentrant.isEnabled(), "lifecycle recursion rolls back cleanly");
    }

    private static EventBus strictBus() {
        return new EventBus((event, failure) -> {
            throw new AssertionError("Unexpected listener failure", failure);
        });
    }

    private static void near(double expected, double actual, String message) {
        check(Double.isFinite(actual) && Math.abs(expected - actual) < 1.0e-10,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static <T extends Throwable> T expect(Class<T> type, Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                return type.cast(failure);
            }
            throw new AssertionError("Expected " + type.getName() + ", got " + failure, failure);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static final class UpdateEvent extends Event {
    }

    private static final class PacketEvent extends Event {
        private PacketEvent() {
            super(true);
        }
    }

    private static final class TestModule extends Module {
        private final EventBus bus;
        private int enables;
        private int updates;
        private boolean failEnable;
        private boolean failDisable;

        private TestModule(EventBus bus) {
            super("test", "Test lifecycle cleanup", Category.UTILITY, bus);
            this.bus = bus;
            register(new BooleanSetting("visible", "Test flag", true));
        }

        private void addSetting(Setting<?> setting) {
            register(setting);
        }

        @Override
        protected void onEnable() {
            enables++;
            listen(UpdateEvent.class, event -> updates++);
            bus.post(new UpdateEvent());
            if (failEnable) {
                throw new IllegalArgumentException("intentional enable failure");
            }
        }

        @Override
        protected void onDisable() {
            if (failDisable) {
                throw new IllegalStateException("intentional disable failure");
            }
        }
    }
}
