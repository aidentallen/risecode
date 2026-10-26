package dev.riserecode.event;

/** A client resource reload failed; a feature owning that reload may recover it. */
public final class ResourceReloadFailureEvent extends Event {
    private final Throwable failure;
    public ResourceReloadFailureEvent(Throwable failure){super(true);this.failure=java.util.Objects.requireNonNull(failure);}
    public Throwable failure(){return failure;}
}
