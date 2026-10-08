package dev.riserecode.ui.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.theme.ThemeManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Original STB TrueType renderer with filled, oversampled glyphs and fractional kerning.
 * Normal text uses an 8x coverage atlas; a separate distance field supplies soft shadows/glow.
 * Both single-channel atlases upload once per face, never during live theme edits.
 * Optional fonts are loaded from the resource directory or the user's config/fonts directory.
 */
public final class CustomTextRenderer {
    private static final int DENSITY = 2, CELL = 128, SIDE = CELL * 16, PADDING = 48;
    private static final float DISTANCE_SCALE = 2.5f;
    private static final int COVERAGE_DENSITY = 8, COVERAGE_PADDING = 3;
    private static final String SYMBOLS = "…−›•×✓";
    private static final Map<String, Face> FACES = new HashMap<>();
    private static final Map<ThemeManager.Typeface, Boolean> AVAILABLE = new HashMap<>();
    private static BitmapPackFont bitmap;
    private static ThemeManager manager;
    private record LayoutKey(Object face, Font fallback, String text, boolean title) { }
    static record GlyphRun(int[] codes, double[] offsets, double width) { }
    private static final Map<LayoutKey, GlyphRun> LAYOUTS = new LinkedHashMap<>(256, 0.75f, true);
    private static long generation, layoutBuilds, layoutHits;
    public static long generation() { return generation; }
    public static long layoutBuilds() { return layoutBuilds; }
    public static long layoutHits() { return layoutHits; }
    public static int cachedLayouts() { return LAYOUTS.size(); }
    private static GlyphRun layout(Object atlas, Font fallback, String text, boolean title) {
        LayoutKey key = new LayoutKey(atlas, fallback, text, title); GlyphRun cached = LAYOUTS.get(key);
        if (cached != null) { layoutHits++; return cached; }
        int[] codes = text.codePoints().toArray(); double[] offsets = new double[codes.length]; double width = 0; int previous = -1;
        for (int i = 0; i < codes.length; i++) {
            if (atlas instanceof Face face) { width += kern(face, previous, codes[i]); offsets[i] = width; width += advance(face, fallback, codes[i], title); }
            else { offsets[i] = width; width += ((BitmapPackFont) atlas).advance(fallback, codes[i]); }
            previous = codes[i];
        }
        GlyphRun result = new GlyphRun(codes, offsets, width); LAYOUTS.put(key, result); layoutBuilds++;
        if (LAYOUTS.size() > 2048) { LAYOUTS.remove(LAYOUTS.keySet().iterator().next()); }
        return result;
    }
    static GlyphRun bitmapLayout(BitmapPackFont pack, Font font, String text) { return layout(pack, font, text, false); }
    private static final Map<String, String> FAILURES = new HashMap<>();
    private static final class Pipelines {
        private static final RenderPipeline FONT = UiMesh.pipeline("font", "font");
    }
    static RenderPipeline fontPipeline() { return Pipelines.FONT; }
    private record Glyph(int cell, int width, int height, int xOffset, int yOffset, double advance) { }
    private static final class Face implements AutoCloseable {
        private final ByteBuffer data;
        private final STBTTFontinfo info = STBTTFontinfo.malloc();
        private final float scale;
        private final double baseline;
        private final boolean pixel;
        private final Map<Integer, Glyph> glyphs = new HashMap<>();
        private final Map<Integer, Glyph> coverageGlyphs = new HashMap<>();
        private final Map<Long, Double> kerning = new HashMap<>();
        private GpuTexture texture;
        private GpuTextureView view;
        private GpuTexture coverageTexture;
        private GpuTextureView coverageView;
        private Face(byte[] bytes, boolean pixel) {
            validate(bytes);
            this.pixel = pixel;
            data = MemoryUtil.memAlloc(bytes.length); data.put(bytes).flip();
            if (!STBTruetype.stbtt_InitFont(info, data)) { info.free(); MemoryUtil.memFree(data); throw new IllegalArgumentException("Invalid TrueType font"); }
            scale = pixel ? STBTruetype.stbtt_ScaleForMappingEmToPixels(info, 12 * DENSITY)
                    : STBTruetype.stbtt_ScaleForPixelHeight(info, 11 * DENSITY);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var x0 = stack.mallocInt(1); var y0 = stack.mallocInt(1); var x1 = stack.mallocInt(1); var y1 = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointBitmapBox(info, 'H', scale, scale, x0, y0, x1, y1);
                baseline = -y0.get(0) / (double) DENSITY;
            }
        }
        private static void validate(byte[] bytes) {
            if (bytes.length < 12) { throw new IllegalArgumentException("Truncated font header"); }
            ByteBuffer header = ByteBuffer.wrap(bytes);
            int signature = header.getInt(0), tables = Short.toUnsignedInt(header.getShort(4));
            if (signature != 0x00010000 && signature != 0x4F54544F || tables == 0 || 12L + 16L * tables > bytes.length) {
                throw new IllegalArgumentException("Expected a TrueType/OpenType SFNT file");
            }
            for (int index = 0; index < tables; index++) {
                int entry = 12 + index * 16;
                long offset = Integer.toUnsignedLong(header.getInt(entry + 8)), length = Integer.toUnsignedLong(header.getInt(entry + 12));
                if (offset + length > bytes.length) { throw new IllegalArgumentException("Truncated font table"); }
            }
        }
        private double advance(int code) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var width = stack.mallocInt(1); var bearing = stack.mallocInt(1);
                STBTruetype.stbtt_GetCodepointHMetrics(info, code, width, bearing);
                return width.get(0) * scale / DENSITY;
            }
        }
        private double kern(int previous, int next) {
            if (previous < 0) { return 0; }
            long key = ((long) previous << 32) | next;
            return kerning.computeIfAbsent(key, ignored -> STBTruetype.stbtt_GetCodepointKernAdvance(info, previous, next) * (double) scale / DENSITY);
        }
        private void upload() {
            if (coverageView != null) { return; }
            ByteBuffer distancePixels = MemoryUtil.memCalloc(SIDE * SIDE);
            ByteBuffer coveragePixels = MemoryUtil.memCalloc(SIDE * SIDE);
            try {
                int cell = 0;
                for (int code = 32; code < 256; code++) {
                    bake(distancePixels, code, cell); bakeCoverage(coveragePixels, code, cell++);
                }
                for (int code : SYMBOLS.codePoints().toArray()) {
                    bake(distancePixels, code, cell); bakeCoverage(coveragePixels, code, cell++);
                }
                texture = uploadAtlas(distancePixels, "risecode font effects");
                view = RenderSystem.getDevice().createTextureView(texture);
                coverageTexture = uploadAtlas(coveragePixels, "risecode filled font");
                coverageView = RenderSystem.getDevice().createTextureView(coverageTexture);
            } finally { MemoryUtil.memFree(distancePixels); MemoryUtil.memFree(coveragePixels); }
        }
        private static GpuTexture uploadAtlas(ByteBuffer pixels, String name) {
            GpuTexture atlas = RenderSystem.getDevice().createTexture(() -> name,
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.R8_UNORM, SIDE, SIDE, 1, 1);
            try (GpuBuffer upload = RenderSystem.getDevice().createBuffer(() -> name + " upload", GpuBuffer.USAGE_COPY_SRC, pixels)) {
                RenderSystem.getDevice().createCommandEncoder().copyBufferToTexture(upload.slice(), 0, 0, SIDE, SIDE, atlas, 0, 0, SIDE, SIDE, 0, 0);
                return atlas;
            } catch (RuntimeException failure) { atlas.close(); throw failure; }
        }
        /** Filled coverage retains fine stems/counters which a coarse SDF can erase.
         * The transparent gutter prevents adjacent atlas cells bleeding into the glyph.
         */
        private void bakeCoverage(ByteBuffer pixels, int code, int cell) {
            if (STBTruetype.stbtt_FindGlyphIndex(info, code) == 0) { return; }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var w = stack.mallocInt(1); var h = stack.mallocInt(1); var xo = stack.mallocInt(1); var yo = stack.mallocInt(1);
                float rasterScale = scale * COVERAGE_DENSITY / DENSITY;
                ByteBuffer coverage = STBTruetype.stbtt_GetCodepointBitmap(info, rasterScale, rasterScale, code, w, h, xo, yo);
                if (coverage == null) { coverageGlyphs.put(code, new Glyph(cell, 0, 0, 0, 0, advance(code))); return; }
                try {
                    int width = w.get(0) + 2 * COVERAGE_PADDING, height = h.get(0) + 2 * COVERAGE_PADDING;
                    if (width > CELL || height > CELL) { throw new IllegalArgumentException("Font glyph exceeds coverage atlas cell"); }
                    int startX = cell % 16 * CELL + COVERAGE_PADDING, startY = cell / 16 * CELL + COVERAGE_PADDING;
                    for (int y = 0; y < h.get(0); y++) {
                        for (int x = 0; x < w.get(0); x++) { pixels.put((startY + y) * SIDE + startX + x, coverage.get(y * w.get(0) + x)); }
                    }
                    coverageGlyphs.put(code, new Glyph(cell, width, height, xo.get(0) - COVERAGE_PADDING,
                            yo.get(0) - COVERAGE_PADDING, advance(code)));
                } finally { STBTruetype.stbtt_FreeBitmap(coverage); }
            }
        }
        private void bake(ByteBuffer pixels, int code, int cell) {
            if (STBTruetype.stbtt_FindGlyphIndex(info, code) == 0) { return; }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var w = stack.mallocInt(1); var h = stack.mallocInt(1); var xo = stack.mallocInt(1); var yo = stack.mallocInt(1);
                ByteBuffer sdf = STBTruetype.stbtt_GetCodepointSDF(info, scale, code, PADDING, (byte) 128, DISTANCE_SCALE, w, h, xo, yo);
                if (sdf == null) { glyphs.put(code, new Glyph(cell, 0, 0, 0, 0, advance(code))); return; }
                try {
                    if (w.get(0) > CELL || h.get(0) > CELL) { throw new IllegalArgumentException("Font glyph exceeds atlas cell"); }
                    int startX = cell % 16 * CELL, startY = cell / 16 * CELL;
                    for (int y = 0; y < h.get(0); y++) {
                        for (int x = 0; x < w.get(0); x++) { pixels.put((startY + y) * SIDE + startX + x, sdf.get(y * w.get(0) + x)); }
                    }
                    glyphs.put(code, new Glyph(cell, w.get(0), h.get(0), xo.get(0), yo.get(0), advance(code)));
                } finally { STBTruetype.stbtt_FreeSDF(sdf); }
            }
        }
        @Override public void close() {
            if (view != null) { view.close(); }
            if (texture != null) { texture.close(); }
            if (coverageView != null) { coverageView.close(); }
            if (coverageTexture != null) { coverageTexture.close(); }
            info.free(); MemoryUtil.memFree(data);
        }
    }
    private CustomTextRenderer() { }
    public static void configure(ThemeManager value) { manager = value; }
    private static String base(ThemeManager.Typeface font) { return font.name().toLowerCase(Locale.ROOT); }
    public static boolean available(ThemeManager.Typeface font) {
        return AVAILABLE.computeIfAbsent(font, CustomTextRenderer::findAvailable);
    }
    private static boolean findAvailable(ThemeManager.Typeface font) {
        if (font == ThemeManager.Typeface.QRAFTYS) { return manager != null && BitmapPackFont.find(manager.getDirectory()) != null; }
        String file = base(font) + "_regular.ttf";
        return CustomTextRenderer.class.getResource("/assets/riserecode/font/" + file) != null
                || manager != null && (Files.isRegularFile(manager.getFontDirectory().resolve(file))
                || Files.isRegularFile(manager.getFontDirectory().resolve(base(font) + ".ttf")));
    }
    public static String status() {
        ThemeManager.Typeface chosen = selected();
        if (chosen == ThemeManager.Typeface.QRAFTYS) {
            if (FAILURES.containsKey("qraftys_pack")) { return "Invalid Qraftys pack · using Inter"; }
            return available(chosen) ? "Qraftys resource-pack glyphs · nearest sampling"
                    : "Add Qraftys ZIP to resourcepacks · using Inter";
        }
        if (FAILURES.containsKey(base(chosen) + "_regular")) { return "Invalid " + base(chosen) + " font · using Inter"; }
        return available(chosen) ? "STB 8× filled glyphs · live preview"
                : "Install " + base(chosen) + ".ttf in config/riserecode/fonts · using Inter";
    }
    private static ThemeManager.Typeface selected() {
        return manager == null ? ThemeManager.Typeface.INTER : manager.choice("Font Selection", ThemeManager.Typeface.class);
    }
    private static BitmapPackFont bitmap() {
        if (selected() == ThemeManager.Typeface.QRAFTYS && bitmap != null) { return bitmap; }
        if (selected() != ThemeManager.Typeface.QRAFTYS || !available(selected()) || FAILURES.containsKey("qraftys_pack")) { return null; }
        if (bitmap == null) {
            try { bitmap = new BitmapPackFont(BitmapPackFont.find(manager.getDirectory())); }
            catch (IOException | RuntimeException failure) {
                FAILURES.put("qraftys_pack", failure.getMessage());
                org.slf4j.LoggerFactory.getLogger(CustomTextRenderer.class).warn("Could not load Qraftys pack; using Inter", failure);
            }
        }
        return bitmap;
    }
    private static Face face(boolean title) {
        ThemeManager.Typeface typeface = selected();
        if (typeface == ThemeManager.Typeface.QRAFTYS || !available(typeface)) { typeface = ThemeManager.Typeface.INTER; }
        String name = base(typeface) + (title ? "_medium" : "_regular");
        if (FAILURES.containsKey(name)) { return fallbackFace(title); }
        Face cached = FACES.get(name); if (cached != null) { return cached; }
        try {
            Face loaded = new Face(read(name + ".ttf", name.replace("_medium", "_regular") + ".ttf"), typeface == ThemeManager.Typeface.MINECRAFTIA);
            FACES.put(name, loaded); return loaded;
        } catch (IOException | RuntimeException failure) {
            FAILURES.put(name, failure.getMessage());
            org.slf4j.LoggerFactory.getLogger(CustomTextRenderer.class).warn("Could not load {}; using Inter", name, failure);
            return fallbackFace(title);
        }
    }
    private static Face fallbackFace(boolean title) {
        String key = title ? "inter_medium" : "inter_regular";
        return FACES.computeIfAbsent(key, ignored -> {
            try (InputStream stream = CustomTextRenderer.class.getResourceAsStream("/assets/riserecode/font/" + key + ".ttf")) {
                if (stream == null) { throw new IOException("Bundled Inter font missing"); }
                return new Face(stream.readAllBytes(), false);
            } catch (IOException failure) { throw new IllegalStateException("Missing bundled fallback font", failure); }
        });
    }
    private static byte[] read(String preferred, String fallback) throws IOException {
        for (String file : new String[] {preferred, fallback, fallback.replace("_regular", "")}) {
            if (manager != null) {
                Path local = manager.getFontDirectory().resolve(file);
                if (Files.isRegularFile(local)) {
                    if (Files.size(local) > 16 * 1024 * 1024) { throw new IOException("Font file exceeds 16 MiB"); }
                    return Files.readAllBytes(local);
                }
            }
            try (InputStream stream = CustomTextRenderer.class.getResourceAsStream("/assets/riserecode/font/" + file)) {
                if (stream != null) { return stream.readAllBytes(); }
            }
        }
        throw new IOException("Missing " + preferred);
    }
    private static boolean supports(Face face, int code) {
        return (code >= 32 && code < 256 || SYMBOLS.indexOf(code) >= 0)
                && (code == ' ' || STBTruetype.stbtt_FindGlyphIndex(face.info, code) != 0);
    }
    private static double advance(Face face, Font fallback, int code, boolean title) {
        return supports(face, code) ? face.advance(code) : fallback.width(Theme.text(new String(Character.toChars(code)), title));
    }
    private static double kern(Face face, int previous, int next) { return supports(face, previous) && supports(face, next) ? face.kern(previous, next) : 0; }
    public static int width(Font fallback, String original, boolean title) {
        BitmapPackFont pack = bitmap();
        return (int) Math.ceil(layout(pack == null ? face(title) : pack, fallback, original, title).width() * Theme.number("Font Scale", 1));
    }
    /** Describes the loaded font, including fallbacks, rather than the requested menu choice. */
    public static boolean isPixelFont() { return bitmap() != null || face(false).pixel; }
    @FunctionalInterface private interface Layer { void draw(double x, double y, int color, double blur, int mode, boolean gradient, ColorSetting.Paint override); }
    public static void draw(GuiGraphicsExtractor graphics, Font fallback, String original, int color,
            boolean title, boolean shadow, boolean accent) {
        draw(graphics, fallback, original, color, title, shadow, accent, accent);
    }
    public static void draw(GuiGraphicsExtractor graphics, Font fallback, String original, int color,
            boolean title, boolean shadow, boolean accent, boolean glow) {
        drawInternal(graphics, fallback, original, color, title, shadow, accent, glow, accent ? null : Theme.tokenPaint(color));
    }
    public static void drawPaint(GuiGraphicsExtractor graphics, Font fallback, String text, ColorSetting.Paint paint, boolean glow) {
        drawInternal(graphics, fallback, text, paint.sample(graphics.pose().m20(), graphics.pose().m21(), Theme.gradientTime()), false, true, false, glow, paint);
    }
    private static void drawInternal(GuiGraphicsExtractor graphics, Font fallback, String original, int color,
            boolean title, boolean shadow, boolean accent, boolean glow, ColorSetting.Paint paint) {
        if (((color >>> 24) == 0 && paint == null) || original.isEmpty()) { return; }
        BitmapPackFont pack = bitmap();
        Layer layer;
        int normalMode;
        if (pack != null) {
            normalMode = 2;
            layer = (x, y, tint, blur, mode, gradient, override) -> pack.draw(graphics, fallback, original, x, y, tint, blur, mode, gradient, override != null ? override : gradient ? paint : null);
        }
        else {
            Face face = face(title); face.upload();
            normalMode = face.pixel ? 4 : 3;
            layer = (x, y, tint, blur, mode, gradient, override) -> drawFace(graphics, fallback, face, original, x, y, tint, blur, mode, gradient, title, override != null ? override : gradient ? paint : null);
        }
        graphics.pose().pushMatrix();
        float fontScale = (float) Theme.number("Font Scale", 1); graphics.pose().scale(fontScale, fontScale);
        try {
            if (shadow && Theme.bool("Text Shadow", true)) {
                layer.draw(Theme.number("Shadow Offset X", 0.5) / fontScale,
                        Theme.number("Shadow Offset Y", 0.7) / fontScale,
                        Theme.opacity(Theme.manager() == null ? color & 0xFF000000 : Theme.manager().colorSetting("Text Shadow Color").sample(graphics.pose().m20(), graphics.pose().m21()), Theme.number("Shadow Opacity", 0.6) * (color >>> 24) / 255.0),
                        Theme.number("Shadow Blur", 0.65) / fontScale, 1, false,
                        Theme.manager() == null ? null : Theme.paint("Text Shadow Color").opacity(Theme.number("Shadow Opacity", 0.6) * (color >>> 24) / 255.0));
            }
            if (glow && Theme.bool("Global Glow", true) && Theme.number("Glow Radius", 5) > 0) {
                layer.draw(0, 0, Theme.opacity(color, Theme.number("Glow Intensity", 0.2)),
                        Theme.number("Glow Radius", 5) / 3 / fontScale, 1, accent || paint != null,
                        paint == null ? null : paint.opacity(Theme.number("Glow Intensity", 0.2)));
            }
            layer.draw(0, 0, color, 0, normalMode, accent || paint != null, null);
        } finally { graphics.pose().popMatrix(); }
    }
    private static void drawFace(GuiGraphicsExtractor graphics, Font fallback, Face face, String value, double x, double y,
            int color, double blur, int mode, boolean gradient, boolean title, ColorSetting.Paint paint) {
        GlyphRun run = layout(face, fallback, value, title); double total = run.width();
        boolean filled = mode == 3 || mode == 4, pixel = mode == 4;
        int density = filled ? COVERAGE_DENSITY : DENSITY;
        Map<Integer, Glyph> atlasGlyphs = filled ? face.coverageGlyphs : face.glyphs;
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(pixel ? FilterMode.NEAREST : FilterMode.LINEAR);
        TextureSetup setup = TextureSetup.singleTexture(filled ? face.coverageView : face.view, sampler);
        dev.riserecode.ui.render.UiBatch.begin(graphics);
        try { for (int i = 0; i < run.codes().length; i++) {
            int code = run.codes()[i]; double advance = run.offsets()[i]; Glyph glyph = atlasGlyphs.get(code);
            if (!supports(face, code)) {
                dev.riserecode.ui.render.UiBatch.flush();
                drawFallbackGlyph(graphics, fallback, new String(Character.toChars(code)), x + advance, y, paint == null ? color : paint.sample(graphics.pose().m20() + advance * graphics.pose().m00(), graphics.pose().m21(), Theme.gradientTime()), blur);
            }
            if (glyph != null && glyph.width > 0 && code != ' ') {
                double gx = x + advance + glyph.xOffset / (double) density;
                double gy = y + face.baseline + glyph.yOffset / (double) density;
                if (pixel) { gx = Math.rint(gx); gy = Math.rint(gy); }
                int tint = paint != null ? paint.sample(graphics.pose().m20() + (x + advance) * graphics.pose().m00(), graphics.pose().m21() + y * graphics.pose().m11(), Theme.gradientTime()) : gradient ? Theme.opacity(Theme.gradientAt(graphics.pose().m20() + advance * graphics.pose().m00(), graphics.pose().m21(), advance / Math.max(1, total)), (color >>> 24) / 255.0) : color;
                float u = glyph.cell % 16 * CELL / (float) SIDE, v = glyph.cell / 16 * CELL / (float) SIDE;
                UiMesh.quad(graphics, Pipelines.FONT, setup, gx, gy, glyph.width / (double) density,
                        glyph.height / (double) density, u, v, u + glyph.width / (float) SIDE, v + glyph.height / (float) SIDE,
                        tint, tint, (int) Math.round(blur * 256), mode, 0, 0);
            }
        }
        } finally { dev.riserecode.ui.render.UiBatch.end(); }
    }
    /** Unicode outside the cached Latin atlas retains the selected font for its neighbouring glyphs.
     * Native fallback shadows use the same offsets and a normalized Gaussian stencil.
     */
    static void drawFallbackGlyph(GuiGraphicsExtractor graphics, Font font, String glyph, double x, double y, int color, double blur) {
        int radius = blur > 0.05 ? 3 : 0; double sum = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) { sum += Math.exp(-(dx * dx + dy * dy) / 2.0); }
        }
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int tint = Theme.opacity(color, Math.exp(-(dx * dx + dy * dy) / 2.0) / sum);
                if ((tint >>> 24) == 0) { continue; }
                graphics.pose().pushMatrix(); graphics.pose().translate((float) (x + dx * blur), (float) (y + dy * blur));
                graphics.text(font, Theme.text(glyph, false), 0, 0, tint, false); graphics.pose().popMatrix();
            }
        }
    }
    public static void close() {
        LAYOUTS.clear(); generation++;
        FACES.values().forEach(Face::close); FACES.clear(); FAILURES.clear(); AVAILABLE.clear();
        if (bitmap != null) { bitmap.close(); bitmap = null; }
    }
}
