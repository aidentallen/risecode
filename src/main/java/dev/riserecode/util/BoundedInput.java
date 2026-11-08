package dev.riserecode.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Limits acquisition itself, including files that grow after opening or a size check. */
public final class BoundedInput {
    private BoundedInput() { }
    public static byte[] read(Path path, int limit) throws IOException {
        try (var input = Files.newInputStream(path)) { return read(input, limit); }
    }
    public static byte[] read(InputStream input, int limit) throws IOException {
        if (limit < 0 || limit == Integer.MAX_VALUE) { throw new IllegalArgumentException("Invalid byte limit"); }
        byte[] bytes = input.readNBytes(limit + 1);
        if (bytes.length > limit) { throw new IOException("Input exceeds " + limit + " byte limit"); }
        return bytes;
    }
}
