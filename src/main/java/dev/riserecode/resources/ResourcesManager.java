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
    private static final class Transaction {
        final Path previous; final List<String> packs; final CompletableFuture<ApplyResult> result; final Consumer<Boolean> commit;
        boolean recovering; Throwable failure;
        Transaction(Path previous, List<String> packs, CompletableFuture<ApplyResult> result, Consumer<Boolean> commit) {
            this.previous = previous; this.packs = packs; this.result = result; this.commit = commit;
        }
    }
    public ResourcesManager(Path config, EventBus bus) {
        var format = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES); major = format.major(); minor = format.minor();
        library = new ResourcesLibrary(config.resolve("resources/26.2"), major, minor);
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
            Consumer<Boolean> commit = success -> {
                if (!success) { applied = previous; return; }
                try {
                    applied = next;
                    if (packChanged) { pointer(generation); }
                    afterReload.run(); changed.run();
                } catch (IOException | RuntimeException failure) {
                    applied = previous;
                    if (packChanged) { try { pointer(previousDirectory); } catch (IOException restore) { failure.addSuppressed(restore); } }
                    throw new IllegalStateException(failure);
                }
            };
            if (!packChanged) { try { commit.accept(true); finish(result, true, "Resources applied"); } catch (RuntimeException failure) { finish(result, false, ResourcesLibrary.message(failure)); } return; }
            transaction = new Transaction(previousDirectory, mc.getResourcePackRepository().getSelectedPacks().stream().map(net.minecraft.server.packs.repository.Pack::getId).toList(), result, commit);
            ResourcesPackSource.select(generation); status = "Reloading resources…";
            mc.reloadResourcePacks().whenComplete((ignored, failure) -> mc.execute(() -> {
                if (transaction == null || transaction.result != result || transaction.recovering) { return; }
                if (failure != null) { reloadFailed(failure); return; }
                try { commit.accept(true); transaction = null; finish(result, true, "Resources applied"); }
                catch (RuntimeException commitFailure) { reloadFailed(commitFailure); }
            }));
        }));
        return result;
    }
    private void pointer(Path generation) throws IOException {
        var pointer = new JsonObject(); pointer.addProperty("generation", generation == null ? "" : generation.getFileName().toString());
        ResourceIo.write(library.directory().resolve("active.json"), pointer);
    }
    /** Called by the Minecraft failure adapter before vanilla discards all selected packs. */
    public boolean reloadFailed(Throwable failure) {
        Transaction t=transaction;if(t==null)return false;
        if(t.recovering){transaction=null;t.commit.accept(false);finish(t.result,false,"Resources recovery failed: "+ResourcesLibrary.message(failure));return false;}
        t.recovering=true;t.failure=failure;ResourcesPackSource.select(t.previous);
        var mc=Minecraft.getInstance();mc.getResourcePackRepository().reload();mc.getResourcePackRepository().setSelected(t.packs);mc.gui.setOverlay(null);
        mc.execute(()->mc.reloadResourcePacks().whenComplete((ignored,error)->mc.execute(()->{
            if(transaction!=t)return;transaction=null;t.commit.accept(false);finish(t.result,false,"Previous resources restored: "+ResourcesLibrary.message(t.failure));
        })));
        return true;
    }
    private void finish(CompletableFuture<ApplyResult> result,boolean success,String message){busy=false;status=message;result.complete(new ApplyResult(success,message));}
    @Override public void close() { closed = true; failures.close(); library.close(); }
}
