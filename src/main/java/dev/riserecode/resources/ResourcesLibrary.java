package dev.riserecode.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Full offline cache. Cache updates never select resources or touch Minecraft's active pack list. */
public final class ResourcesLibrary implements AutoCloseable {
    public record Progress(String phase, int completed, int total, long bytes, boolean running, boolean paused,
            boolean ready, List<String> failures) { public Progress { failures = List.copyOf(failures); } }
    private final Path root;
    private final int major, minor;
    private final ResourceProvider provider;
    private final ExecutorService coordinator = Executors.newSingleThreadExecutor(r -> thread(r, "risecode-resource-cache"));
    private final ExecutorService downloads = Executors.newFixedThreadPool(2, r -> thread(r, "risecode-resource-download"));
    private final ExecutorService work = Executors.newFixedThreadPool(2, r -> thread(r, "risecode-resource-io"));
    private final AtomicLong generation = new AtomicLong(), bytes = new AtomicLong();
    private final AtomicInteger done = new AtomicInteger(), total = new AtomicInteger();
    private final List<String> failures = new CopyOnWriteArrayList<>();
    private final List<Future<?>> jobs = new CopyOnWriteArrayList<>();
    private volatile ResourceCatalog catalog;
    private volatile String phase = "Open Resources to cache Vanilla Tweaks";
    private volatile boolean running, paused, ready, closed;
    private Future<?> task;
    private final Map<String, JsonObject> indexes = new LinkedHashMap<>();
    private final Map<String, ResourceCatalog> catalogs = new LinkedHashMap<>();
    private static Thread thread(Runnable runnable, String name) { var thread = new Thread(runnable, name); thread.setDaemon(true); return thread; }
    public ResourcesLibrary(Path root, int major, int minor) { this(root, major, minor, new VanillaTweaksProvider()); }
    public ResourcesLibrary(Path root, int major, int minor, ResourceProvider provider) {
        this.root = root; this.major = major; this.minor = minor; this.provider = java.util.Objects.requireNonNull(provider);
        try {
            if (Files.exists(root.resolve("catalog-pointer.json"))) {
                String revision = ResourceIo.read(root.resolve("catalog-pointer.json")).get("revision").getAsString();
                catalog = catalog(revision); ready = index(revision).get("complete").getAsBoolean();
                phase = ready ? "Offline ready · " + catalog.entries().size() + " resources" : "Partial cache · Resume to finish";
            }
        } catch (IOException | RuntimeException invalid) { phase = "Cache metadata unavailable · Retry downloads"; }
    }
    public Path directory() { return root; }
    public ResourceCatalog catalog() { return catalog; }
    public Progress progress() { return new Progress(phase, done.get(), total.get(), bytes.get(), running, paused, ready, failures); }
    public <T> CompletableFuture<T> work(java.util.concurrent.Callable<T> action) {
        return CompletableFuture.supplyAsync(() -> { try { return action.call(); } catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); } }, work);
    }
    public synchronized void start(boolean refresh) {
        if (running || closed) { return; }
        running = true; paused = false; ready = false; failures.clear(); done.set(0); total.set(0); bytes.set(0);
        long token = generation.incrementAndGet(); phase = refresh ? "Refreshing catalog…" : "Checking offline cache…";
        task = coordinator.submit(() -> {
            try { synchronize(refresh, token); }
            catch (InterruptedException cancelled) { Thread.currentThread().interrupt(); }
            catch (Exception failure) { if (token == generation.get()) { fail(message(failure)); phase = "Cache incomplete · Retry"; } }
            finally { if (token == generation.get()) { running = false; paused = false; jobs.clear(); } }
        });
    }
    public void pause() { if (running) { paused = !paused; } }
    public synchronized void cancel() {
        generation.incrementAndGet(); if (task != null) { task.cancel(true); }
        jobs.forEach(job -> job.cancel(true)); jobs.clear(); running = false; paused = false; phase = "Cancelled · completed downloads kept";
    }
    private void check(long token) throws InterruptedException {
        if (token != generation.get() || Thread.currentThread().isInterrupted() || closed) { throw new InterruptedException("Cache cancelled"); }
        while (paused) { TimeUnit.MILLISECONDS.sleep(100); if (token != generation.get() || closed) { throw new InterruptedException("Cache cancelled"); } }
    }
    private synchronized JsonObject index(String revision) throws IOException {
        revision(revision);
        JsonObject existing = indexes.get(revision); if (existing != null) { return existing; }
        Path path = revisionPath(revision).resolve("index.json");
        var value = Files.exists(path) ? ResourceIo.read(path) : new JsonObject();
        for (String key : List.of("archives", "icons", "previews", "recipes", "probes")) { if (!value.has(key)) { value.add(key, new JsonObject()); } }
        if (!value.has("complete")) { value.addProperty("complete", false); }
        if (!value.has("compilerVersion") || value.get("compilerVersion").getAsInt() != 3) {
            value.add("recipes", new JsonObject()); value.addProperty("complete", false);
        }
        value.addProperty("compilerVersion", 3);
        if (value.has("formatMajor") && (value.get("formatMajor").getAsInt() != major || value.get("formatMinor").getAsInt() != minor)) {
            value.add("recipes", new JsonObject()); value.addProperty("complete", false);
        }
        value.addProperty("formatMajor", major); value.addProperty("formatMinor", minor);
        indexes.put(revision, value); return value;
    }
    private synchronized void save(String revision) throws IOException { ResourceIo.write(revisionPath(revision).resolve("index.json"), index(revision)); }
    private synchronized String reference(String revision, String kind, String key) throws IOException {
        var values = index(revision).getAsJsonObject(kind); return values.has(key) ? values.get(key).getAsString() : null;
    }
    private synchronized void remember(String revision, String kind, String key, String hash) throws IOException {
        index(revision).getAsJsonObject(kind).addProperty(key, hash); save(revision);
    }
    private Path revisionPath(String revision) { revision(revision); return root.resolve("revisions").resolve(revision); }
    private static void revision(String revision) { if (!revision.matches("[a-f0-9]{64}")) { throw new IllegalArgumentException("Invalid cache revision"); } }
    private Path object(String hash) { revision(hash); return root.resolve("objects").resolve(hash); }
    private String store(byte[] data) throws IOException {
        String hash = ResourceIo.hash(data); Path path = object(hash);
        if (!Files.exists(path) || !ResourceIo.hash(Files.readAllBytes(path)).equals(hash)) { ResourceIo.writeBytes(path, data); }
        return hash;
    }
    private byte[] read(String hash) throws IOException {
        try {
            byte[] data = Files.readAllBytes(object(hash));
            if (!ResourceIo.hash(data).equals(hash)) { throw new IOException("Cached resource checksum mismatch"); }
            return data;
        } catch (IOException unavailable) {
            ready = false; phase = "Cache data missing or corrupt · Cache all to repair";
            throw new IOException("Cache data missing or corrupt · Cache all to repair", unavailable);
        }
    }
    public synchronized ResourceCatalog catalog(String revision) throws IOException {
        revision(revision); ResourceCatalog value = catalogs.get(revision); if (value != null) { return value; }
        value = ResourceCatalog.read(Files.readAllBytes(revisionPath(revision).resolve("catalog.json")));
        if (!value.revision().equals(revision)) { throw new IOException("Catalog checksum mismatch"); }
        catalogs.put(revision, value); return value;
    }
    public Path icon(ResourceEntry entry) throws IOException {
        ResourceCatalog current = catalog; if (current == null) { throw new IOException("Catalog unavailable"); }
        String hash = reference(current.revision(), "icons", entry.id());
        if (hash == null) { throw new IOException("Thumbnail is not cached yet"); }
        read(hash); return object(hash);
    }
    public byte[] thumbnail(ResourceEntry entry, boolean large) throws IOException {
        ResourceCatalog current = catalog; if (current == null) { throw new IOException("Catalog unavailable"); }
        String hash = reference(current.revision(), large ? "previews" : "icons", entry.id());
        if (hash == null) { throw new IOException("Image is not cached yet"); }
        byte[] raw = read(hash);
        if (large && webp(raw)) {
            String icon = reference(current.revision(), "icons", entry.id());
            if (icon == null) { throw new IOException("Thumbnail not cached"); }
            return read(icon);
        }
        return large ? thumbnail(raw) : raw;
    }
    private static boolean webp(byte[] raw) {
        return raw.length >= 16 && new String(raw, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(raw, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
    }
    private static void validatePreview(byte[] raw) throws IOException {
        // Keep the original WebP offline; the stock JVM has no WebP decoder, so its official icon is displayed.
        if (!webp(raw)) { thumbnail(raw); }
    }
    private static byte[] thumbnail(byte[] raw) throws IOException {
        try (var stream = javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(raw))) {
            var readers = javax.imageio.ImageIO.getImageReaders(stream); if (!readers.hasNext()) { throw new IOException("Invalid preview image"); }
            var reader = readers.next();
            try {
                reader.setInput(stream); int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 67108864) { throw new IOException("Preview image exceeds dimensions"); }
                var parameters = reader.getDefaultReadParam(); int step = Math.max(1, (int) Math.ceil(Math.max(width, height) / 512.0));
                parameters.setSourceSubsampling(step, step, 0, 0);
                var output = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(reader.read(0, parameters), "png", output); return output.toByteArray();
            } finally { reader.dispose(); }
        }
    }
    public ResourceArchive archive(String revision, String key) throws IOException {
        String hash = reference(revision, "archives", key);
        if (hash == null) { throw new IOException("Missing offline resource · Resume the full cache download"); }
        return ResourceArchive.read(read(hash));
    }
    private ResourceArchive ensure(ResourceCatalog source, String key, List<String> ids, long token) throws IOException, InterruptedException {
        check(token);
        try {
            var cached = archive(source.revision(), key); cached.verifyReceipt(ids); cached.effective(major, minor); return cached;
        } catch (IOException | RuntimeException unavailable) {
            if (Files.getFileStore(root).getUsableSpace() < 256L * 1024 * 1024) { throw new IOException("Not enough free space for resource downloads"); }
            IOException last = null;
            for (int attempt = 0; attempt < 3; attempt++) {
                check(token);
                try {
                    byte[] raw = provider.pack(source, ids); check(token);
                    var pack = ResourceArchive.read(raw); pack.verifyReceipt(ids); pack.effective(major, minor);
                    remember(source.revision(), "archives", key, store(raw)); bytes.addAndGet(raw.length); return pack;
                } catch (IOException invalid) { last = invalid; if (attempt < 2) { TimeUnit.MILLISECONDS.sleep(250L * (attempt + 1)); } }
            }
            throw last == null ? new IOException("Could not validate resource pack") : last;
        }
    }
    private void ensureIcon(ResourceCatalog source, ResourceEntry entry, long token) throws IOException, InterruptedException {
        check(token); String hash = reference(source.revision(), "icons", entry.id());
        try { if (hash != null) { validateImage(read(hash)); return; } } catch (IOException | RuntimeException invalid) { /* Repair only this cached image. */ }
        byte[] raw = provider.icon(entry); check(token); validateImage(raw);
        remember(source.revision(), "icons", entry.id(), store(raw)); bytes.addAndGet(raw.length);
    }
    private void ensurePreview(ResourceCatalog source, ResourceEntry entry, long token) throws IOException, InterruptedException {
        check(token); String hash = reference(source.revision(), "previews", entry.id());
        try { if (hash != null) { validatePreview(read(hash)); return; } } catch (IOException | RuntimeException invalid) { /* Repair this preview. */ }
        byte[] raw = provider.preview(entry); check(token); validatePreview(raw);
        remember(source.revision(), "previews", entry.id(), store(raw)); bytes.addAndGet(raw.length);
    }
    private static void validateImage(byte[] png) throws IOException {
        if (png.length < 24 || java.nio.ByteBuffer.wrap(png).getInt() != 0x89504e47) { throw new IOException("Invalid resource thumbnail"); }
        var buffer = java.nio.ByteBuffer.wrap(png); int width = buffer.getInt(16), height = buffer.getInt(20);
        if (width < 1 || height < 1 || (long) width * height > 16777216) { throw new IOException("Thumbnail exceeds dimensions"); }
    }
    private void synchronize(boolean refresh, long token) throws Exception {
        if (refresh || catalog == null) {
            check(token); phase = "Loading official 26.2 catalog…";
            byte[] raw = provider.catalog(); ResourceCatalog next = ResourceCatalog.read(raw); check(token);
            ResourceIo.writeBytes(revisionPath(next.revision()).resolve("catalog.json"), raw);
            synchronized (this) { catalogs.put(next.revision(), next); }
            var pointer = new JsonObject(); pointer.addProperty("revision", next.revision()); ResourceIo.write(root.resolve("catalog-pointer.json"), pointer);
            catalog = next;
        }
        ResourceCatalog source = catalog; if (source == null) { throw new IOException("Catalog unavailable"); }
        synchronized (this) { index(source.revision()).addProperty("complete", false); save(source.revision()); }
        phase = "Caching resources and previews…"; total.set(source.entries().size() * 3);
        var currentJobs = new ArrayList<Future<?>>();
        for (var entry : source.entries()) {
            currentJobs.add(downloads.submit(() -> {
                try { ensure(source, "pack:" + entry.id(), List.of(entry.id()), token); }
                catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
                catch (Exception failure) { if (token == generation.get()) { fail(entry.name() + ": " + message(failure)); } }
                finally { if (token == generation.get()) { done.incrementAndGet(); } }
            }));
            currentJobs.add(downloads.submit(() -> {
                try { ensureIcon(source, entry, token); }
                catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
                catch (Exception failure) { if (token == generation.get()) { fail(entry.name() + " thumbnail: " + message(failure)); } }
                finally { if (token == generation.get()) { done.incrementAndGet(); } }
            }));
            currentJobs.add(downloads.submit(() -> {
                try { ensurePreview(source, entry, token); }
                catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
                catch (Exception failure) { if (token == generation.get()) { fail(entry.name() + " preview: " + message(failure)); } }
                finally { if (token == generation.get()) { done.incrementAndGet(); } }
            }));
        }
        jobs.addAll(currentJobs);
        for (var job : currentJobs) { check(token); job.get(); }
        jobs.clear(); check(token);
        if (!failures.isEmpty()) { throw new IOException("Some resources could not be cached; Retry preserves completed downloads"); }
        phase = "Discovering official combined resources…";
        List<String> all = source.defaults(source.entries().stream().map(ResourceEntry::id).toList());
        var seed = ensure(source, "probe:all", all, token);
        var recipes = new LinkedHashMap<String, List<String>>();
        addRecipes(recipes, seed.combinations(), source);
        synchronized (this) {
            for (var value : index(source.revision()).getAsJsonObject("recipes").entrySet()) {
                var members = value.getValue().getAsJsonObject().getAsJsonArray("members").asList().stream().map(v -> v.getAsString()).toList();
                addRecipes(recipes, List.of(members), source);
            }
        }
        var processed = new HashSet<String>();
        while (processed.size() < recipes.size()) {
            check(token); var item = recipes.entrySet().stream().filter(e -> !processed.contains(e.getKey())).findFirst().orElseThrow();
            String key = item.getKey(); List<String> members = item.getValue(); processed.add(key);
            phase = "Caching combined assets · " + processed.size() + "/" + recipes.size();
            var official = ensure(source, "combo:" + key, source.defaults(members), token);
            addRecipes(recipes, official.combinations(), source);
            // A superset can hide a more specific fallback recipe. Probe each immediate subset recursively.
            if (members.size() > 2) { for (String removed : members) {
                check(token); var subset = members.stream().filter(id -> !id.equals(removed)).toList();
                var probe = ensure(source, "probe:" + key(subset), source.defaults(subset), token);
                addRecipes(recipes, probe.combinations(), source);
            } }
        }
        phase = "Caching priority-aware combinations…";
        synchronized (this) { index(source.revision()).add("recipes", new JsonObject()); save(source.revision()); }
        int variantsDone = 0;
        for (var entry : recipes.entrySet().stream().sorted(Comparator.<Map.Entry<String, List<String>>>comparingInt(e -> e.getValue().size()).thenComparing(Map.Entry::getKey)).toList()) {
            check(token); var members = entry.getValue();
            var recipe = new JsonObject(); var names = new JsonArray(); members.forEach(names::add); recipe.add("members", names);
            recipe.addProperty("mode", members.size() <= 5 ? "order" : "top"); recipe.add("variants", new JsonObject());
            synchronized (this) { index(source.revision()).getAsJsonObject("recipes").add(entry.getKey(), recipe); }
            var orders = orders(source.defaults(members)); var prepared = new LinkedHashMap<List<String>, Future<ResourceArchive>>();
            for (var ordered : orders) {
                String archiveKey = ordered.equals(source.defaults(members)) ? "combo:" + entry.getKey() : "variant:" + entry.getKey() + ":" + orderKey(ordered);
                Future<ResourceArchive> pending = downloads.submit(() -> ensure(source, archiveKey, ordered, token)); prepared.put(ordered, pending); jobs.add(pending);
            }
            for (var ordered : orders) {
                check(token); phase = "Caching combination priorities · " + (++variantsDone);
                var pending = prepared.get(ordered); var official = pending.get(); jobs.remove(pending);
                Map<String, byte[]> baseline = compose(new ResourcesState(source.revision(), ordered), true);
                Map<String, byte[]> expected = official.effective(major, minor);
                var patch = ResourceComposer.difference(members, baseline, expected);
                var hashes = new LinkedHashMap<String, String>(); for (var file : patch.files().entrySet()) { hashes.put(file.getKey(), store(file.getValue())); }
                synchronized (this) {
                    recipe.getAsJsonObject("variants").add(variantKey(recipe, ordered), ResourceComposer.patchJson(patch, hashes)); save(source.revision());
                }
                assertEqual(compose(new ResourcesState(source.revision(), ordered)), expected, "Combination " + String.join(" + ", ordered));
            }
            if (members.size() > 5) {
                // The public catalog's nine-zombie recipe has fixed combined model data and a top-priority vanilla skin.
                // Verify that reversing every lower-priority member still composes exactly from the cached top variant.
                for (String first : source.defaults(members)) {
                    var ordered = new ArrayList<String>(); ordered.add(first);
                    ordered.addAll(source.defaults(members).reversed().stream().filter(id -> !id.equals(first)).toList());
                    var reference = ensure(source, "order-proof:" + orderKey(ordered), ordered, token);
                    assertEqual(compose(new ResourcesState(source.revision(), ordered)), reference.effective(major, minor), "Large combination ordering");
                }
            }
        }
        check(token);
        synchronized (this) { index(source.revision()).addProperty("complete", true); save(source.revision()); }
        ready = true; phase = "Offline ready · " + source.entries().size() + " resources · " + recipes.size() + " combinations";
    }
    private static void addRecipes(Map<String, List<String>> result, Collection<List<String>> groups, ResourceCatalog source) throws IOException {
        for (var members : groups) {
            if (members.stream().anyMatch(id -> !source.byId().containsKey(id))) { throw new IOException("Unknown resource in official combination receipt"); }
            result.putIfAbsent(key(members), List.copyOf(members));
        }
    }
    public static String key(Collection<String> ids) { return ResourceIo.hash(String.join("+", ids.stream().sorted().toList()).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    public Map<String, byte[]> compose(ResourcesState state) throws IOException { return compose(state, false); }
    private Map<String, byte[]> compose(ResourcesState state, boolean building) throws IOException {
        var result = new LinkedHashMap<String, byte[]>(); if (state.selected().isEmpty()) { return result; }
        var source = catalog(state.revision());
        var owners = new LinkedHashMap<String, List<String>>();
        var keyOwners = new LinkedHashMap<String, Map<String, List<String>>>();
        var positions = new LinkedHashMap<String, Integer>();
        for (int i = 0; i < state.selected().size(); i++) { positions.put(state.selected().get(i), i); }
        for (int i = state.selected().size() - 1; i >= 0; i--) {
            String id = state.selected().get(i); if (!source.byId().containsKey(id)) { throw new IOException("Unknown resource: " + id); }
            var files = archive(state.revision(), "pack:" + id).effective(major, minor);
            for (var file : files.entrySet()) {
                owners.put(file.getKey(), List.of(id));
                if (ResourceComposer.mergesKeys(file.getKey())) {
                    var keys = keyOwners.computeIfAbsent(file.getKey(), ignored -> new LinkedHashMap<>());
                    ResourceArchive.json(file.getValue()).keySet().forEach(key -> keys.put(key, List.of(id)));
                }
            }
            ResourceComposer.merge(result, files);
        }
        List<JsonObject> patches;
        synchronized (this) { patches = index(state.revision()).getAsJsonObject("recipes").entrySet().stream().map(e -> e.getValue().getAsJsonObject().deepCopy()).toList(); }
        Set<String> selected = Set.copyOf(state.selected());
        // General recipes first; more specific recipes include the official correction for their subsets.
        for (var value : patches.stream().sorted((left, right) -> {
            int specificity = Integer.compare(left.getAsJsonArray("members").size(), right.getAsJsonArray("members").size());
            return specificity != 0 ? specificity : priority(members(right), members(left), positions);
        }).toList()) {
            var members = value.getAsJsonArray("members").asList().stream().map(v -> v.getAsString()).toList();
            if (!selected.containsAll(members)) { continue; }
            var ordered = state.selected().stream().filter(members::contains).toList();
            var variants = value.getAsJsonObject("variants"); String variant = variantKey(value, ordered);
            if (!variants.has(variant)) { if (building) { continue; } throw new IOException("Combination priority assets are not fully cached"); }
            value = variants.getAsJsonObject(variant);
            var files = new LinkedHashMap<String, byte[]>();
            for (var file : value.getAsJsonObject("files").entrySet()) { files.put(file.getKey(), read(file.getValue().getAsString())); }
            var removed = value.getAsJsonArray("removed").asList().stream().map(v -> v.getAsString()).toList();
            var removedKeys = new LinkedHashMap<String, List<String>>();
            if (value.has("removedKeys")) { value.getAsJsonObject("removedKeys").entrySet().forEach(e -> removedKeys.put(e.getKey(), e.getValue().getAsJsonArray().asList().stream().map(v -> v.getAsString()).toList())); }
            for (String path : removed) { if (overrides(owners.get(path), members, positions)) { result.remove(path); owners.remove(path); keyOwners.remove(path); } }
            for (var removal : removedKeys.entrySet()) {
                if (!result.containsKey(removal.getKey())) { continue; }
                var json = ResourceArchive.json(result.get(removal.getKey())); var keys = keyOwners.computeIfAbsent(removal.getKey(), ignored -> new LinkedHashMap<>());
                for (String key : removal.getValue()) { if (overrides(keys.get(key), members, positions)) { json.remove(key); keys.put(key, members); } }
                result.put(removal.getKey(), ResourceComposer.json(json));
            }
            for (var file : files.entrySet()) {
                if (ResourceComposer.mergesKeys(file.getKey())) {
                    var delta = ResourceArchive.json(file.getValue()); var filtered = new JsonObject(); var keys = keyOwners.computeIfAbsent(file.getKey(), ignored -> new LinkedHashMap<>());
                    delta.entrySet().forEach(entry -> { if (overrides(keys.get(entry.getKey()), members, positions)) { filtered.add(entry.getKey(), entry.getValue()); keys.put(entry.getKey(), members); } });
                    ResourceComposer.merge(result, Map.of(file.getKey(), ResourceComposer.json(filtered)));
                } else if (overrides(owners.get(file.getKey()), members, positions)) { result.put(file.getKey(), file.getValue()); owners.put(file.getKey(), members); }
            }
        }
        return result;
    }
    private static List<List<String>> orders(List<String> members) {
        var result = new ArrayList<List<String>>();
        if (members.size() > 5) {
            for (String first : members) { var order = new ArrayList<String>(); order.add(first); order.addAll(members.stream().filter(id -> !id.equals(first)).toList()); result.add(List.copyOf(order)); }
        } else { permutations(new ArrayList<>(), members, result); }
        return result;
    }
    private static void permutations(List<String> prefix, List<String> remaining, List<List<String>> result) {
        if (remaining.isEmpty()) { result.add(List.copyOf(prefix)); return; }
        for (String id : remaining) { var next = new ArrayList<>(prefix); next.add(id); permutations(next, remaining.stream().filter(value -> !value.equals(id)).toList(), result); }
    }
    private static String orderKey(List<String> ids) { return ResourceIo.hash(String.join("+", ids).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private static String variantKey(JsonObject recipe, List<String> ordered) { return recipe.get("mode").getAsString().equals("top") ? "top:" + ordered.getFirst() : orderKey(ordered); }
    private static List<String> members(JsonObject value) { return value.getAsJsonArray("members").asList().stream().map(v -> v.getAsString()).toList(); }
    private static int priority(List<String> left, List<String> right, Map<String, Integer> positions) {
        var a = left.stream().map(id -> positions.getOrDefault(id, Integer.MAX_VALUE)).sorted().toList();
        var b = right.stream().map(id -> positions.getOrDefault(id, Integer.MAX_VALUE)).sorted().toList();
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) { int comparison = Integer.compare(a.get(i), b.get(i)); if (comparison != 0) { return comparison; } }
        return Integer.compare(b.size(), a.size());
    }
    private static boolean overrides(List<String> owner, List<String> members, Map<String, Integer> positions) {
        return owner == null || members.containsAll(owner) || priority(owner, members, positions) >= 0;
    }
    public List<List<String>> recipes(String revision) throws IOException {
        synchronized (this) { return index(revision).getAsJsonObject("recipes").entrySet().stream()
                .map(e -> e.getValue().getAsJsonObject().getAsJsonArray("members").asList().stream().map(v -> v.getAsString()).toList()).toList(); }
    }
    public boolean complete(String revision) throws IOException { synchronized (this) { return index(revision).get("complete").getAsBoolean(); } }
    public static void assertEqual(Map<String, byte[]> actual, Map<String, byte[]> expected, String label) throws IOException {
        var differences = new ArrayList<String>();
        for (var file : expected.entrySet()) { if (!ResourceComposer.equivalent(file.getKey(), actual.get(file.getKey()), file.getValue())) { differences.add(file.getKey()); } }
        for (String file : actual.keySet()) { if (!expected.containsKey(file)) { differences.add("extra:" + file); } }
        if (!differences.isEmpty()) { throw new IOException(label + " differs from official output: " + differences.stream().limit(5).toList()); }
    }
    public void verifySelection(ResourcesState state) throws IOException {
        if (state.selected().isEmpty()) { return; }
        if (!complete(state.revision())) { throw new IOException("Finish the full offline cache before applying Resources"); }
        compose(state);
    }
    public JsonObject report() throws IOException {
        var report = new JsonObject(); var progress = progress(); report.addProperty("phase", progress.phase()); report.addProperty("ready", progress.ready());
        if (catalog != null) { report.addProperty("revision", catalog.revision()); report.addProperty("resources", catalog.entries().size()); report.addProperty("combinations", recipes(catalog.revision()).size()); }
        var errors = new JsonArray(); failures.forEach(errors::add); report.add("failures", errors); return report;
    }
    private void fail(String value) { if (failures.size() < 50) { failures.add(value); } }
    public static String message(Throwable failure) { while (failure.getCause() != null) { failure = failure.getCause(); } return failure.getMessage() == null ? failure.toString() : failure.getMessage(); }
    @Override public void close() { closed = true; cancel(); coordinator.shutdownNow(); downloads.shutdownNow(); work.shutdownNow(); provider.close(); }
}
