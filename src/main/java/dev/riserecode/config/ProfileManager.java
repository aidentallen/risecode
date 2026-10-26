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
    public java.util.concurrent.CompletionStage<List<String>> listAsync(){return java.util.concurrent.CompletableFuture.supplyAsync(this::getProfiles,io);}
    public java.util.concurrent.CompletionStage<Prepared> prepareAsync(String name){Path file=getFile(name);return java.util.concurrent.CompletableFuture.supplyAsync(()->{try{backupLegacy(file);if(Files.size(file)>1_048_576){throw new IllegalArgumentException("Profile too large");}byte[] bytes=Files.readAllBytes(file);return new Prepared(name,com.google.gson.JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().toString(),fingerprint(bytes));}catch(IOException failure){throw new IllegalArgumentException("Could not read profile");}},io);}
    public java.util.concurrent.CompletionStage<Boolean> savePrepared(String name,String document,String expected){Path file=getFile(name);return java.util.concurrent.CompletableFuture.supplyAsync(()->{try{String current=Files.exists(file)?fingerprint(Files.readAllBytes(file)):"";if(!current.equals(expected)){return false;}if(!current.isEmpty()){Files.copy(file,file.resolveSibling(name+".backup-"+java.util.UUID.randomUUID()+".json.bak"));}JsonStorage.write(file,com.google.gson.JsonParser.parseString(document).getAsJsonObject());return JsonStorage.read(file).equals(com.google.gson.JsonParser.parseString(document));}catch(IOException | RuntimeException failure){return false;}},io);}
    public java.util.concurrent.CompletionStage<String> fingerprintAsync(String name){Path file=getFile(name);return java.util.concurrent.CompletableFuture.supplyAsync(()->{try{return Files.exists(file)?fingerprint(Files.readAllBytes(file)):"";}catch(IOException failure){throw new IllegalArgumentException("Could not inspect profile");}},io);}
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
        try { config.applyProfile(JsonStorage.read(file)); }
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

    private void backupLegacy(Path file) throws IOException {
        if (Files.exists(file) && !JsonStorage.read(file).has("resources")) { Path backup = file.resolveSibling(file.getFileName()+".pre-resources"); if (!Files.exists(backup)) { Files.copy(file, backup); } }
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
        try { backupLegacy(getFile(name)); JsonStorage.write(getFile(name), config.withCurrentHudState(JsonStorage.read(getFile(name)))); return true; }
        catch (IOException | RuntimeException failure) { logger.error("Could not save HUD layout to profile {}", name, failure); return false; }
    }
    public void refreshUi(){refreshUi.run();}
    @Override public void close(){io.shutdown();try{if(!io.awaitTermination(2,java.util.concurrent.TimeUnit.SECONDS)){io.shutdownNow();}}catch(InterruptedException interrupted){Thread.currentThread().interrupt();io.shutdownNow();}}
}
