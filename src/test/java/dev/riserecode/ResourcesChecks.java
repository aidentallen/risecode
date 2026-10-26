package dev.riserecode;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.riserecode.resources.ResourceArchive;
import dev.riserecode.resources.ResourceCatalog;
import dev.riserecode.resources.ResourceComposer;
import dev.riserecode.resources.ResourceEntry;
import dev.riserecode.resources.ResourceIo;
import dev.riserecode.resources.ResourceProvider;
import dev.riserecode.resources.ResourcesLibrary;
import dev.riserecode.resources.ResourcesMigration;
import dev.riserecode.resources.ResourcesState;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Pure offline fixtures plus real worker cancellation, resumability and disk-integrity checks. */
public final class ResourcesChecks {
    private static int checks;
    private ResourcesChecks() { }
    private static byte[] text(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean value, String label) { checks++; if (!value) { throw new AssertionError(label); } }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }
    private static void invalid(Throwing action, String label) throws Exception {
        try { action.run(); throw new AssertionError(label); } catch (IllegalArgumentException | IllegalStateException | IOException expected) { checks++; }
    }
    private static byte[] zip(Map<String, byte[]> files) throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var stream = new ZipOutputStream(bytes)) { for (var file : files.entrySet()) { stream.putNextEntry(new ZipEntry(file.getKey())); stream.write(file.getValue()); stream.closeEntry(); } }
        return bytes.toByteArray();
    }
    private static byte[] catalog() {
        var root = new JsonObject(); var categories = new JsonArray(); var category = new JsonObject(); category.addProperty("category", "Aesthetic"); var packs = new JsonArray();
        for (String id : List.of("A", "B", "C", "1.11ShulkerBoxes", "Hidden")) {
            var pack = new JsonObject(); pack.addProperty("name", id); pack.addProperty("display", id); pack.addProperty("description", "<b>Preview</b> &amp; detail");
            var conflicts = new JsonArray(); if (id.equals("A")) { conflicts.add("B"); } pack.add("incompatible", conflicts);
            if (id.equals("Hidden")) { pack.addProperty("experiment", true); } packs.add(pack);
        }
        category.add("packs", packs); categories.add(category); root.add("categories", categories); return text(root.toString());
    }
    private static final class Fake implements ResourceProvider {
        final AtomicBoolean online = new AtomicBoolean(true), failA = new AtomicBoolean();
        final AtomicInteger requests = new AtomicInteger(), singlesB = new AtomicInteger();
        volatile CountDownLatch gate, entered;
        private void network() { if (!online.get()) { throw new AssertionError("Offline composition made a network request"); } requests.incrementAndGet(); }
        public byte[] catalog() { network(); return ResourcesChecks.catalog(); }
        public byte[] pack(ResourceCatalog catalog, Collection<String> selection) throws IOException {
            network(); var ids = List.copyOf(selection);
            if (ids.equals(List.of("B"))) { singlesB.incrementAndGet(); }
            if (gate != null) {
                if (entered != null) { entered.countDown(); }
                try { gate.await(); } catch (InterruptedException cancelled) { Thread.currentThread().interrupt(); throw new IOException("Cancelled", cancelled); }
            }
            if (failA.get() && ids.equals(List.of("A"))) { throw new IOException("Fixture server failure"); }
            var files = new LinkedHashMap<String, byte[]>(); var language = new JsonObject();
            for (int i = ids.size() - 1; i >= 0; i--) { String id = ids.get(i); language.addProperty(id, id); language.addProperty("shared", id); files.put("assets/minecraft/models/item/" + id.toLowerCase(java.util.Locale.ROOT) + ".json", text("{}")); }
            String receipt = "Vanilla Tweaks Resource Pack\nVersion: 26.2\nPacks:\n\t" + String.join("\n\t", ids);
            if (ids.containsAll(List.of("A", "B"))) { language.addProperty("combined", "official"); files.put("assets/minecraft/models/item/combination.json", text("{\"generated\":true}")); receipt += "\n\nCombined packs:\n\tA+B"; }
            files.put("assets/minecraft/lang/en_us.json", text(language.toString()));
            files.put("assets/minecraft/models/item/choice.json", text("{\"variant\":\"" + (ids.containsAll(List.of("A", "B")) ? "combined" : ids.getFirst()) + "\"}"));
            files.put("pack.mcmeta", text("{\"pack\":{\"description\":\"Fixture\",\"min_format\":88,\"max_format\":88}}"));
            files.put("Selected Packs.txt", text(receipt)); return zip(files);
        }
        public byte[] icon(ResourceEntry entry) throws IOException { network(); return png(); }
        public byte[] preview(ResourceEntry entry) throws IOException { network(); return png(); }
        public void close() { }
    }
    private static byte[] png() throws IOException {
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var bytes = new ByteArrayOutputStream(); javax.imageio.ImageIO.write(image, "png", bytes); return bytes.toByteArray();
    }
    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean()) { if (System.nanoTime() > deadline) { throw new AssertionError("Worker fixture timeout"); } TimeUnit.MILLISECONDS.sleep(5); }
    }
    public static void main(String[] args) throws Exception {
        var catalog = ResourceCatalog.read(catalog());
        check(catalog.entries().size() == 4 && !catalog.byId().containsKey("Hidden"), "Only publicly offered catalog entries");
        check(catalog.byId().containsKey("1.11ShulkerBoxes"), "Official dotted identifier preserved");
        check(catalog.byId().get("A").description().equals("Preview & detail"), "Descriptions are plain text, not executable markup");
        check(catalog.byId().get("A").incompatible().equals(List.of("B")), "Official incompatibility metadata retained");
        var state = new ResourcesState(catalog.revision(), List.of("B", "A"));
        check(ResourcesState.read(state.json()).equals(state), "Profile round trip retains top-first priority and revision");
        invalid(() -> new ResourcesState(catalog.revision(), List.of("A", "A")), "Duplicate selections rejected");
        invalid(() -> new ResourcesState("", List.of("A")), "Nonempty selections require a pinned revision");
        invalid(() -> new ResourcesState(catalog.revision(), List.of("../escape")), "Selection path injection rejected");
        var futureSchema = state.json(); futureSchema.addProperty("version", 2); invalid(() -> ResourcesState.read(futureSchema), "Unknown state schemas rejected");
        var wrongGame = state.json(); wrongGame.addProperty("minecraft", "26.3"); invalid(() -> ResourcesState.read(wrongGame), "Different game versions rejected");
        migration(); archives();
        Path root = Files.createTempDirectory("risecode-resources-check-"); var fake = new Fake();
        try (var library = new ResourcesLibrary(root, 88, 0, fake)) {
            fake.failA.set(true); library.start(false); waitFor(() -> !library.progress().running());
            check(!library.progress().ready() && !library.progress().failures().isEmpty(), "Partial download cannot claim offline readiness");
            int before = fake.singlesB.get(); fake.failA.set(false); library.start(false); waitFor(() -> !library.progress().running());
            check(library.progress().ready(), "Full cache includes every entry, image and verified combination");
            check(fake.singlesB.get() == before, "Retry reuses completed downloads");
            check(library.recipes(catalog.revision()).equals(List.of(List.of("A", "B"))), "Official combination receipt indexed");
            fake.online.set(false); int requests = fake.requests.get();
            var files = library.compose(state); var language = ResourceArchive.json(files.get("assets/minecraft/lang/en_us.json"));
            check(language.get("shared").getAsString().equals("B"), "Custom priority survives combination patches");
            check(language.has("A") && language.has("B") && language.has("combined"), "Language keys and generated combination data coexist");
            check(files.containsKey("assets/minecraft/models/item/combination.json"), "Offline combined assets included");
            var novel = library.compose(new ResourcesState(catalog.revision(), List.of("C", "B", "A")));
            check(ResourceArchive.json(novel.get("assets/minecraft/lang/en_us.json")).get("shared").getAsString().equals("C"), "Previously ungenerated combination works offline");
            check(ResourceArchive.json(novel.get("assets/minecraft/models/item/choice.json")).get("variant").getAsString().equals("C"), "A lower-priority combination cannot replace a higher-priority independent model");
            check(fake.requests.get() == requests, "New combinations make zero network requests");
            check(library.thumbnail(catalog.byId().get("A"), true).length > 24, "Previews available offline");
            var index = ResourceIo.read(root.resolve("revisions/" + catalog.revision() + "/index.json"));
            Path object = root.resolve("objects/" + index.getAsJsonObject("archives").get("pack:A").getAsString()); Files.write(object, new byte[] {0});
            invalid(() -> library.compose(state), "Corrupt cached archives fail before application");
            fake.online.set(true); library.start(false); waitFor(() -> !library.progress().running()); check(library.progress().ready(), "Corrupt object repaired by verified full-cache retry");
        }
        fake.online.set(false);
        try (var restored = new ResourcesLibrary(root, 88, 0, fake)) {
            check(restored.progress().ready() && restored.catalog().entries().size() == 4, "Offline cache restores after restart");
            check(restored.compose(state).containsKey("assets/minecraft/models/item/combination.json"), "Restart retains combination recipes without provider access");
        }
        cancellation();
        System.out.println("Resources checks passed (" + checks + " assertions).");
    }
    private static void migration() throws Exception {
        var root = new JsonObject(); root.addProperty("schemaVersion", 4); root.add("modules", new JsonObject()); var styles = new JsonObject(); var atmosphere = new JsonObject();
        atmosphere.addProperty("Sky & Water", true); atmosphere.addProperty("Sky Mode", "NEBULA"); atmosphere.addProperty("Water", false);
        atmosphere.addProperty("Atmosphere Key", 297); atmosphere.addProperty("Atmosphere Binding", "HOLD");
        var color = new JsonObject(); color.addProperty("primary", 123); color.addProperty("secondary", 456); atmosphere.add("Sky Color", color);
        styles.add("atmosphere", atmosphere); styles.add("selections", new JsonObject()); root.add("styles", styles);
        var ui = new JsonObject(); ui.addProperty("selectedTab", "STYLES"); ui.addProperty("offsetX", 19); root.add("ui", ui); root.addProperty("unrelated", "keep");
        var migrated = ResourcesMigration.upgrade(root); var module = migrated.getAsJsonObject("modules").getAsJsonObject("Ambience");
        check(module.get("enabled").getAsBoolean() && module.get("keybind").getAsInt() == 297 && module.get("bindMode").getAsString().equals("HOLD"), "Ambience activation and binding migrate to module");
        check(module.getAsJsonObject("settings").get("Sky Color").equals(color) && !module.getAsJsonObject("settings").get("Water").getAsBoolean(), "Complete atmosphere setting snapshots migrate");
        check(!migrated.has("styles") && ResourcesState.read(migrated.getAsJsonObject("resources")).selected().isEmpty(), "Old resource styles removed without inventing replacements");
        check(migrated.getAsJsonObject("ui").get("selectedTab").getAsString().equals("RESOURCES") && migrated.getAsJsonObject("ui").get("offsetX").getAsInt() == 19, "Tab changes while other UI preferences survive");
        check(migrated.get("unrelated").getAsString().equals("keep") && root.has("styles"), "Migration preserves unrelated values and original document");
        check(ResourcesMigration.upgrade(migrated).equals(migrated), "Migration is idempotent");
        var legacy = root.deepCopy(); var explicit = new JsonObject(); explicit.addProperty("enabled", false); legacy.getAsJsonObject("modules").add("Ambience", explicit);
        check(!ResourcesMigration.upgrade(legacy).getAsJsonObject("modules").getAsJsonObject("Ambience").get("enabled").getAsBoolean(), "Explicit legacy module takes precedence");
        legacy.addProperty("schemaVersion", 6); invalid(() -> ResourcesMigration.upgrade(legacy), "Future configuration preserved rather than downgraded");
    }
    private static void archives() throws Exception {
        var files = new LinkedHashMap<String, byte[]>();
        files.put("pack.mcmeta", text("{\"pack\":{\"min_format\":88,\"max_format\":88},\"overlays\":{\"entries\":[{\"directory\":\"overlay\",\"min_format\":88,\"max_format\":88}]}}"));
        files.put("assets/minecraft/textures/block/stone.png", new byte[] {1}); files.put("overlay/assets/minecraft/textures/block/stone.png", new byte[] {2});
        files.put("assets/minecraft/sounds/a.ogg", new byte[] {3}); files.put("assets/minecraft/font/custom.json", text("{}")); files.put("assets/minecraft/textures/block/stone.png.mcmeta", text("{}"));

        var storedBytes = new ByteArrayOutputStream();
        try (var output = new ZipOutputStream(storedBytes)) {
            var directory = new ZipEntry("assets/"); directory.setMethod(ZipEntry.STORED); directory.setSize(0); directory.setCompressedSize(0); directory.setCrc(0); output.putNextEntry(directory); output.closeEntry();
            var metadata = new ZipEntry("pack.mcmeta"); output.putNextEntry(metadata); output.write(text("{\"pack\":{\"min_format\":88,\"max_format\":88}}")); output.closeEntry();
        }
        byte[] stored = storedBytes.toByteArray(); stored[6] |= 8;
        check(ResourceArchive.read(stored).files().containsKey("pack.mcmeta"), "Official STORED directory descriptors use central-directory sizes");
        var pack = ResourceArchive.read(zip(files)); var effective = pack.effective(88, 0);
        check(effective.get("assets/minecraft/textures/block/stone.png")[0] == 2, "Current-version overlays override canonical resources");
        check(effective.containsKey("assets/minecraft/sounds/a.ogg") && effective.containsKey("assets/minecraft/font/custom.json") && effective.containsKey("assets/minecraft/textures/block/stone.png.mcmeta"), "Sounds fonts and animation are not filtered by old categories");
        invalid(() -> pack.effective(89, 0), "Incompatible pack format rejected");
        invalid(() -> ResourceArchive.read(zip(Map.of("../escape", text("bad")))), "Archive traversal rejected");
        invalid(() -> ResourceIo.child(Path.of("/tmp/resources"), "assets/../../escape"), "Generated path traversal rejected");
        invalid(() -> ResourceArchive.read(zip(Map.of("assets/a.txt", text("no metadata")))), "Missing metadata rejected");
        var result = new LinkedHashMap<String, byte[]>();
        ResourceComposer.merge(result, Map.of("assets/minecraft/lang/en_us.json", text("{\"a\":1,\"same\":1}")));
        ResourceComposer.merge(result, Map.of("assets/minecraft/lang/en_us.json", text("{\"b\":2,\"same\":2}")));
        var language = ResourceArchive.json(result.get("assets/minecraft/lang/en_us.json"));
        check(language.size() == 3 && language.get("same").getAsInt() == 2, "Language merge keeps independent keys and higher priority");
        ResourceComposer.merge(result, Map.of("assets/minecraft/models/item/test.json", text("{\"a\":1}")));
        ResourceComposer.merge(result, Map.of("assets/minecraft/models/item/test.json", text("{\"b\":2}")));
        check(ResourceArchive.json(result.get("assets/minecraft/models/item/test.json")).size() == 1, "Model JSON uses resource replacement, not blanket merging");
        var expected = new LinkedHashMap<>(result); expected.put("assets/minecraft/lang/en_us.json", text("{\"same\":2,\"b\":2,\"generated\":true}"));
        var patch = ResourceComposer.difference(List.of("A", "B"), result, expected); ResourceComposer.patch(result, patch);
        ResourcesLibrary.assertEqual(result, expected, "Fixture patch"); checks++;
    }
    private static void cancellation() throws Exception {
        var fake = new Fake(); fake.gate = new CountDownLatch(1); fake.entered = new CountDownLatch(1);
        try (var library = new ResourcesLibrary(Files.createTempDirectory("resources-cancel-"), 88, 0, fake)) {
            library.start(false); check(fake.entered.await(5, TimeUnit.SECONDS), "Download worker started"); library.pause();
            check(library.progress().paused(), "Pause is immediately visible"); fake.gate.countDown(); TimeUnit.MILLISECONDS.sleep(150);
            int before = fake.requests.get(); TimeUnit.MILLISECONDS.sleep(150); check(before == fake.requests.get(), "Paused cache makes no new requests after in-flight work drains");
            library.cancel(); check(!library.progress().running() && !library.progress().ready(), "Cancel cannot mark a partial cache complete");
            fake.gate = null; library.start(false); waitFor(() -> !library.progress().running()); check(library.progress().ready(), "Cancelled cache resumes without touching active selections");
        }
    }
}
