package dev.riserecode;

/** Activated only around direct test calls, never during normal frames. */
public final class LifetimeFaults {
    public static int mode;
    public static final IllegalStateException FAILURE=new IllegalStateException("Expected renderer fixture failure");
    private LifetimeFaults(){ }
}
