package dev.riserecode.map;

import com.google.gson.JsonObject;
import dev.riserecode.config.JsonStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Filesystem boundary only; MapService owns scheduling, snapshots and acknowledgments. */
final class WaypointFiles {
    private WaypointFiles() { }

    static JsonObject read(Path path) throws IOException {
        return Files.isRegularFile(path) ? JsonStorage.read(path, 8 * 1024 * 1024) : new JsonObject();
    }

    static WaypointStore readDeathPage(Path path) throws IOException {
        var history = new WaypointStore();
        history.load(JsonStorage.read(path, 2 * 1024 * 1024));
        return history;
    }

    static void write(Path path, JsonObject snapshot, Map<Integer, JsonObject> archives) throws IOException {
        // Archive pages precede the index that makes them reachable. Owners acknowledge only
        // after this entire operation succeeds; failures leave their immutable snapshot retained.
        for (var archive : archives.entrySet()) {
            JsonStorage.write(path.getParent().resolve("deaths").resolve(archive.getKey() + ".json"), archive.getValue());
        }
        JsonStorage.write(path, snapshot);
    }
}
