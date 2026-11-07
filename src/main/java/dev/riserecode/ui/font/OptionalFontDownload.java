package dev.riserecode.ui.font;

import dev.riserecode.cosmetics.CosmeticsLibrary;
import dev.riserecode.net.BoundedBodyHandler;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.*;

/** Optional third-party pack is acquired only when selected, never bundled or fetched at startup. */
public final class OptionalFontDownload implements AutoCloseable {
    public static final URI SOURCE = URI.create("https://mediafilez.forgecdn.net/files/5605/880/qrafty's-capitalized-font.zip");
    public static final String SHA256 = "d5de138c0be92d785f77adb8c65d1ee8e12b24ec75683eb0485b38c336415ce7";
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> Thread.ofPlatform().daemon().name("risecode-optional-font").unstarted(r));
    @FunctionalInterface public interface Fetcher { byte[] get() throws IOException, InterruptedException; }
    private final Fetcher fetcher;
    public OptionalFontDownload() { this(OptionalFontDownload::http); }
    public OptionalFontDownload(Fetcher fetcher) { this.fetcher = java.util.Objects.requireNonNull(fetcher); }
    private CompletableFuture<Path> request;
    private boolean closed;
    private volatile String status = "Qraftys downloads when selected · using Inter until ready";
    public synchronized CompletableFuture<Path> request(Path directory) {
        if (closed) { return CompletableFuture.failedFuture(new IllegalStateException("Font downloader closed")); }
        if (request != null) { return request; }
        status = "Downloading Qraftys from CurseForge · using Inter until ready";
        request = CompletableFuture.supplyAsync(() -> {
            try {
                byte[] data = fetcher.get();
                if (data.length > 2*1024*1024) { throw new IOException("Optional font exceeds size limit"); }
                if (!CosmeticsLibrary.hash(data).equals(SHA256)) { throw new IOException("Optional font checksum mismatch"); }
                synchronized (this) {
                    if (closed || Thread.currentThread().isInterrupted()) { throw new InterruptedException("Font request cancelled"); }
                    Path target = directory.resolve("qraftys.zip");
                    CosmeticsLibrary.atomic(target, data); status = "Qraftys downloaded locally"; return target;
                }
            } catch (IOException | InterruptedException failure) {
                if (failure instanceof InterruptedException) { Thread.currentThread().interrupt(); }
                status = "Qraftys unavailable · using Inter. Retry or add the ZIP locally";
                throw new CompletionException(failure);
            }
        }, worker);
        return request;
    }
    private static byte[] http() throws IOException, InterruptedException {
        try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build()) {
            var response = client.send(HttpRequest.newBuilder(SOURCE).timeout(Duration.ofSeconds(30)).GET().build(), new BoundedBodyHandler(2*1024*1024));
            if (response.statusCode() != 200) { throw new IOException("Font host HTTP " + response.statusCode()); }
            return response.body();
        }
    }
    public String status() { return status; }
    @Override public synchronized void close() { closed = true; if (request != null) { request.cancel(true); } worker.shutdownNow(); }
}
