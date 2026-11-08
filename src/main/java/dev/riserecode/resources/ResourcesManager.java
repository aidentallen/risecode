package dev.riserecode.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.riserecode.event.EventBus;
import dev.riserecode.event.ResourceReloadFailureEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackType;

/** Client-thread selection and an atomic, recoverable resource-reload transaction. */
public final class ResourcesManager implements AutoCloseable {
    public record ApplyResult(boolean success, String message) { }
    private final ResourcesLibrary library;
    private final EventBus.Subscription failures;
    private final int major, minor;
    private ResourcesState applied = ResourcesState.empty();
    private Runnable changed = () -> { };
    private boolean closed;
    private volatile boolean busy;
    private String status = "Choose resources, then Apply";
    private Transaction transaction;
    private Path rollbackDirectory;
    private static final class Transaction {
        final Path previous; final List<String> packs; final CompletableFuture<ApplyResult> result; final Consumer<Boolean> commit;
        ReloadRecovery recovery;
        Transaction(Path previous, List<String> packs, CompletableFuture<ApplyResult> result, Consumer<Boolean> commit) {
            this.previous = previous; this.packs = packs; this.result = result; this.commit = commit;
        }
    }
    public ResourcesManager(Path config, EventBus bus) {
        var format = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES); major = format.major(); minor = format.minor();
        library = new ResourcesLibrary(config.resolve("resources/26.2"), major, minor);
        try {
            Path pointer = library.directory().resolve("active.json");
            if (Files.isRegularFile(pointer)) {
                var saved = ResourceIo.read(pointer);
                String rollback = saved.has("rollback") ? saved.get("rollback").getAsString() : "";
                if (rollback.matches("[a-f0-9-]{36}") && UUID.fromString(rollback).toString().equals(rollback)) {
                    rollbackDirectory = library.directory().resolve("generations").resolve(rollback);
                }
            }
        } catch (IOException | RuntimeException invalid) { dev.riserecode.Client.INSTANCE.getLogger().warn("Could not read resource rollback pointer", invalid); }
        failures = bus.subscribe(ResourceReloadFailureEvent.class, 100, false, event -> { if (reloadFailed(event.failure())) { event.cancel(); } });
        Path old = config.resolve("styles");
        if (Files.exists(old)) {
            try { Files.move(old, config.resolve("styles-backup-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")))); }
            catch (IOException failure) { dev.riserecode.Client.INSTANCE.getLogger().warn("Could not archive old Styles files; they will remain inactive", failure); }
        }
    }
    public ResourcesLibrary library() { return library; }
    public ResourcesState state() { return applied; }
    public JsonObject snapshot() { return applied.json(); }
    public boolean busy() { return busy; }
    public String status() { return status; }
    public void onChange(Runnable listener) { changed = listener; }
    public void restoreStartup(JsonObject value) {
        applied = ResourcesState.read(value);
        if (!ResourcesPackSource.matches(applied)) { ResourcesPackSource.select(null); status = applied.selected().isEmpty() ? "Choose resources, then Apply" : "Cached generation missing · Apply to recover"; }
        else { status = "Resources restored"; }
    }
    public CompletableFuture<ApplyResult> apply(ResourcesState next) { return apply(next, () -> { }); }
    public CompletableFuture<ApplyResult> apply(ResourcesState next, Runnable afterReload) {
        var mc = Minecraft.getInstance();
        if (!mc.isSameThread()) { throw new IllegalStateException("Resource selection belongs to the client thread"); }
        if (busy || closed || mc.gui.overlay() != null) { return CompletableFuture.completedFuture(new ApplyResult(false, "Wait for the current resource reload")); }
        busy = true; status = "Preparing resources offline…"; var result = new CompletableFuture<ApplyResult>();
        ResourcesState previous = applied; boolean packChanged = !next.equals(previous) || !ResourcesPackSource.matches(next);
        library.work(() -> {
            var retained = new java.util.HashSet<Path>();
            Path active = ResourcesPackSource.directory();
            if (active != null) { retained.add(active); }
            if (rollbackDirectory != null) { retained.add(rollbackDirectory); }
            try { GenerationRetention.prune(library.directory().resolve("generations"), retained); }
            catch (IOException failure) { dev.riserecode.Client.INSTANCE.getLogger().warn("Could not reclaim unused resource generations", failure); }
            if (!packChanged || next.selected().isEmpty()) { return (Path) null; }
            library.verifySelection(next); var resources = library.compose(next);
            Path generation = library.directory().resolve("generations/" + UUID.randomUUID()); Files.createDirectories(generation);
            var manifest = new JsonArray();
            for (var file : resources.entrySet()) {
                Path path = ResourceIo.child(generation, file.getKey()); ResourceIo.writeBytes(path, file.getValue()); manifest.add(file.getKey());
            }
            var metadata = new JsonObject(); var pack = new JsonObject(); pack.addProperty("description", "risecode Resources · Vanilla Tweaks");
            var format = new JsonArray(); format.add(major); format.add(minor); pack.add("min_format", format); pack.add("max_format", format.deepCopy());
            metadata.add("pack", pack); ResourceIo.write(generation.resolve("pack.mcmeta"), metadata);
            var info = new JsonObject(); info.add("state", next.json()); info.add("files", manifest); ResourceIo.write(generation.resolve("resources.json"), info);
            ResourceIo.writeBytes(generation.resolve("credits.txt"), "Credits:\nVanilla Tweaks: https://vanillatweaks.net/\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            for (String id : next.selected()) {
                var archive = library.archive(next.revision(), "pack:" + id);
                for (var file : archive.files().entrySet()) {
                    String name = file.getKey();
                    if (!name.contains("/") && !name.equals("Selected Packs.txt") && (name.endsWith(".txt") || name.endsWith(".md") || name.startsWith("LICENSE"))) {
                        ResourceIo.writeBytes(ResourceIo.child(generation, "credits/" + id + "/" + name), file.getValue());
                    }
                }
            }
            return generation;
        }).whenComplete((generation, error) -> mc.execute(() -> {
            if (closed) { busy = false; result.complete(new ApplyResult(false, "Client stopped")); return; }
            if (error != null) { finish(result, false, ResourcesLibrary.message(error)); return; }
            if (mc.gui.overlay() != null) { finish(result, false, "Wait for the current resource reload"); return; }
            Path previousDirectory = ResourcesPackSource.directory();
            Path previousRollback = rollbackDirectory;
            Consumer<Boolean> commit = success -> {
                if (!success) { applied = previous; return; }
                try {
                    applied = next;
                    if (packChanged) { pointer(generation, previousDirectory); rollbackDirectory = previousDirectory; }
                    afterReload.run(); changed.run();
                } catch (IOException | RuntimeException failure) {
                    applied = previous;
                    if (packChanged) { try { pointer(previousDirectory, previousRollback); rollbackDirectory = previousRollback; } catch (IOException restore) { failure.addSuppressed(restore); } }
                    throw new IllegalStateException(failure);
                }
            };
            if (!packChanged) { try { commit.accept(true); finish(result, true, "Resources applied"); } catch (RuntimeException failure) { finish(result, false, ResourcesLibrary.message(failure)); } return; }
            transaction = new Transaction(previousDirectory, mc.getResourcePackRepository().getSelectedPacks().stream().map(net.minecraft.server.packs.repository.Pack::getId).toList(), result, commit);
            configureRecovery(transaction, mc);
            ResourcesPackSource.select(generation); status = "Reloading resources…";
            try { mc.reloadResourcePacks().whenComplete((ignored, failure) -> mc.execute(() -> {
                if (transaction == null || transaction.result != result || transaction.recovery.recovering()) { return; }
                if (failure != null) { reloadFailed(failure); return; }
                try { commit.accept(true); transaction = null; finish(result, true, "Resources applied"); }
                catch (RuntimeException commitFailure) { reloadFailed(commitFailure); }
            })); } catch (RuntimeException synchronousReload) { reloadFailed(synchronousReload); }
        }));
        return result;
    }
    private void pointer(Path generation, Path rollback) throws IOException {
        var pointer = new JsonObject(); pointer.addProperty("generation", generation == null ? "" : generation.getFileName().toString());
        pointer.addProperty("rollback", rollback == null ? "" : rollback.getFileName().toString());
        ResourceIo.write(library.directory().resolve("active.json"), pointer);
    }
    private void configureRecovery(Transaction pending, Minecraft mc) {
        pending.recovery = new ReloadRecovery(mc::execute, () -> {
            ResourcesPackSource.select(pending.previous);
            mc.getResourcePackRepository().reload();
            mc.getResourcePackRepository().setSelected(pending.packs);
            mc.gui.setOverlay(null);
            return mc.reloadResourcePacks();
        }, outcome -> completeRecovery(pending, outcome));
    }
    private void completeRecovery(Transaction pending, ReloadRecovery.Outcome outcome) {
        if (transaction != pending) { return; }
        transaction = null;
        pending.commit.accept(false);
        String original = ResourcesLibrary.message(outcome.original());
        if (outcome.restored()) {
            finish(pending.result, false, "Previous resources restored: " + original);
        } else {
            finish(pending.result, false, "Resources recovery failed: " + ResourcesLibrary.message(outcome.recovery()) + " · Original failure: " + original);
        }
    }
    /** Called by the Minecraft failure adapter before vanilla discards all selected packs. */
    public boolean reloadFailed(Throwable failure) {
        return transaction != null && transaction.recovery.fail(failure);
    }
    private void finish(CompletableFuture<ApplyResult> result,boolean success,String message){busy=false;status=message;result.complete(new ApplyResult(success,message));}
    @Override public void close() {
        closed = true;
        if (transaction != null) {
            Transaction pending = transaction; transaction = null; pending.recovery.cancel();
            ResourcesPackSource.select(pending.previous); pending.commit.accept(false);
            finish(pending.result, false, "Client stopped during resource reload; prior selection retained");
        }
        failures.close(); library.close();
    }
}
