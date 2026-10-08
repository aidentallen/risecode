package dev.riserecode.event;

/**
 * Base for game-hook events. Instances are confined to the posting thread.
 * Update/render events normally use the default, non-cancellable constructor;
 * a packet event can explicitly opt in with super(true).
 */
public abstract class Event {
    private final boolean cancellable;
    private boolean cancelled;

    protected Event() {
        this(false);
    }

    protected Event(boolean cancellable) {
        this.cancellable = cancellable;
    }

    public final boolean isCancellable() {
        return cancellable;
    }

    public final boolean isCancelled() {
        return cancelled;
    }

    public final void cancel() {
        setCancelled(true);
    }

    public final void setCancelled(boolean cancelled) {
        if (cancelled && !cancellable) {
            throw new IllegalStateException("This event cannot be cancelled");
        }
        this.cancelled = cancelled;
    }
}
