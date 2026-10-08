package dev.riserecode.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;

/** Profiles contain module/HUD choices; friends and command prefix remain global. */
public final class ProfileManager {
    private final Path directory;
    private final ConfigManager config;
    private final Runnable refreshUi;
    private final Logger logger;

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
        try { JsonStorage.write(file, config.profileSnapshot()); config.setActiveProfile(name); return true; }
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
        try { JsonStorage.write(getFile(name), config.withCurrentHudState(JsonStorage.read(getFile(name)))); return true; }
        catch (IOException | RuntimeException failure) { logger.error("Could not save HUD layout to profile {}", name, failure); return false; }
    }
}
