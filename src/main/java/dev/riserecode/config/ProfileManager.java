package dev.riserecode.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;

/** Profiles contain module/HUD choices; friends and command prefix remain global. */
public final class ProfileManager implements AutoCloseable {
    private final Path directory;
    private final ConfigManager config;
    private final Runnable refreshUi;
    private final Logger logger;
    private final java.util.concurrent.ExecutorService io=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread thread=new Thread(r,"risecode-profile-io");thread.setDaemon(true);return thread;});
    public record Prepared(String name,String document,String fingerprint){ }
    public java.util.concurrent.CompletionStage<List<String>> listAsync() { return submitIo(this::getProfiles); }
    public java.util.concurrent.CompletionStage<Prepared> prepareAsync(String name) {
        Path file = getFile(name);
        return submitIo(() -> {
            byte[] bytes = readProfile(file);
            var document = parseDocument(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            backupLegacyDocument(file, document);
            return new Prepared(name, document.toString(), fingerprint(bytes));
        });
    }
    public java.util.concurrent.CompletionStage<Boolean> savePrepared(String name, String document, String expected) {
        Path file = getFile(name);
        return submitIo(() -> {
            try {
                var parsed = parseDocument(document);
                String current = currentFingerprint(file);
                if (!current.equals(expected)) { return false; }
                backupCurrent(file, name, current);
                JsonStorage.write(file, parsed);
                return readDocument(file).equals(parsed);
            } catch (IOException | RuntimeException failure) { logger.warn("Could not save prepared profile {}", name, failure); return false; }
        });
    }
    public java.util.concurrent.CompletionStage<Boolean> deletePrepared(String name, String expected) {
        Path file = getFile(name);
        return submitIo(() -> {
            try {
                String current = currentFingerprint(file);
                if (current.isEmpty() || !current.equals(expected)) { return false; }
                backupCurrent(file, name, current);
                Files.delete(file);
                return true;
            } catch (IOException | RuntimeException failure) { logger.warn("Could not delete profile {}", name, failure); return false; }
        }).thenApplyAsync(success -> { if (success) { config.clearActiveProfile(name); } return success; }, config.clientExecutor());
    }
    public java.util.concurrent.CompletionStage<String> fingerprintAsync(String name) {
        Path file = getFile(name);
        return submitIo(() -> currentFingerprint(file));
    }
    private <T> java.util.concurrent.CompletableFuture<T> submitIo(java.util.concurrent.Callable<T> action) {
        var result = new java.util.concurrent.CompletableFuture<T>();
        try { io.execute(() -> { try { result.complete(action.call()); } catch (Exception failure) { result.completeExceptionally(failure); } }); }
        catch (java.util.concurrent.RejectedExecutionException stopped) { result.completeExceptionally(stopped); }
        return result;
    }
    private static com.google.gson.JsonObject parseDocument(String text) throws IOException {
        if (text.length() > 1_048_576 || text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1_048_576) { throw new IOException("Profile exceeds 1 MiB"); }
        return com.google.gson.JsonParser.parseString(text).getAsJsonObject();
    }
    private static String currentFingerprint(Path file) throws IOException { return Files.exists(file) ? fingerprint(readProfile(file)) : ""; }
    private static void backupCurrent(Path file, String name, String fingerprint) throws IOException {
        if (!fingerprint.isEmpty()) { Files.copy(file, file.resolveSibling(name+".backup-"+java.util.UUID.randomUUID()+".json.bak")); }
    }
    private static void backupLegacyDocument(Path file, com.google.gson.JsonObject document) throws IOException {
        Path backup = file.resolveSibling(file.getFileName()+".pre-resources");
        if (!document.has("resources") && !Files.exists(backup)) { Files.copy(file, backup); }
    }
    private static String fingerprint(byte[] bytes){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}

    public ProfileManager(ConfigManager config, Runnable refreshUi, Logger logger) {
        this.config = Objects.requireNonNull(config); this.refreshUi = Objects.requireNonNull(refreshUi); this.logger = logger;
        directory = config.getDirectory().resolve("profiles");
        try { Files.createDirectories(directory); }
        catch (IOException failure) { logger.error("Could not create profile directory {}", directory, failure); }
    }
    public Path getDirectory() { return directory; }
    public Path getFile(String name) {
        if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}")) {
            throw new IllegalArgumentException("Profile names must contain 1–32 letters, digits, hyphens, or underscores");
        }
        return directory.resolve(name + ".json");
    }
    public boolean saveProfile(String name) {
        Path file = getFile(name);
        try { backupLegacy(file); JsonStorage.write(file, config.profileSnapshot()); config.setActiveProfile(name); return true; }
        catch (IOException | RuntimeException failure) { logger.error("Could not save profile {}", name, failure); return false; }
    }
    public boolean loadProfile(String name) {
        Path file = getFile(name);
        if (!Files.isRegularFile(file)) { return false; }
        try { config.applyProfile(readDocument(file)); }
        catch (IOException | RuntimeException failure) { logger.error("Could not load profile {}", name, failure); return false; }
        config.setActiveProfile(name); refreshUi.run();
        return true;
    }
    public java.util.concurrent.CompletableFuture<Boolean> loadProfileAsync(String name) {
        var result = new java.util.concurrent.CompletableFuture<Boolean>();
        try {
            prepareAsync(name).whenCompleteAsync((prepared, readFailure) -> {
                if (readFailure != null) { logger.warn("Could not read profile {}", name, readFailure); result.complete(false); return; }
                config.applyProfileAsync(com.google.gson.JsonParser.parseString(prepared.document()).getAsJsonObject())
                        .whenCompleteAsync((success, failure) -> {
                            try {
                                boolean ok = failure == null && Boolean.TRUE.equals(success);
                                if (ok) { config.setActiveProfile(name); refreshUi.run(); }
                                result.complete(ok);
                            } catch (RuntimeException invalid) { result.complete(false); }
                        }, config.clientExecutor());
            }, config.clientExecutor());
        } catch (RuntimeException invalid) { config.clientExecutor().execute(() -> result.complete(false)); }
        return result;
    }

    private static byte[] readProfile(Path file) throws IOException {
        try (var input = Files.newInputStream(file)) {
            byte[] data = input.readNBytes(1_048_577);
            if (data.length > 1_048_576) { throw new IOException("Profile exceeds 1 MiB"); }
            return data;
        }
    }
    private static com.google.gson.JsonObject readDocument(Path file) throws IOException {
        return com.google.gson.JsonParser.parseString(new String(readProfile(file), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private void backupLegacy(Path file) throws IOException {
        if (Files.exists(file)) { backupLegacyDocument(file, readDocument(file)); }
    }
    public List<String> getProfiles() {
        if (!Files.isDirectory(directory)) { return List.of(); }
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5)).filter(name -> name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}"))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        } catch (IOException failure) { logger.error("Could not list profiles", failure); return List.of(); }
    }
    public String getActiveProfile() { return config.getActiveProfile(); }
    public boolean saveActiveHudLayout() {
        String name = getActiveProfile();
        if (name == null || !Files.isRegularFile(getFile(name))) { return true; }
        try { backupLegacy(getFile(name)); JsonStorage.write(getFile(name), config.withCurrentHudState(readDocument(getFile(name)))); return true; }
        catch (IOException | RuntimeException failure) { logger.error("Could not save HUD layout to profile {}", name, failure); return false; }
    }
    public void refreshUi(){refreshUi.run();}
    @Override public void close(){io.shutdown();try{if(!io.awaitTermination(2,java.util.concurrent.TimeUnit.SECONDS)){io.shutdownNow();}}catch(InterruptedException interrupted){Thread.currentThread().interrupt();io.shutdownNow();}}
}
