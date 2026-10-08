package dev.riserecode.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Synchronous, typed event dispatch without reflection. Higher priority runs
 * first; equal priorities retain subscription order. Base-type listeners
 * receive subclasses. Cached dispatch arrays are rebuilt only after registry
 * changes. Callbacks execute on the posting thread, including network threads.
 *
 * Registry changes are thread-safe. Dispatch uses a snapshot: new listeners
 * begin on the next post; closed listeners are skipped unless already executing.
 * Runtime listener failures are reported and do not block remaining listeners.
 */
public final class EventBus {
    private static final System.Logger LOGGER = System.getLogger(EventBus.class.getName());
    private static final Comparator<Registration<?>> PRIORITY_ORDER =
            (left, right) -> Integer.compare(right.priority, left.priority);

    private final Object registryLock = new Object();
    private final BiConsumer<? super Event, ? super RuntimeException> errorHandler;
    private volatile Registry registry = new Registry(new Registration<?>[0]);

    public EventBus() {
        this((event, failure) -> LOGGER.log(System.Logger.Level.ERROR,
                "Listener failed for " + event.getClass().getName(), failure));
    }

    /** A custom error handler must return normally to continue dispatch. */
    public EventBus(BiConsumer<? super Event, ? super RuntimeException> errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
    }

    public <E extends Event> Subscription subscribe(Class<E> eventType, Consumer<? super E> listener) {
        return subscribe(eventType, 0, false, listener);
    }

    public <E extends Event> Subscription subscribe(Class<E> eventType, int priority,
            boolean receiveCancelled, Consumer<? super E> listener) {
        Registration<E> registration = new Registration<>(this,
                Objects.requireNonNull(eventType, "eventType"), priority, receiveCancelled,
                Objects.requireNonNull(listener, "listener"));
        synchronized (registryLock) {
            Registration<?>[] next = Arrays.copyOf(registry.listeners, registry.listeners.length + 1);
            next[next.length - 1] = registration;
            Arrays.sort(next, PRIORITY_ORDER);
            registry = new Registry(next);
        }
        return registration;
    }

    /** Returns the same event so a game hook can inspect cancellation. */
    public <E extends Event> E post(E event) {
        Objects.requireNonNull(event, "event");
        Registration<?>[] dispatch = registry.resolve(event.getClass());
        for (Registration<?> registration : dispatch) {
            if (!registration.active || (event.isCancelled() && !registration.receiveCancelled)) {
                continue;
            }
            try {
                registration.invoke(event);
            } catch (RuntimeException failure) {
                errorHandler.accept(event, failure);
            }
        }
        return event;
    }

    public int getListenerCount() {
        return registry.listeners.length;
    }

    private void unsubscribe(Registration<?> registration) {
        synchronized (registryLock) {
            if (!registration.active) {
                return;
            }
            registration.active = false;
            Registration<?>[] next = Arrays.stream(registry.listeners)
                    .filter(candidate -> candidate != registration)
                    .toArray(Registration<?>[]::new);
            registry = new Registry(next);
        }
    }

    public interface Subscription extends AutoCloseable {
        boolean isActive();

        /** Idempotent; does not throw checked exceptions. */
        @Override
        void close();
    }

    private static final class Registry {
        private final Registration<?>[] listeners;
        private final ConcurrentHashMap<Class<? extends Event>, Registration<?>[]> cache =
                new ConcurrentHashMap<>();

        private Registry(Registration<?>[] listeners) {
            this.listeners = listeners;
        }

        private Registration<?>[] resolve(Class<? extends Event> eventType) {
            Registration<?>[] cached = cache.get(eventType);
            if (cached != null) {
                return cached;
            }
            return cache.computeIfAbsent(eventType, type -> {
                var matching = new ArrayList<Registration<?>>();
                for (Registration<?> listener : listeners) {
                    if (listener.eventType.isAssignableFrom(type)) {
                        matching.add(listener);
                    }
                }
                return matching.toArray(Registration<?>[]::new);
            });
        }
    }

    private static final class Registration<E extends Event> implements Subscription {
        private final EventBus owner;
        private final Class<E> eventType;
        private final int priority;
        private final boolean receiveCancelled;
        private final Consumer<? super E> listener;
        private volatile boolean active = true;

        private Registration(EventBus owner, Class<E> eventType, int priority,
                boolean receiveCancelled, Consumer<? super E> listener) {
            this.owner = owner;
            this.eventType = eventType;
            this.priority = priority;
            this.receiveCancelled = receiveCancelled;
            this.listener = listener;
        }

        private void invoke(Event event) {
            listener.accept(eventType.cast(event));
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void close() {
            owner.unsubscribe(this);
        }
    }
}
