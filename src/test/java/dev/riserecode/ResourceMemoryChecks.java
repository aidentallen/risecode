package dev.riserecode;

import dev.riserecode.resources.ResourceArchive;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Real inflation at the aggregate ceiling, using streamed input rather than giant fixture arrays. */
public final class ResourceMemoryChecks {
    private static final byte[] METADATA = "{\"pack\":{\"pack_format\":88,\"description\":\"memory boundary\"}}".getBytes(StandardCharsets.UTF_8);
    private ResourceMemoryChecks() { }

    private static byte[] archive(int decodedBytes) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("pack.mcmeta")); zip.write(METADATA); zip.closeEntry();
            byte[] chunk = new byte[32768];
            int remaining = decodedBytes - METADATA.length;
            for (int entry = 0; remaining > 0; entry++) {
                zip.putNextEntry(new ZipEntry("assets/probe/" + entry + ".bin"));
                int size = Math.min(64 * 1024 * 1024, remaining); remaining -= size;
                while (size > 0) { int count = Math.min(size, chunk.length); zip.write(chunk, 0, count); size -= count; }
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        // This budget is a user-visible resource policy, not an incidental parser implementation.
        if (ResourceArchive.MAX_DECODED_BYTES > 128 * 1024 * 1024) { throw new AssertionError("Decoded pack budget exceeds 128 MiB policy"); }
        byte[] exact = archive(ResourceArchive.MAX_DECODED_BYTES);
        long actual = ResourceArchive.read(exact).files().values().stream().mapToLong(bytes -> bytes.length).sum();
        if (actual != ResourceArchive.MAX_DECODED_BYTES) { throw new AssertionError("Exactly-at-budget archive was not retained intact"); }
        try {
            ResourceArchive.read(archive(ResourceArchive.MAX_DECODED_BYTES + 1));
            throw new AssertionError("Aggregate limit allowed an extra decoded byte");
        } catch (IOException rejected) {
            if (!rejected.getMessage().contains("memory budget")) { throw new AssertionError("Wrong rejection boundary", rejected); }
        }
        var status = java.nio.file.Path.of("/proc/self/status");
        if (java.nio.file.Files.isRegularFile(status)) {
            String peak = java.nio.file.Files.readAllLines(status).stream().filter(line -> line.startsWith("VmHWM:")).findFirst().orElseThrow();
            long kib = Long.parseLong(peak.split("\\s+")[1]);
            if (kib >= 1536L * 1024) { throw new AssertionError("Parser process peak RSS exceeded 1.5 GiB: " + peak); }
            System.out.println("Parser JVM peak RSS: " + kib + " KiB (Linux VmHWM; process-only, not combined Gradle/Minecraft)");
        }
        System.out.println("Resource memory checks passed (3 assertions); decoded=" + actual + ", compressed=" + exact.length
                + "; Linux peak RSS reported above when available; excludes Minecraft/GPU and concurrent pack composition");
    }
}
