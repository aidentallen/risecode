package dev.riserecode;

import dev.riserecode.cosmetics.*;
import dev.riserecode.net.BoundedBodyHandler;
import dev.riserecode.resources.GenerationRetention;
import dev.riserecode.seedcracker.SeedDatabase;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Failure-boundary regressions from the independent whole-repository review. */
public final class ReviewSafetyChecks {
    private static int assertions;
    private static void check(boolean value, String message) { assertions++; if (!value) { throw new AssertionError(message); } }
    public static void main(String[] args) throws Exception {
        for (String asset : new String[]{"/tmp/probe.png", "../probe.png", "nested/../probe.png", "nested//probe.png", "./probe.png", "nested/./probe.png", "a\\b.png"}) {
            boolean rejected = false;
            try { new CosmeticDefinition("probe", "Probe", CosmeticCategory.CAPE, "cape", asset, "0".repeat(64), 1, "a", "b", "c"); }
            catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Unsafe catalog asset rejected: " + asset);
        }
        int reads = 0;
        var budget = new dev.riserecode.seedcracker.FinderBudget(7, TimeUnit.SECONDS.toNanos(5));
        try {
            while (true) { dev.riserecode.seedcracker.FinderBudget.read(); reads++; }
        } catch (dev.riserecode.seedcracker.FinderBudget.Yield yielded) { check(reads == 7, "Finder quota suspends before the eighth read"); }
        finally { budget.close(); }
        dev.riserecode.seedcracker.FinderBudget.read();
        var bus = new dev.riserecode.event.EventBus();
        var module = new dev.riserecode.module.Module("Headless", "", dev.riserecode.module.Category.WORLD, bus) { };
        module.setEnabled(true); check(module.isEnabled() && module.popScale(.5) == 1, "Headless lifecycle without theme singleton");
        module.setEnabled(false); check(!module.isEnabled(), "Headless cleanup without presentation");
        Path temp = Files.createTempDirectory("risecode-review-safety-");
        try {
            byte[] png = CosmeticsLibrary.bundled("capes/gradient.png");
            Path outside = Files.createDirectories(temp.resolve("outside"));
            Path sentinel = outside.resolve("sentinel.png"); Files.writeString(sentinel, "keep");
            Path cache = Files.createDirectories(temp.resolve("cache")); Files.createSymbolicLink(cache.resolve("escape"), outside);
            var definition = new CosmeticDefinition("probe", "Probe", CosmeticCategory.CAPE, "cape", "escape/sentinel.png", CosmeticsLibrary.hash(png), png.length, "a", "b", "c");
            try (var library = new CosmeticsLibrary(cache, (path, limit) -> png)) {
                library.download(definition);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (library.busy() && System.nanoTime() < deadline) { Thread.sleep(5); }
                check(!library.busy() && !library.available(definition), "Symlink download refused");
                check(Files.readString(sentinel).equals("keep"), "Outside sentinel unchanged");
                boolean rejected = false;
                try { library.read(definition); } catch (java.io.IOException expected) { rejected = true; }
                check(rejected, "Symlink read refused");
            }
            Path generations = Files.createDirectories(temp.resolve("generations"));
            Path active = Files.createDirectories(generations.resolve(UUID.randomUUID().toString()));
            Path rollback = Files.createDirectories(generations.resolve(UUID.randomUUID().toString()));
            Files.writeString(active.resolve("pack.mcmeta"), "active"); Files.writeString(rollback.resolve("pack.mcmeta"), "rollback");
            for (int i = 0; i < 20; i++) { Path old = Files.createDirectories(generations.resolve(UUID.randomUUID().toString())); Files.writeString(old.resolve("data"), "unused"); }
            Files.createSymbolicLink(generations.resolve(UUID.randomUUID().toString()), outside);
            GenerationRetention.prune(generations, Set.of(active, rollback));
            try (var entries = Files.list(generations)) { check(entries.count() == 2, "Only active and rollback generations retained"); }
            check(Files.readString(sentinel).equals("keep"), "Retention never follows symlinks");
            var requests = new java.util.concurrent.atomic.AtomicInteger();
            Path fontCache = Files.createDirectories(temp.resolve("fonts"));
            Path font = fontCache.resolve("qraftys.zip"); Files.writeString(font, "keep prior pack");
            try (var optional = new dev.riserecode.ui.font.OptionalFontDownload(() -> { requests.incrementAndGet(); return new byte[]{1,2,3}; })) {
                check(requests.get() == 0, "Optional font construction sends no request");
                boolean rejected = false;
                try { optional.request(fontCache).get(5, TimeUnit.SECONDS); }
                catch (java.util.concurrent.ExecutionException expected) { rejected = true; }
                check(rejected && Files.readString(font).equals("keep prior pack"), "Invalid downloaded font preserves prior cache");
                check(optional.request(fontCache).isCompletedExceptionally() && requests.get() == 1, "Failed font acquisition does not retry every frame");
            }
            networkChecks();
        } finally {
            try (var files = Files.walk(temp)) { for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) { Files.deleteIfExists(file); } }
        }
        System.out.println("Review safety checks passed (" + assertions + " assertions).");
    }
    private static void networkChecks() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/oversized", exchange -> {
            exchange.sendResponseHeaders(200, 0); // chunked: no Content-Length hint
            try (var output = exchange.getResponseBody()) { for (int i = 0; i < 4096; i++) { output.write(new byte[8192]); } }
            catch (java.io.IOException cancelled) { /* receiver closes early at its cap */ }
        });
        server.createContext("/ok", exchange -> { byte[] bytes = "ok".getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200, bytes.length); try (var output = exchange.getResponseBody()) { output.write(bytes); } });
        server.start();
        URI root = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (var client = HttpClient.newHttpClient(); var database = new SeedDatabase(root.resolve("/ok"), root.resolve("/oversized"))) {
            boolean rejected = false;
            try { client.send(HttpRequest.newBuilder(root.resolve("/oversized")).build(), new BoundedBodyHandler(1024)); }
            catch (java.io.IOException expected) { rejected = true; }
            check(rejected, "Chunked response rejected while acquiring bytes");
            check(new String(client.send(HttpRequest.newBuilder(root.resolve("/ok")).build(), new BoundedBodyHandler(2)).body(), StandardCharsets.UTF_8).equals("ok"), "Client usable after oversized body");
            for (int i = 0; i < 10; i++) { check(database.lookup(1).get(5, TimeUnit.SECONDS).isEmpty(), "Lookup completes"); }
            boolean submitRejected = false;
            try { database.submit(java.util.Map.of(), () -> { }).get(5, TimeUnit.SECONDS); }
            catch (java.util.concurrent.ExecutionException expected) { submitRejected = true; }
            check(submitRejected, "Submission response is also capped");
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (database.pendingRequests() != 0 && System.nanoTime() < deadline) { Thread.onSpinWait(); }
            check(database.pendingRequests() == 0, "Completed response tracking is empty");
        } finally { server.stop(0); }
    }
}
