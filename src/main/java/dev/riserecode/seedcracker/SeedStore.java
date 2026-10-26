package dev.riserecode.seedcracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import dev.riserecode.config.JsonStorage;
import dev.riserecode.seedcracker.cracker.storage.DataStorage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/** Versioned, validated, world-scoped archives. Reading never starts a job. */
public final class SeedStore {
    public record Identity(String world, long hash, String generation) {
        public Identity { if (world == null || world.isBlank() || generation == null || generation.isBlank()) { throw new IllegalArgumentException("Missing world identity"); } }
    }
    public record Saved(int format, Identity identity, List<Observation> observations, List<Integer> pillars,
            List<Long> structureSeeds, List<Long> verifiedSeeds) {
        public Saved {
            observations = List.copyOf(observations); pillars = List.copyOf(pillars);
            structureSeeds = List.copyOf(structureSeeds); verifiedSeeds = List.copyOf(verifiedSeeds);
        }
    }
    private static final Gson GSON = new Gson();
    private final Path root;
    public SeedStore(Path root) { this.root = root; }
    public Path path(Identity id) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(GSON.toJson(id).getBytes(StandardCharsets.UTF_8));
            return root.resolve(HexFormat.of().formatHex(digest)+".json");
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public void save(Saved saved) throws IOException { validate(saved, saved.identity()); JsonStorage.write(path(saved.identity()), GSON.toJsonTree(saved).getAsJsonObject()); }
    public Saved restore(Identity identity) throws IOException {
        Path file=path(identity);
        if (!Files.exists(file)) { throw new IOException("No saved data matches this world, hash and generation profile"); }
        if (Files.size(file)>16*1024*1024) { throw new IOException("Saved data exceeds the 16 MiB limit; original file preserved"); }
        try {
            JsonObject document=JsonStorage.read(file);
            Saved saved=GSON.fromJson(document, Saved.class); validate(saved, identity); return saved;
        } catch (RuntimeException failure) { throw new IOException("Invalid saved data; original file preserved: "+failure.getMessage(), failure); }
    }
    private static void validate(Saved saved, Identity identity) {
        if (saved.format()!=1 || !identity.equals(saved.identity())) { throw new IllegalArgumentException("Saved world identity does not match"); }
        new DataStorage().replace(saved.observations(), saved.pillars());
        if (saved.structureSeeds().size()>CrackingJob.MAX_CANDIDATES || saved.verifiedSeeds().size()>1024 ||
                saved.structureSeeds().stream().anyMatch(s -> s<0 || s>=(1L<<48)) ||
                saved.verifiedSeeds().stream().anyMatch(s -> identity.hash()==0 || WorldSeed.toHash(s)!=identity.hash())) {
            throw new IllegalArgumentException("Invalid saved seed results");
        }
    }
}
