package dev.riserecode.seedcracker;

import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.structure.RegionStructure;

/** The only structure representation crossing the client/worker boundary. */
public record Observation(String type, int chunkX, int chunkZ) {
    public Observation {
        if (type == null || type.length() > 64 || Math.abs((long)chunkX) > 1_875_000 || Math.abs((long)chunkZ) > 1_875_000) {
            throw new IllegalArgumentException("Invalid structure observation");
        }
    }
    public Feature.Data<?> data() {
        return Features.STRUCTURE_TYPES.stream().filter(f -> f.getName().equals(type)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported structure: " + type)).at(chunkX, chunkZ);
    }
    public static Observation of(Feature.Data<?> data) {
        if (!(data.feature instanceof RegionStructure<?, ?>)) { throw new IllegalArgumentException("Unsupported finder data"); }
        return new Observation(data.feature.getName(), data.chunkX, data.chunkZ);
    }
}
