package dev.riserecode.ui.theme;

import com.mojang.blaze3d.platform.NativeImage;
import dev.riserecode.util.RenderUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Local content-addressed imports and one cached GPU texture. No per-frame image decoding. */
public final class BrandingLogo {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("riserecode", "branding/logo");
    private static String loaded = "", status = "No logo";
    private static long stamp = -1, nextCheck, loads;
    private static int width, height;
    private static DynamicTexture texture;
    private BrandingLogo() { }
    public static String importPng(Path directory, Path source) throws IOException {
        if (!Files.isRegularFile(source) || Files.size(source) > 16_777_216) { throw new IOException("Choose a PNG smaller than 16 MiB"); }
        byte[] bytes = Files.readAllBytes(source);
        byte[] signature = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        if (bytes.length < 24 || !java.util.Arrays.equals(signature, java.util.Arrays.copyOf(bytes, 8))) { throw new IOException("Logo must be a PNG"); }
        var header = java.nio.ByteBuffer.wrap(bytes); int w = header.getInt(16), h = header.getInt(20);
        if (w < 1 || h < 1 || w > 4096 || h > 4096) { throw new IOException("Logo must be 1–4096 pixels per side"); }
        try (var image = NativeImage.read(bytes)) { if (image.getWidth() != w || image.getHeight() != h) { throw new IOException("Invalid PNG dimensions"); } }
        String name;
        try { name = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)) + ".png"; }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        Path folder = directory.resolve("branding"); Files.createDirectories(folder);
        Path destination = folder.resolve(name);
        if (!Files.exists(destination)) {
            Path temporary = Files.createTempFile(folder, "logo-", ".tmp");
            try { Files.write(temporary, bytes); Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
            finally { Files.deleteIfExists(temporary); }
        }
        return name;
    }
    public static boolean present() { refresh(); return texture != null && Theme.bool("Logo Visible", true); }
    public static long loads() { return loads; }
    public static String status() { return status; }
    public static void invalidate() { nextCheck = 0; stamp = -1; }
    private static void refresh() {
        var theme = Theme.manager(); if (theme == null) { return; }
        String requested = theme.text("Logo File"); long now = System.nanoTime();
        if (loaded.equals(requested) && now < nextCheck) { return; }
        nextCheck = now + 1_000_000_000L;
        if (!requested.matches("[a-f0-9]{64}\\.png")) { close(); loaded = requested; status = requested.isBlank() ? "No logo" : "Use Import Logo to copy this PNG"; return; }
        Path file = theme.getDirectory().resolve("branding").resolve(requested);
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (texture != null && loaded.equals(requested) && stamp == modified) { return; }
            close(); var image = NativeImage.read(Files.readAllBytes(file)); width = image.getWidth(); height = image.getHeight();
            texture = new DynamicTexture(() -> "risecode branding", image); Minecraft.getInstance().getTextureManager().register(ID, texture);
            loaded = requested; stamp = modified; loads++; status = width + " × " + height + " PNG";
        } catch (IOException | RuntimeException failure) { close(); loaded = requested; status = "Logo unavailable: " + failure.getMessage(); }
    }
    public static void draw(GuiGraphicsExtractor g, double x, double y, double size, double opacity) {
        refresh(); if (texture == null || !Theme.bool("Logo Visible", true)) { return; }
        double w = size * width / Math.max(width, height), h = size * height / Math.max(width, height);
        g.blit(RenderPipelines.GUI_TEXTURED, ID, (int)x, (int)y, 0, 0, (int)Math.ceil(w), (int)Math.ceil(h), width, height, width, height,
                Theme.opacity(Theme.manager().colorSetting("Logo Tint").sample(x, y), opacity));
    }
    public static void drawBeside(GuiGraphicsExtractor g, double x, double y, double textWidth, double opacity) {
        double size = Theme.number("Logo Size", 16);
        var placement = Theme.manager().choice("Logo Placement", ThemeManager.LogoPlacement.class);
        draw(g, placement == ThemeManager.LogoPlacement.RIGHT ? x + textWidth + 4 : placement == ThemeManager.LogoPlacement.LEFT ? Math.max(0, x - size - 4) : x + (textWidth - size) / 2,
                placement == ThemeManager.LogoPlacement.ABOVE ? y - size - 3 : y, size, opacity);
    }
    public static void close() {
        if (texture != null) { Minecraft.getInstance().getTextureManager().release(ID); texture = null; }
        stamp = -1;
    }
}
