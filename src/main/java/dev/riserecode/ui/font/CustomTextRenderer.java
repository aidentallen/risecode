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
    static record GlyphRun(int[] codes, double[] offsets, double width, float[] bitmapCoordinates) { }
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
        float[] bitmapCoordinates=null;
        if(atlas instanceof BitmapPackFont pack){
            bitmapCoordinates=new float[codes.length*8];
            for(int i=0;i<codes.length;i++){pack.geometry(codes[i],offsets[i],bitmapCoordinates,i*8);}
        }
        GlyphRun result = new GlyphRun(codes, offsets, width, bitmapCoordinates); LAYOUTS.put(key, result); layoutBuilds++;
        if (LAYOUTS.size() > 2048) { LAYOUTS.remove(LAYOUTS.keySet().iterator().next()); }
        return result;
    }
    static GlyphRun bitmapLayout(BitmapPackFont pack, Font font, String text) { return layout(pack, font, text, false); }
    private static final Map<String, String> FAILURES = new HashMap<>();
    private static final class Pipelines {
        private static final RenderPipeline FONT = UiMesh.pipeline("font", "font");
        private static final RenderPipeline BITMAP = net.minecraft.client.renderer.RenderPipelines.register(RenderPipeline.builder(net.minecraft.client.renderer.RenderPipelines.GUI_SNIPPET)
                .withLocation(UiMesh.id("pipeline/bitmap_font")).withVertexShader(UiMesh.id("core/bitmap_font"))
                .withFragmentShader(UiMesh.id("core/bitmap_font")).withVertexBinding(0,UiMesh.BITMAP_FORMAT)
                .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.SAMPLER0_SAMPLER1)
                .withPrimitiveTopology(com.mojang.blaze3d.PrimitiveTopology.QUADS).build());
    }
    static RenderPipeline fontPipeline() { return Pipelines.BITMAP; }
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
            // Minecraftia's em includes unusually large ascender space. Normalize the visible
            // capital height to the shared seven-unit menu/HUD cap, not its em box.
            if (pixel) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    var x0 = stack.mallocInt(1); var y0 = stack.mallocInt(1); var x1 = stack.mallocInt(1); var y1 = stack.mallocInt(1);
                    STBTruetype.stbtt_GetCodepointBox(info, 'H', x0, y0, x1, y1);
                    scale = (float) (7.0 * DENSITY / Math.max(1, y1.get(0) - y0.get(0)));
                }
            } else { scale = STBTruetype.stbtt_ScaleForPixelHeight(info, 11 * DENSITY); }
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
    private static boolean branding;
    private record BrandKey(Face face,String text,int density){}
    private record BrandRaster(GpuTexture texture,GpuTextureView view,int width,int height,int density) implements AutoCloseable {
        public void close(){view.close();texture.close();}
    }
    private static final Map<BrandKey,BrandRaster> BRANDS=new LinkedHashMap<>();
    public static void drawBrand(GuiGraphicsExtractor g,Font font,String text,int color){
        branding=true;try{draw(g,font,text,color,false,true,true);}finally{branding=false;}
    }
    private static BrandRaster raster(Face face,Font font,String text,int density){
        BrandKey key=new BrandKey(face,text,density);BrandRaster cached=BRANDS.get(key);if(cached!=null){return cached;}
        GlyphRun run=layout(face,font,text,false);int pad=4,w=(int)Math.ceil(run.width()*density)+pad*2,h=16*density+pad*2;
        ByteBuffer pixels=MemoryUtil.memCalloc(w*h);
        try(MemoryStack stack=MemoryStack.stackPush()){
            var gw=stack.mallocInt(1);var gh=stack.mallocInt(1);var gx=stack.mallocInt(1);var gy=stack.mallocInt(1);
            for(int i=0;i<run.codes().length;i++){
                ByteBuffer bitmap=STBTruetype.stbtt_GetCodepointBitmap(face.info,face.scale*density/DENSITY,face.scale*density/DENSITY,run.codes()[i],gw,gh,gx,gy);
                if(bitmap==null){continue;}
                try{
                    int left=(int)Math.round(run.offsets()[i]*density)+gx.get(0)+pad,top=(int)Math.round(face.baseline*density)+gy.get(0)+pad;
                    for(int y=0;y<gh.get(0);y++){for(int x=0;x<gw.get(0);x++){int px=left+x,py=top+y;if(px>=0&&px<w&&py>=0&&py<h){int index=py*w+px;pixels.put(index,(byte)Math.max(pixels.get(index)&255,bitmap.get(y*gw.get(0)+x)&255));}}}
                }finally{STBTruetype.stbtt_FreeBitmap(bitmap);}
            }
            GpuTexture texture=RenderSystem.getDevice().createTexture(()->"risecode title coverage",GpuTexture.USAGE_TEXTURE_BINDING|GpuTexture.USAGE_COPY_DST,GpuFormat.R8_UNORM,w,h,1,1);
            try(GpuBuffer upload=RenderSystem.getDevice().createBuffer(()->"risecode title upload",GpuBuffer.USAGE_COPY_SRC,pixels)){
                RenderSystem.getDevice().createCommandEncoder().copyBufferToTexture(upload.slice(),0,0,w,h,texture,0,0,w,h,0,0);
            }catch(RuntimeException error){texture.close();throw error;}
            BrandRaster result=new BrandRaster(texture,RenderSystem.getDevice().createTextureView(texture),w,h,density);
            if(BRANDS.size()>=8){BrandKey first=BRANDS.keySet().iterator().next();BRANDS.remove(first).close();}BRANDS.put(key,result);return result;
        }finally{MemoryUtil.memFree(pixels);}
    }
    private CustomTextRenderer() { }
    public static void configure(ThemeManager value) { manager = value; }
    public static boolean prepareNativeBitmap(Path config,Path root) throws IOException {return BitmapPackFont.prepareNative(config,root);}
    private static String base(ThemeManager.Typeface font) { return font.name().toLowerCase(Locale.ROOT); }
    public static boolean available(ThemeManager.Typeface font) {
        return AVAILABLE.computeIfAbsent(font, CustomTextRenderer::findAvailable);
    }
    private static boolean findAvailable(ThemeManager.Typeface font) {
        if (font == ThemeManager.Typeface.QRAFTYS) { return CustomTextRenderer.class.getResource("/assets/riserecode/textures/font/qraftys.png") != null; }
        String file = base(font) + "_regular.ttf";
        return CustomTextRenderer.class.getResource("/assets/riserecode/font/" + file) != null
                || manager != null && (Files.isRegularFile(manager.getFontDirectory().resolve(file))
                || Files.isRegularFile(manager.getFontDirectory().resolve(base(font) + ".ttf")));
    }
    public static String status() {
        ThemeManager.Typeface chosen = selected();
        if (chosen == ThemeManager.Typeface.QRAFTYS) {
            if (FAILURES.containsKey("qraftys_pack")) { return "Invalid Qraftys pack · using Inter"; }
            return available(chosen) ? "Qraftys original bitmap · bundled offline"
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
            try { bitmap = new BitmapPackFont(manager == null ? null : BitmapPackFont.find(manager.getDirectory())); }
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
        return supports(face, code) ? face.advance(code) : fallback.width(new String(Character.toChars(code)));
    }
    private static double kern(Face face, int previous, int next) { return supports(face, previous) && supports(face, next) ? face.kern(previous, next) : 0; }
    public static int width(Font fallback, String original, boolean title) {
        if(dev.riserecode.ui.theme.PageAppearance.plain()){return fallback.width(original);}
        if (original.indexOf('§') >= 0) { return (int)Math.ceil(StyledText.of(original).width(fallback, title)); }
        BitmapPackFont pack = bitmap();
        return (int) Math.ceil(layout(pack == null ? face(title) : pack, fallback, original, title).width() * Theme.number("Font Scale", 1));
    }
    /** Per-line measurement uses the same advances, kerning and rounding as width(), without caching every prefix. */
    public static final class TextMeasure {
        private final Object atlas;
        private final Font fallback;
        private final boolean title;
        private final double scale;
        private double rawWidth;
        private int previous=-1;
        private TextMeasure(Object atlas,Font fallback,boolean title,double scale){this.atlas=atlas;this.fallback=fallback;this.title=title;this.scale=scale;}
        public double append(int codepoint){
            if(atlas instanceof Face face){rawWidth+=kern(face,previous,codepoint);rawWidth+=advance(face,fallback,codepoint,title);}
            else{rawWidth+=((BitmapPackFont)atlas).advance(fallback,codepoint);}
            previous=codepoint;return Math.ceil(rawWidth*scale);
        }
        public void reset(){rawWidth=0;previous=-1;}
    }
    public static TextMeasure measure(Font fallback,boolean title){
        BitmapPackFont pack=bitmap();return new TextMeasure(pack==null?face(title):pack,fallback,title,Theme.number("Font Scale",1));
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
        if (dev.riserecode.ui.theme.PageAppearance.plain()){graphics.text(fallback,net.minecraft.network.chat.Component.literal(original),0,0,color,shadow);return;}
        if (original.indexOf('§') >= 0) { StyledText.of(original).draw(graphics, fallback, 0, 0, 1, color); return; }
        if (((color >>> 24) == 0 && (paint == null || (paint.primary()>>>24)==0&&(paint.secondary()>>>24)==0)) || original.isEmpty()) { return; }
        double visibility=1;var clip=graphics.scissorStack.peek();
        if(clip!=null){
            var source=new net.minecraft.client.gui.navigation.ScreenRectangle(0,0,Math.max(1,width(fallback,original,title)),(int)Math.ceil(10*Theme.number("Font Scale",1))).transformMaxBounds(graphics.pose());
            var visible=source.intersection(clip);if(visible==null){return;}
            visibility=Math.min(1,visible.height()/(double)Math.max(1,source.height()));
        }
        final double glowVisibility=visibility;
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
                UiMesh.effect(() -> layer.draw(0, 0, Theme.opacity(color, Theme.number("Glow Intensity", 0.2)*glowVisibility),
                        Theme.number("Glow Radius", 5) / 3 / fontScale, 1, accent || paint != null,
                        paint == null ? null : paint.opacity(Theme.number("Glow Intensity", 0.2)*glowVisibility)));
            }
            layer.draw(0, 0, color, 0, normalMode, accent || paint != null, null);
        } finally { graphics.pose().popMatrix(); }
    }
    private static void drawFace(GuiGraphicsExtractor graphics, Font fallback, Face face, String value, double x, double y,
            int color, double blur, int mode, boolean gradient, boolean title, ColorSetting.Paint paint) {
        GlyphRun run = layout(face, fallback, value, title); double total = run.width();
        if (stroke() > 0 && (mode == 3 || mode == 4)) { mode = 0; }
        boolean filled = mode == 3 || mode == 4;
        double framebufferScale = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        double physical = Math.hypot(graphics.pose().m00(), graphics.pose().m01()) * framebufferScale;
        if(branding&&filled&&!face.pixel&&value.codePoints().allMatch(code->supports(face,code))){
            int density=Math.clamp(Integer.highestOneBit(Math.max(8,(int)Math.ceil(physical))*2-1),8,64);
            double logicalWidth=Math.max(1,layout(face,fallback,value,false).width());density=Math.max(1,Math.min(density,(int)(4090/logicalWidth)));
            BrandRaster image=raster(face,fallback,value,density);
            int first=paint!=null?paint.sample(graphics.pose().m20(),graphics.pose().m21(),Theme.gradientTime()):gradient?Theme.opacity(Theme.gradientAt(graphics.pose().m20(),graphics.pose().m21(),0),(color>>>24)/255.0):color;
            int last=paint!=null?paint.sample(graphics.pose().m20()+logicalWidth*graphics.pose().m00(),graphics.pose().m21(),Theme.gradientTime()):gradient?Theme.opacity(Theme.gradientAt(graphics.pose().m20()+logicalWidth*graphics.pose().m00(),graphics.pose().m21(),1),(color>>>24)/255.0):color;
            UiMesh.quad(graphics,Pipelines.FONT,TextureSetup.singleTexture(image.view(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),x-4.0/density,y-4.0/density,image.width()/(double)density,image.height()/(double)density,0,0,1,1,first,last,0,3,0,0,true);return;
        }
        boolean pixel = mode == 4 && Math.abs(physical - Math.rint(physical)) < 0.015 && physical >= 0.99;
        if (mode == 4 && !pixel) { mode = 3; }
        int density = filled ? COVERAGE_DENSITY : DENSITY;
        Map<Integer, Glyph> atlasGlyphs = filled ? face.coverageGlyphs : face.glyphs;
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(pixel ? FilterMode.NEAREST : FilterMode.LINEAR);
        TextureSetup setup = TextureSetup.singleTexture(filled ? face.coverageView : face.view, sampler);
        var glyphRun=new UiMesh.GlyphRun(graphics,Pipelines.FONT,setup,run.codes().length,(int)Math.round(blur*256),mode);
        dev.riserecode.ui.render.UiBatch.begin(graphics);
        try { for (int i = 0; i < run.codes().length; i++) {
            int code = run.codes()[i]; double advance = run.offsets()[i]; Glyph glyph = atlasGlyphs.get(code);
            if (!supports(face, code)) {
                glyphRun.flush();
                dev.riserecode.ui.render.UiBatch.flush();
                drawFallbackGlyph(graphics, fallback, new String(Character.toChars(code)), x + advance, y, paint == null ? color : paint.sample(graphics.pose().m20() + advance * graphics.pose().m00(), graphics.pose().m21(), Theme.gradientTime()), blur);
            }
            if (glyph != null && glyph.width > 0 && code != ' ') {
                double gx = x + advance + glyph.xOffset / (double) density;
                double gy = y + face.baseline + glyph.yOffset / (double) density;
                if (pixel) {
                    double sx = graphics.pose().m00() * framebufferScale, sy = graphics.pose().m11() * framebufferScale;
                    if (Math.abs(sx) > 0.001 && Math.abs(sy) > 0.001) {
                        gx += (Math.rint(gx * sx + graphics.pose().m20() * framebufferScale) - (gx * sx + graphics.pose().m20() * framebufferScale)) / sx;
                        gy += (Math.rint(gy * sy + graphics.pose().m21() * framebufferScale) - (gy * sy + graphics.pose().m21() * framebufferScale)) / sy;
                    }
                }
                int tint = paint != null ? paint.sample(graphics.pose().m20() + (x + advance) * graphics.pose().m00(), graphics.pose().m21() + y * graphics.pose().m11(), Theme.gradientTime()) : gradient ? Theme.opacity(Theme.gradientAt(graphics.pose().m20() + advance * graphics.pose().m00(), graphics.pose().m21(), advance / Math.max(1, total)), (color >>> 24) / 255.0) : color;
                float u = glyph.cell % 16 * CELL / (float) SIDE, v = glyph.cell / 16 * CELL / (float) SIDE;
                glyphRun.add(gx, gy, glyph.width / (double) density,
                        glyph.height / (double) density, u, v, u + glyph.width / (float) SIDE, v + glyph.height / (float) SIDE, tint);
            }
        }
        } finally { glyphRun.flush(); dev.riserecode.ui.render.UiBatch.end(); }
    }
    /** Unicode outside the cached Latin atlas retains the selected font for its neighbouring glyphs.
     * Native fallback shadows use the same offsets and a normalized Gaussian stencil.
     */
    private record FallbackKey(Font font,String glyph,float a,float b,float c,float d,float x,float y,
            net.minecraft.client.gui.navigation.ScreenRectangle clip,int color,double opacity){ }
    private static final Map<FallbackKey,java.util.List<CachedFallback>> FALLBACKS=new LinkedHashMap<>(128,.75f,true);
    private record CachedFallback(RenderPipeline pipeline,TextureSetup textureSetup,org.joml.Matrix4f pose,
            net.minecraft.client.gui.font.TextRenderable renderable,net.minecraft.client.gui.navigation.ScreenRectangle scissorArea,
            net.minecraft.client.gui.navigation.ScreenRectangle bounds) implements dev.riserecode.ui.menu.MenuTransition.Captured {
        public void buildVertices(com.mojang.blaze3d.vertex.VertexConsumer vertices){renderable.render(pose,vertices,15728880,true);}
    }
    private static final double[] FALLBACK_WEIGHTS=fallbackWeights();
    private static double[] fallbackWeights(){double[] weights=new double[49];double sum=0;for(int y=-3;y<=3;y++)for(int x=-3;x<=3;x++){double weight=Math.exp(-(x*x+y*y)/2.0);weights[(y+3)*7+x+3]=weight;sum+=weight;}for(int i=0;i<weights.length;i++)weights[i]/=sum;return weights;}
    private static void fallbackText(GuiGraphicsExtractor g,Font font,String glyph,int color){
        // Container label adapters retain their existing interception/re-entry semantics.
        if(dev.riserecode.ui.menu.ContainerTheme.active()){g.text(font,net.minecraft.network.chat.Component.literal(glyph),0,0,color,false);return;}
        var p=g.pose();var clip=g.scissorStack.peek();
        var key=new FallbackKey(font,glyph,p.m00(),p.m01(),p.m10(),p.m11(),p.m20(),p.m21(),clip,color,dev.riserecode.ui.menu.MenuTransition.opacity());
        var states=FALLBACKS.get(key);
        if(states==null){
            var state=new net.minecraft.client.renderer.state.gui.GuiTextRenderState(font,net.minecraft.network.chat.Component.literal(glyph).getVisualOrderText(),new org.joml.Matrix3x2f(p),0,0,color,0,false,false,clip);
            var prepared=state.ensurePrepared();var bounds=state.bounds();var result=new java.util.ArrayList<CachedFallback>();
            if(bounds!=null){var matrix=new org.joml.Matrix4f().mul(p);prepared.visit(new Font.GlyphVisitor(){
                @Override public void acceptRenderable(net.minecraft.client.gui.font.TextRenderable renderable){result.add(new CachedFallback(renderable.guiPipeline(),TextureSetup.singleTextureWithLightmap(renderable.textureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),matrix,renderable,clip,bounds));}
            });}
            states=java.util.List.copyOf(result);FALLBACKS.put(key,states);if(FALLBACKS.size()>1024){FALLBACKS.remove(FALLBACKS.keySet().iterator().next());}
        }
        for(var state:states){dev.riserecode.ui.render.UiBatch.submit(g,state);}
    }
    public static double stroke() { return Theme.number("Font Boldness", 0) * .0075; }
    static void drawFallbackGlyph(GuiGraphicsExtractor graphics, Font font, String glyph, double x, double y, int color, double blur) {
        int radius = blur > 0.05 ? 3 : 0;
        dev.riserecode.ui.render.UiBatch.begin(graphics);
        try{for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int tint = Theme.opacity(color, radius==0?1:FALLBACK_WEIGHTS[(dy+3)*7+dx+3]);
                if ((tint >>> 24) == 0) { continue; }
                graphics.pose().pushMatrix(); graphics.pose().translate((float) (x + dx * blur), (float) (y + dy * blur));
                fallbackText(graphics,font,glyph,tint);
                double stroke = stroke();
                if (stroke > 0) for (int tap = 0; tap < 8; tap++) {
                    double angle = tap * Math.PI / 4;
                    graphics.pose().pushMatrix();
                    graphics.pose().translate((float)(Math.cos(angle)*stroke),(float)(Math.sin(angle)*stroke));
                    fallbackText(graphics,font,glyph,tint); graphics.pose().popMatrix();
                }
                graphics.pose().popMatrix();
            }
        }}finally{dev.riserecode.ui.render.UiBatch.end();}
    }
    public static void close() {
        dev.riserecode.ui.render.UiBatch.clearPlans();
        BRANDS.values().forEach(BrandRaster::close);BRANDS.clear();
        LAYOUTS.clear();FALLBACKS.clear(); generation++;
        FACES.values().forEach(Face::close); FACES.clear(); FAILURES.clear(); AVAILABLE.clear();
        if (bitmap != null) { bitmap.close(); bitmap = null; }
    }
}
