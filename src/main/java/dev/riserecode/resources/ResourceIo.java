package dev.riserecode.resources;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Atomic local storage; all paths are owned by the Resources cache. */
public final class ResourceIo {
    private ResourceIo() { }
    public static JsonObject read(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path)) { return JsonParser.parseReader(reader).getAsJsonObject(); }
    }
    public static void write(Path path, JsonObject data) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "resources-", ".tmp");
        try {
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(data));
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static String hash(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static void writeBytes(Path path,byte[] bytes) throws IOException {
        Files.createDirectories(path.getParent());Path temporary=Files.createTempFile(path.getParent(),"resources-",".tmp");
        try { Files.write(temporary,bytes);try { Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(java.nio.file.AtomicMoveNotSupportedException ignored){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
        } finally { Files.deleteIfExists(temporary); }
    }
    public static Path child(Path root, String relative) throws IOException {
        if (relative.isBlank() || relative.contains("\\") || relative.startsWith("/") || relative.contains(":")
                || java.util.Arrays.stream(relative.split("/")).anyMatch(s -> s.equals("..") || s.equals("."))) {
            throw new IOException("Invalid resource path: " + relative);
        }
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root.normalize())) { throw new IOException("Resource escapes its pack"); }
        return path;
    }
}
