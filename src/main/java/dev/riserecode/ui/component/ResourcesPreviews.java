package dev.riserecode.ui.component;

import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.resources.ResourceEntry;
import dev.riserecode.resources.ResourceIo;
import dev.riserecode.resources.ResourcesLibrary;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Bounded GPU cache; file reads and image preview conversion run on the resource I/O workers. */
public final class ResourcesPreviews {
    private record Preview(Identifier id, int width, int height) { }
    private static final dev.riserecode.ui.render.FramePinnedCache<String,Preview> loaded = new dev.riserecode.ui.render.FramePinnedCache<>(48,preview -> Minecraft.getInstance().getTextureManager().release(preview.id()));
    private static final LinkedHashMap<String, CompletableFuture<byte[]>> pending = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Long> failed = new LinkedHashMap<>();
    private static long textureSerial;
    private ResourcesPreviews() { }
    public static void beginFrame() {
        var policy=dev.riserecode.performance.PerformancePolicy.frame();loaded.maximum(policy.previewEntries());loaded.beginFrame();
        while(pending.size()>policy.previewPending()){var it=pending.entrySet().iterator();it.next().getValue().cancel(false);it.remove();}
    }
    public record Statistics(int entries,int pending,long decodedCpuBytes,long nativeImageBytes,long ownedGpuBytes,int overage) { }
    public static Statistics statistics(){long decoded=0,pixels=0;for(var f:pending.values()){if(f.isDone()&&!f.isCompletedExceptionally()&&!f.isCancelled()){var data=f.getNow(null);if(data!=null){decoded+=data.length;}}}for(var p:loaded.owned()){pixels+=p.width()*(long)p.height()*4;}return new Statistics(loaded.size(),pending.size(),decoded,pixels,pixels,loaded.overage());}
    public static void draw(GuiGraphicsExtractor g, ResourcesLibrary library, ResourceEntry entry, boolean large,
            double x, double y, double size, double opacity) {
        if (library.catalog() == null) { return; }
        String key = library.catalog().revision() + ":" + entry.id() + ":" + large;
        if (failed.getOrDefault(key, 0L) > System.nanoTime()) { return; }
        Preview preview = loaded.get(key);
        if (preview == null) {
            if (!pending.containsKey(key) && pending.size() >= dev.riserecode.performance.PerformancePolicy.frame().previewPending()) {
                // Retain completed visible previews until their next draw. Evict only when scrolling needs room.
                var iterator = pending.entrySet().iterator();
                while (iterator.hasNext() && pending.size() >= dev.riserecode.performance.PerformancePolicy.frame().previewPending()) {
                    if (iterator.next().getValue().isDone()) { iterator.remove(); }
                }
                if (pending.size() >= dev.riserecode.performance.PerformancePolicy.frame().previewPending()) { return; }
            }
            var future = pending.computeIfAbsent(key, ignored -> library.work(() -> library.thumbnail(entry, large)));
            if (!future.isDone()) { return; }
            // A frame containing more than 48 distinct cards waits for another extraction.
            // Never destroy a texture already referenced by deferred GUI state.
            if (!loaded.makeRoom()) { return; }
            pending.remove(key);
            try {
                var image = NativeImage.read(future.join()); int width = image.getWidth(), height = image.getHeight();
                var texture = new DynamicTexture(() -> "risecode resource preview", image);
                var id = Identifier.fromNamespaceAndPath("riserecode", "resources/preview/" + ResourceIo.hash(key.getBytes(java.nio.charset.StandardCharsets.UTF_8))+"_"+textureSerial++);
                Minecraft.getInstance().getTextureManager().register(id, texture); preview = new Preview(id, width, height); loaded.put(key, preview);
            } catch (java.io.IOException | RuntimeException unavailable) {
                failed.put(key, System.nanoTime() + 5_000_000_000L); if (failed.size() > 128) { failed.remove(failed.keySet().iterator().next()); } return;
            }
        }
        double scale = size / Math.max(preview.width(), preview.height()), width = preview.width() * scale, height = preview.height() * scale;
        g.blit(RenderPipelines.GUI_TEXTURED, preview.id(), (int) (x + (size - width) / 2), (int) (y + (size - height) / 2), 0, 0,
                (int) Math.max(1, width), (int) Math.max(1, height), preview.width(), preview.height(), preview.width(), preview.height(),
                dev.riserecode.ui.theme.Theme.opacity(-1, opacity));
    }
    /** Client shutdown has no outstanding GUI draw. */
    public static void shutdown() { close(); loaded.beginFrame(); }
    public static void close() {
        loaded.discard();pending.clear();failed.clear();
    }
}
