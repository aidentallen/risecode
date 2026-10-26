package dev.riserecode.seedcracker.cracker.storage;

import com.seedfinding.mcfeature.Feature;
import dev.riserecode.seedcracker.Observation;
import dev.riserecode.seedcracker.cracker.DataAddedEvent;
import dev.riserecode.seedcracker.cracker.PillarData;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Client-thread collection only. No executors, callbacks or automatic jobs. */
public final class DataStorage {
    public static final int MAX_OBSERVATIONS = 4096;
    private final Set<Observation> structures = new LinkedHashSet<>();
    private PillarData pillars;
    private long revision;
    public boolean addBaseData(Feature.Data<?> data, DataAddedEvent ignored) {
        if (structures.size() >= MAX_OBSERVATIONS) { return false; }
        boolean added = structures.add(Observation.of(data));
        if (added) { revision++; }
        return added;
    }
    /** Existing observations still need outlines after chunk reload/explicit Restore. */
    public boolean observeForOutline(Feature.Data<?> data, DataAddedEvent event) {
        Observation observation=Observation.of(data);
        if (structures.contains(observation)) { return true; }
        return addBaseData(data,event);
    }
    public boolean addPillarData(PillarData data, DataAddedEvent ignored) {
        if (pillars != null) { return false; }
        if (data != null) { pillars = data; revision++; }
        return true;
    }
    public List<Observation> observations() { return List.copyOf(structures); }
    public List<Integer> pillars() { return pillars == null ? List.of() : pillars.heights(); }
    public long revision() { return revision; }
    public void replace(List<Observation> observations, List<Integer> heights) {
        if (observations.size() > MAX_OBSERVATIONS) { throw new IllegalArgumentException("Too many saved observations"); }
        observations.forEach(Observation::data);
        if (!heights.isEmpty() && (heights.size() != 10 || heights.stream().distinct().count() != 10 || heights.stream().anyMatch(h -> h < 76 || h > 103 || (h-76)%3 != 0))) {
            throw new IllegalArgumentException("Invalid saved End pillars");
        }
        structures.clear(); structures.addAll(observations);
        pillars = heights.isEmpty() ? null : new PillarData(heights); revision++;
    }
    public void clear() { structures.clear(); pillars = null; revision++; }
}
