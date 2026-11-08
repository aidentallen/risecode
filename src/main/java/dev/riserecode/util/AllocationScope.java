package dev.riserecode.util;

import java.util.ArrayDeque;
import java.util.function.Consumer;

/** LIFO rollback for partially constructed native/GPU owners; commit transfers ownership. */
public final class AllocationScope implements AutoCloseable {
    private final ArrayDeque<Runnable> cleanup = new ArrayDeque<>();
    public <T> T own(T value, Consumer<T> release) { cleanup.push(() -> release.accept(value)); return value; }
    public void commit() { cleanup.clear(); }
    @Override public void close() {
        RuntimeException failure = null;
        while (!cleanup.isEmpty()) {
            try { cleanup.pop().run(); }
            catch (RuntimeException error) { if (failure == null) { failure = error; } else { failure.addSuppressed(error); } }
        }
        if (failure != null) { throw failure; }
    }
}
