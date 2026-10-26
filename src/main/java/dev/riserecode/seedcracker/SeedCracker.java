package dev.riserecode.seedcracker;

import dev.riserecode.seedcracker.cracker.storage.DataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scoped bridge for vendored finder calls; installed and used only on the client thread. */
public final class SeedCracker {
    public static final Logger LOGGER = LoggerFactory.getLogger("riserecode-seedcracker");
    private static SeedCracker current;
    private final DataStorage data;
    private final Runnable rescan;
    public SeedCracker(DataStorage data, Runnable rescan) { this.data = data; this.rescan = rescan; }
    public static SeedCracker get() {
        if (current == null) { throw new IllegalStateException("SeedCracker collection is disabled"); }
        return current;
    }
    public static void bind(SeedCracker scope) { current = scope; }
    public DataStorage getDataStorage() { return data; }
    public void rescan() { rescan.run(); }
}
