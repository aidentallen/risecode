package dev.riserecode.map;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.LongSupplier;

/** Immutable control write retained by its owner until a successful ordered acknowledgement. */
public final class RetainedWrite {
    @FunctionalInterface public interface Action { void run() throws IOException; }
    private final Action action;
    private final LongSupplier clock;
    private CompletableFuture<Void> inFlight;
    private Throwable failure;
    private long retryAt;
    public RetainedWrite(Action action) { this(action, System::nanoTime); }
    public RetainedWrite(Action action, LongSupplier clock) { this.action = action; this.clock = clock; }
    public boolean progress(Executor executor) {
        if (inFlight != null) {
            if (!inFlight.isDone()) { return false; }
            try { inFlight.join(); failure = null; return true; }
            catch (java.util.concurrent.CompletionException failed) { failure = failed.getCause(); inFlight = null; retryAt = clock.getAsLong()+250_000_000; return false; }
        }
        if (clock.getAsLong() < retryAt) { return false; }
        var result = new CompletableFuture<Void>();
        try {
            executor.execute(() -> { try { action.run(); result.complete(null); } catch (IOException | RuntimeException error) { result.completeExceptionally(error); } });
            inFlight = result;
        } catch (RejectedExecutionException full) { failure = full; }
        return false;
    }
    public Throwable failure() { return failure; }
}
