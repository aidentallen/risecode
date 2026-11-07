package dev.riserecode.seedcracker;

/** Client-thread scan quota. Suspension happens before a read, with cursors owned by each finder. */
public final class FinderBudget implements AutoCloseable {
    public static final class Yield extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private Yield() { super(null, null, false, false); }
    }
    private static final Yield YIELD = new Yield();
    private static final ThreadLocal<FinderBudget> CURRENT = new ThreadLocal<>();
    private final long deadline;
    private int remaining;
    public FinderBudget(int reads, long nanos) {
        if (CURRENT.get() != null || reads < 1 || nanos < 1) { throw new IllegalArgumentException("Invalid scan budget"); }
        remaining = reads; deadline = System.nanoTime() + nanos; CURRENT.set(this);
    }
    public static void read() {
        FinderBudget budget = CURRENT.get();
        if (budget != null && (budget.remaining-- <= 0 || System.nanoTime() >= budget.deadline)) { throw YIELD; }
    }
    @Override public void close() { CURRENT.remove(); }
}
