package dev.riserecode.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Adapter for the official picker's endpoints, not a promise of a stable public API. No telemetry. */
public final class VanillaTweaksProvider implements ResourceProvider {
    private static final java.util.concurrent.atomic.AtomicLong requests = new java.util.concurrent.atomic.AtomicLong();
    public static long requestCount() { return requests.get(); }
    public static final URI BASE = URI.create("https://vanillatweaks.net/");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final java.util.concurrent.ScheduledExecutorService deadlines = Executors.newScheduledThreadPool(2, r -> {
        var thread = new Thread(r, "risecode-resource-timeouts"); thread.setDaemon(true); return thread;
    });
    public byte[] catalog() throws IOException {
        return request(BASE.resolve("assets/resources/json/26.2/rpcategories.json"), null, 4 * 1024 * 1024);
    }
    public byte[] pack(ResourceCatalog catalog, Collection<String> selected) throws IOException {
        var groups = new JsonObject();
        // The website reverses each top-first row list before submitting it to its generator.
        var ids = List.copyOf(selected);
        for (int i = ids.size() - 1; i >= 0; i--) {
            var entry = catalog.byId().get(ids.get(i)); if (entry == null) { throw new IOException("Unknown resource: " + ids.get(i)); }
            if (!groups.has(entry.group())) { groups.add(entry.group(), new JsonArray()); }
            groups.getAsJsonArray(entry.group()).add(entry.id());
        }
        String form = "version=26.2&packs=" + URLEncoder.encode(groups.toString(), StandardCharsets.UTF_8);
        byte[] response = request(BASE.resolve("assets/server/zipresourcepacks.php"), form, 1024 * 1024);
        try {
            var result = JsonParser.parseString(new String(response, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!result.has("status") || !result.get("status").getAsString().equals("success")) { throw new IOException("Vanilla Tweaks could not prepare this pack"); }
            URI link = BASE.resolve(result.get("link").getAsString());
            if (!link.getPath().startsWith("/download/") || !link.getPath().endsWith(".zip")) { throw new IOException("Invalid pack download URL"); }
            return request(link, null, 128 * 1024 * 1024);
        } catch (IllegalArgumentException | IllegalStateException invalid) { throw new IOException("Official download response changed", invalid); }
    }
    public byte[] icon(ResourceEntry entry) throws IOException {
        return request(BASE.resolve("assets/resources/icons/resourcepacks/26.2/" + entry.id() + ".png?v1"), null, 8 * 1024 * 1024);
    }
    public byte[] preview(ResourceEntry entry) throws IOException {
        return request(BASE.resolve("assets/resources/previews/resourcepacks/26.2/" + entry.id() + "." + entry.previewExtension() + "?v2"), null, 32 * 1024 * 1024);
    }
    private byte[] request(URI uri, String form, int maximum) throws IOException {
        requests.incrementAndGet();
        if (Boolean.getBoolean("riserecode.resourcesOffline")) { throw new IOException("Network acquisition disabled for offline verification"); }
        if (!"https".equals(uri.getScheme()) || !"vanillatweaks.net".equals(uri.getHost()) || uri.getUserInfo() != null || uri.getPort() != -1) {
            throw new IOException("Untrusted resource URL");
        }
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            if (Thread.currentThread().isInterrupted()) { throw new IOException("Download cancelled"); }
            var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(45))
                    .header("User-Agent", "risecode-resources/0.14.0 (+https://github.com/aidentallen/risecode)");
            if (form != null) { request.header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)); }
            try {
                var response = http.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
                try (var body = response.body()) {
                    if (response.statusCode() == 429 || response.statusCode() >= 500) {
                        long delay = 1L << attempt;
                        try { delay = Math.min(60, Math.max(delay, Long.parseLong(response.headers().firstValue("Retry-After").orElse("0")))); }
                        catch (NumberFormatException ignored) { /* The bounded retry remains valid without a numeric header. */ }
                        last = new IOException("Vanilla Tweaks returned HTTP " + response.statusCode());
                        TimeUnit.SECONDS.sleep(delay); continue;
                    }
                    if (response.statusCode() != 200) { throw new IOException("Vanilla Tweaks returned HTTP " + response.statusCode()); }
                    var expired = new AtomicBoolean(); var timeout = deadlines.schedule(() -> {
                        expired.set(true); closeTimedOut(body);
                    }, 45, TimeUnit.SECONDS);
                    try {
                        byte[] bytes = body.readNBytes(maximum + 1);
                        if (expired.get()) { throw new IOException("Resource download timed out"); }
                        if (bytes.length > maximum) { throw new IOException("Resource download exceeds limits"); }
                        return bytes;
                    } finally { timeout.cancel(false); }
                }
            } catch (InterruptedException cancelled) { Thread.currentThread().interrupt(); throw new IOException("Download cancelled", cancelled); }
            catch (IOException failure) { last = failure; }
        }
        throw last == null ? new IOException("Resource download failed") : last;
    }
    private static void closeTimedOut(java.io.InputStream body) { try { body.close(); } catch (IOException ignored) { } }
    @Override public void close() { deadlines.shutdownNow(); http.shutdownNow(); }
}
