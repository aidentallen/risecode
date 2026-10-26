package dev.riserecode.ui.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.riserecode.ui.render.UiMesh;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.setting.ColorSetting;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipFile;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import org.lwjgl.system.MemoryUtil;

/** Loads the original bundled Qraftys bitmap with optional explicit local overrides.
 * Original bitmap advances and small-cap lowercase glyphs are preserved. A linear-time Euclidean
 * distance transform creates a separate GPU atlas for live shadows/glow; text uses nearest sampling.
 */
final class BitmapPackFont implements AutoCloseable {
    private static final int DENSITY = 2, CELL = 128, SIDE = CELL * 16, PAD = 48;
    private static final String[] ROWS = {
            "", "", " !\"#$%&'()*+,-./", "0123456789:;<=>?", "@ABCDEFGHIJKLMNO", "PQRSTUVWXYZ[\\]^_",
            "`abcdefghijklmno", "pqrstuvwxyz{|}~\0", "", "\0\0\0\0\0\0\0\0\0\0\0\0£\0\0ƒ",
            "\0\0\0\0\0\0ªº\0\0¬\0\0\0«»", "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐", "└┴┬├─┼╞╟╚╔╩╦╠═╬╧",
            "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀", "\0\0\0\0\0\0\0\0\0\0\0\0\0∅∈\0", "≡±≥≤⌠⌡÷≈°∙\0√ⁿ²■\0" };
    private record Glyph(int cell, int advance) { }
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final GpuTexture texture;
    private final GpuTextureView view;

    static Path find(Path configDirectory) {
        Path override = configDirectory.resolve("fonts/qraftys.zip");
        if (Files.isRegularFile(override)) { return override; }
        Path packs = configDirectory.toAbsolutePath().getParent().getParent().resolve("resourcepacks");
        Path canonical = packs.resolve("qraftys-capitalized-font.zip");
        if (Files.isRegularFile(canonical)) { return canonical; }
        if (!Files.isDirectory(packs)) { return null; }
        try (var paths = Files.list(packs)) {
            return paths.filter(Files::isRegularFile).filter(path -> {
                String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.contains("qraft") && name.endsWith(".zip");
            }).sorted().findFirst().orElse(null);
        } catch (IOException ignored) { return null; }
    }

    static boolean prepareNative(Path config,Path root) throws IOException {
        Path pack=find(config);if(pack==null){return true;}
        try(ZipFile zip=new ZipFile(pack.toFile())){
            var entry=zip.getEntry("assets/minecraft/textures/font/ascii.png");
            if(entry==null||entry.getSize()>1024*1024){return false;}
            byte[] bytes;try(var stream=zip.getInputStream(entry)){bytes=stream.readNBytes(1024*1024+1);}
            if(bytes.length>1024*1024){return false;}
            try(NativeImage image=NativeImage.read(bytes)){if(image.getWidth()!=128||image.getHeight()!=128){return false;}}
            Path textures=root.resolve("assets/riserecode/textures/font");Files.createDirectories(textures);Files.write(textures.resolve("qraftys.png"),bytes);
            var provider=new com.google.gson.JsonObject();provider.addProperty("type","bitmap");provider.addProperty("file","riserecode:font/qraftys.png");provider.addProperty("ascent",7);provider.addProperty("height",8);
            var chars=new com.google.gson.JsonArray();for(String row:ROWS){chars.add(row+"\0".repeat(16-row.codePointCount(0,row.length())));}provider.add("chars",chars);
            var providers=new com.google.gson.JsonArray();providers.add(provider);var fallback=new com.google.gson.JsonObject();fallback.addProperty("type","reference");fallback.addProperty("id","minecraft:default");providers.add(fallback);
            var json=new com.google.gson.JsonObject();json.add("providers",providers);Path fonts=root.resolve("assets/riserecode/font");Files.createDirectories(fonts);Files.writeString(fonts.resolve("qraftys.json"),json.toString());return true;
        }
    }

    private static java.io.InputStream open(Path pack) throws IOException {
        if (pack == null) {
            var stream = BitmapPackFont.class.getResourceAsStream("/assets/riserecode/textures/font/qraftys.png");
            if (stream == null) throw new IOException("Bundled Qraftys bitmap missing");
            return stream;
        }
        try (var zip = new ZipFile(pack.toFile())) {
            var entry = zip.getEntry("assets/minecraft/textures/font/ascii.png");
            if (entry == null || entry.getSize() > 1024 * 1024) throw new IOException("Missing/oversized Qraftys bitmap");
            try (var stream = zip.getInputStream(entry)) {
                var bytes = stream.readNBytes(1024 * 1024 + 1);
                if (bytes.length > 1024 * 1024) throw new IOException("Oversized Qraftys bitmap");
                return new java.io.ByteArrayInputStream(bytes);
            }
        }
    }

    BitmapPackFont(Path pack) throws IOException {
        ByteBuffer atlas = MemoryUtil.memCalloc(SIDE * SIDE);
        try (var stream = open(pack); NativeImage image = NativeImage.read(stream)) {
            {
                if (image.getWidth() != 128 || image.getHeight() != 128) { throw new IOException("Expected the Qraftys 16×16 bitmap grid"); }
                for (int row = 0; row < ROWS.length; row++) {
                    int[] codes = ROWS[row].codePoints().toArray();
                    for (int column = 0; column < codes.length; column++) {
                        int code = codes[column]; if (code == 0) { continue; }
                        int cell = row * 16 + column, extent = 0;
                        boolean[] mask = new boolean[CELL * CELL];
                        for (int y = 0; y < 8; y++) {
                            for (int x = 0; x < 8; x++) {
                                if ((image.getPixel(column * 8 + x, row * 8 + y) >>> 24) == 0) { continue; }
                                extent = Math.max(extent, x + 1);
                                for (int dy = 0; dy < DENSITY; dy++) {
                                    for (int dx = 0; dx < DENSITY; dx++) { mask[(PAD + y * DENSITY + dy) * CELL + PAD + x * DENSITY + dx] = true; }
                                }
                            }
                        }
                        glyphs.put(code, new Glyph(cell, code == ' ' ? 4 : extent + 1));
                        if (extent == 0) { continue; }
                        double[] inside = distances(mask, true), outside = distances(mask, false);
                        for (int y = 0; y < CELL; y++) {
                            for (int x = 0; x < CELL; x++) {
                                int index = y * CELL + x;
                                double distance = mask[index] ? Math.sqrt(outside[index]) - 0.5 : 0.5 - Math.sqrt(inside[index]);
                                int value = (int) Math.round(Math.max(0, Math.min(255, 128 + 2.5 * distance)));
                                atlas.put((cell / 16 * CELL + y) * SIDE + cell % 16 * CELL + x, (byte) value);
                            }
                        }
                    }
                }
            }
            texture = RenderSystem.getDevice().createTexture(() -> "risecode Qraftys bitmap SDF", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                    GpuFormat.R8_UNORM, SIDE, SIDE, 1, 1);
            view = RenderSystem.getDevice().createTextureView(texture);
            try (GpuBuffer upload = RenderSystem.getDevice().createBuffer(() -> "risecode bitmap font upload", GpuBuffer.USAGE_COPY_SRC, atlas)) {
                RenderSystem.getDevice().createCommandEncoder().copyBufferToTexture(upload.slice(), 0, 0, SIDE, SIDE, texture, 0, 0, SIDE, SIDE, 0, 0);
            }
        } finally { MemoryUtil.memFree(atlas); }
    }

    /** Squared distance to the nearest selected pixel: separable lower envelopes of parabolas. */
    private static double[] distances(boolean[] mask, boolean selected) {
        double[] result = new double[mask.length], input = new double[CELL], output = new double[CELL];
        int[] vertices = new int[CELL]; double[] boundaries = new double[CELL + 1];
        for (int x = 0; x < CELL; x++) {
            for (int y = 0; y < CELL; y++) { input[y] = mask[y * CELL + x] == selected ? 0 : 1e9; }
            distanceLine(input, output, vertices, boundaries);
            for (int y = 0; y < CELL; y++) { result[y * CELL + x] = output[y]; }
        }
        for (int y = 0; y < CELL; y++) {
            System.arraycopy(result, y * CELL, input, 0, CELL); distanceLine(input, output, vertices, boundaries);
            System.arraycopy(output, 0, result, y * CELL, CELL);
        }
        return result;
    }
    private static void distanceLine(double[] input, double[] output, int[] vertices, double[] boundaries) {
        int k = 0; vertices[0] = 0; boundaries[0] = Double.NEGATIVE_INFINITY; boundaries[1] = Double.POSITIVE_INFINITY;
        for (int q = 1; q < CELL; q++) {
            double intersection;
            do {
                int p = vertices[k]; intersection = ((input[q] + q * q) - (input[p] + p * p)) / (2.0 * (q - p));
                if (intersection > boundaries[k]) { break; }
                k--;
            } while (k >= 0);
            k++; vertices[k] = q; boundaries[k] = intersection; boundaries[k + 1] = Double.POSITIVE_INFINITY;
        }
        k = 0;
        for (int q = 0; q < CELL; q++) {
            while (boundaries[k + 1] < q) { k++; }
            double delta = q - vertices[k]; output[q] = delta * delta + input[vertices[k]];
        }
    }
    double advance(Font fallback, int code) {
        Glyph glyph = glyphs.get(code);
        return glyph == null ? fallback.width(new String(Character.toChars(code))) : glyph.advance;
    }
    double width(Font fallback, String value) { return CustomTextRenderer.bitmapLayout(this, fallback, value).width(); }
    /** Cached with the existing bounded, reload-invalidated text layout. Negative width is fallback. */
    void geometry(int code,double cursor,float[] data,int i){
        Glyph glyph=glyphs.get(code);
        if(glyph==null){data[i+2]=-1;return;}
        if(code==' ')return;
        data[i]=(float)(cursor-PAD/(double)DENSITY);data[i+1]=-PAD/(float)DENSITY;
        data[i+2]=data[i+3]=CELL/(float)DENSITY;data[i+4]=glyph.cell%16/16f;data[i+5]=glyph.cell/16/16f;
        data[i+6]=data[i+4]+1/16f;data[i+7]=data[i+5]+1/16f;
    }
    void draw(GuiGraphicsExtractor graphics, Font fallback, String value, double x, double y, int color, double blur, int mode, boolean gradient, ColorSetting.Paint paint) {
        var run = CustomTextRenderer.bitmapLayout(this, fallback, value); double total = run.width();
        // Body and effects retain their original samplers, bound together so their
        // overlapping painter sequence can share one material and draw call.
        TextureSetup setup = TextureSetup.doubleTexture(view, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST),
                view,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        float[] geometry=run.bitmapCoordinates();
        int blurCode=(int)Math.round(blur*256);
        // The shader's Gaussian tail rounds to exactly zero beyond six sigma.
        // Retain an extra logical pixel for atlas quantization and bilinear support.
        int inset=mode==2?PAD/DENSITY-1:mode==1&&blurCode>0?Math.max(0,PAD/DENSITY-(int)Math.ceil(blurCode/256.0*6+1)):0;
        // Ink is 8×8 at offset 24 in a 64×64 cell: right/bottom padding is
        // eight logical pixels larger. Tighten ordering bounds only, never the quad or UVs.
        inset=Math.max(0,inset-(int)Math.ceil(CustomTextRenderer.stroke()));
        int trailingInset=inset==0?0:inset+(CELL-2*PAD-8*DENSITY)/DENSITY;
        int[] tints=new int[run.codes().length];int first=0;
        for (int i = 0; i < run.codes().length; i++) {
            int code = run.codes()[i]; double cursor = run.offsets()[i];
            int tint = paint != null ? paint.sample(graphics.pose().m20() + (x + cursor) * graphics.pose().m00(), graphics.pose().m21() + y * graphics.pose().m11(), Theme.gradientTime()) : gradient ? Theme.opacity(Theme.gradientAt(graphics.pose().m20() + (x + cursor) * graphics.pose().m00(),
                    graphics.pose().m21() + y * graphics.pose().m11(), cursor / Math.max(1, total)), (color >>> 24) / 255.0) : color;
            tints[i]=dev.riserecode.ui.menu.MenuTransition.color(tint);
            if (geometry[i*8+2]<0) {
                UiMesh.bitmapRun(graphics,CustomTextRenderer.fontPipeline(),setup,geometry,tints,first,i,x,y,blurCode,mode,inset,trailingInset);
                dev.riserecode.ui.render.UiBatch.flush();
                CustomTextRenderer.drawFallbackGlyph(graphics, fallback, new String(Character.toChars(code)), x + cursor, y, tint, blur);
                first=i+1;
            }
        }
        UiMesh.bitmapRun(graphics,CustomTextRenderer.fontPipeline(),setup,geometry,tints,first,run.codes().length,x,y,blurCode,mode,inset,trailingInset);
    }
    @Override public void close() { view.close(); texture.close(); }
}
