package dev.riserecode.seedcracker;

import com.seedfinding.mccore.rand.seed.WorldSeed;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Created lazily by explicit database actions. Never contacted during startup. */
public final class SeedDatabase implements AutoCloseable {
    public static final URI LOOKUP=URI.create("https://docs.google.com/spreadsheets/d/1tuQiE-0leW88em9OHbZnH-RFNhVqgoHhIt9WQbeqqWw/export?format=csv&gid=0");
    public static final URI SUBMIT=URI.create("https://script.google.com/macros/s/AKfycbye87L-fEYq2EkgczvhKb_kGecp5wL1oX95vg45TRSwNvpv7K-53zoInGTeI1FZ0kv7DA/exec");
    private final ExecutorService executor=Executors.newFixedThreadPool(2, runnable -> Thread.ofPlatform().daemon().name("risecode-seed-database").unstarted(runnable));
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).executor(executor).followRedirects(HttpClient.Redirect.NORMAL).build();
    private final List<CompletableFuture<?>> pending=java.util.Collections.synchronizedList(new ArrayList<>());
    private volatile boolean closed;
    private final URI lookup, submit;
    public SeedDatabase() { this(LOOKUP,SUBMIT); }
    public SeedDatabase(URI lookup, URI submit) { this.lookup=lookup; this.submit=submit; }
    public CompletableFuture<List<Long>> lookup(long hash) {
        if (hash==0) { return CompletableFuture.failedFuture(new IllegalArgumentException("No usable server seed hash; database results cannot be verified")); }
        var request=HttpRequest.newBuilder(lookup).timeout(Duration.ofSeconds(10)).header("User-Agent","Risecode SeedCracker/0.28.0").GET().build();
        var response=client.sendAsync(request,HttpResponse.BodyHandlers.ofString()); pending.add(response);
        var result=response.thenApply(reply -> {
            if (reply.statusCode()!=200) { throw new IllegalStateException("Lookup HTTP "+reply.statusCode()); }
            return parseVerified(reply.body(),hash);
        }); pending.add(result); return result;
    }
    public static List<Long> parseVerified(String csv, long hash) {
        if (csv.length()>8*1024*1024) { throw new IllegalArgumentException("Database response too large"); }
        var seeds=new java.util.LinkedHashSet<Long>();
        for (String row : csv.split("\n")) {
            String[] fields=row.split(",",-1);
            if (fields.length<7) { continue; }
            try {
                String value=fields[2].strip().replace("\"","");
                long seed=Long.parseLong(value.endsWith("L") ? value.substring(0,value.length()-1) : value);
                if (hash!=0 && WorldSeed.toHash(seed)==hash) { seeds.add(seed); }
            } catch (NumberFormatException ignored) { /* headers and malformed rows are not evidence */ }
        }
        return List.copyOf(seeds);
    }
    public CompletableFuture<String> submit(Map<String, String> payload, Runnable authenticate) {
        String form=payload.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
        if (closed) { return CompletableFuture.failedFuture(new IllegalStateException("Database disabled")); }
        var auth=CompletableFuture.runAsync(authenticate,executor).orTimeout(15,java.util.concurrent.TimeUnit.SECONDS); pending.add(auth);
        var result=auth.thenCompose(ignored -> {
            if (closed || Thread.currentThread().isInterrupted()) { throw new java.util.concurrent.CancellationException(); }
            var request=HttpRequest.newBuilder(submit).timeout(Duration.ofSeconds(10)).header("User-Agent","SeedcrackerX mod")
                    .header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build();
            var response=client.sendAsync(request,HttpResponse.BodyHandlers.ofString()); synchronized (pending) { pending.add(response); }
            return response.thenApply(reply -> {
                if (reply.statusCode()!=200 && reply.statusCode()!=302) { throw new IllegalStateException("Submission HTTP "+reply.statusCode()); }
                return "Submission request accepted by the upstream service";
            });
        }); pending.add(result); return result;
    }
    @Override public void close() {
        closed=true;
        synchronized (pending) { pending.forEach(future -> future.cancel(true)); pending.clear(); }
        client.shutdownNow(); executor.shutdownNow();
    }
}
