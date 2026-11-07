package dev.riserecode.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/** Reclaims only owned UUID generations; active and rollback paths must be retained. */
public final class GenerationRetention {
    private GenerationRetention() { }
    public static void prune(Path directory, Set<Path> retained) throws IOException {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory)) { return; }
        Set<Path> keep = retained.stream().map(p -> p.toAbsolutePath().normalize()).collect(java.util.stream.Collectors.toSet());
        try (var entries = Files.list(directory)) {
            for (Path entry : entries.toList()) {
                try { if (!UUID.fromString(entry.getFileName().toString()).toString().equals(entry.getFileName().toString())) { continue; } }
                catch (IllegalArgumentException other) { continue; }
                if (keep.contains(entry.toAbsolutePath().normalize())) { continue; }
                // Files.walk does not follow symlinks. Deleting a link never visits its target.
                try (var files = Files.walk(entry)) {
                    for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) { Files.deleteIfExists(file); }
                }
            }
        }
    }
}
