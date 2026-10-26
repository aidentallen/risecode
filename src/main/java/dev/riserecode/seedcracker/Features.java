// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker;

import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcfeature.Feature;
import com.seedfinding.mcfeature.decorator.DesertWell;
import com.seedfinding.mcfeature.decorator.EndGateway;
import com.seedfinding.mcfeature.structure.BuriedTreasure;
import com.seedfinding.mcfeature.structure.DesertPyramid;
import com.seedfinding.mcfeature.structure.EndCity;
import com.seedfinding.mcfeature.structure.Igloo;
import com.seedfinding.mcfeature.structure.JunglePyramid;
import com.seedfinding.mcfeature.structure.Monument;
import com.seedfinding.mcfeature.structure.PillagerOutpost;
import com.seedfinding.mcfeature.structure.RegionStructure;
import com.seedfinding.mcfeature.structure.Shipwreck;
import com.seedfinding.mcfeature.structure.SwampHut;
import dev.riserecode.seedcracker.finder.Finder;
import dev.riserecode.seedcracker.structures.TrialChambers;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Features {
    public static final ArrayList<RegionStructure<?, ?>> STRUCTURE_TYPES = new ArrayList<>();

    public static BuriedTreasure BURIED_TREASURE;
    public static DesertPyramid DESERT_PYRAMID;
    public static EndCity END_CITY;
    public static JunglePyramid JUNGLE_PYRAMID;
    public static Monument MONUMENT;
    public static Shipwreck SHIPWRECK;
    public static SwampHut SWAMP_HUT;
    public static PillagerOutpost PILLAGER_OUTPOST;
    public static Igloo IGLOO;
    public static TrialChambers TRIAL_CHAMBERS;

    public static void init(MCVersion version) {
        STRUCTURE_TYPES.clear();

        BURIED_TREASURE = safe(STRUCTURE_TYPES, Finder.Type.BURIED_TREASURE, () -> new BuriedTreasure(version));
        DESERT_PYRAMID = safe(STRUCTURE_TYPES, Finder.Type.DESERT_TEMPLE, () -> new DesertPyramid(version));
        END_CITY = safe(STRUCTURE_TYPES, Finder.Type.END_CITY, () -> new EndCity(version));
        JUNGLE_PYRAMID = safe(STRUCTURE_TYPES, Finder.Type.JUNGLE_TEMPLE, () -> new JunglePyramid(version));
        MONUMENT = safe(STRUCTURE_TYPES, Finder.Type.MONUMENT, () -> new Monument(version));
        SHIPWRECK = safe(STRUCTURE_TYPES, Finder.Type.SHIPWRECK, () -> new Shipwreck(version));
        SWAMP_HUT = safe(STRUCTURE_TYPES, Finder.Type.SWAMP_HUT, () -> new SwampHut(version));
        PILLAGER_OUTPOST = safe(STRUCTURE_TYPES, Finder.Type.PILLAGER_OUTPOST, () -> new PillagerOutpost(version));
        IGLOO = safe(STRUCTURE_TYPES, Finder.Type.IGLOO, () -> new Igloo(version));
        TRIAL_CHAMBERS = safe(STRUCTURE_TYPES, Finder.Type.TRIAL_CHAMBERS, () -> new TrialChambers(version));

        STRUCTURE_TYPES.trimToSize();
    }

    private static <F extends Feature<?, ?>> F safe(Finder.Type finderType, Supplier<F> lambda) {
        try {
            return lambda.get();
        } catch (Throwable t) {
            SeedCracker.LOGGER.error("Exception thrown loading feature", t);
            finderType.enabled.set(false);
            return null;
        }
    }

    private static <F extends RegionStructure<?, ?>> F safe(List<RegionStructure<?, ?>> list, Finder.Type finderType, Supplier<F> lambda) {
        F initializedFeature = safe(finderType, lambda);
        if (initializedFeature != null) list.add(initializedFeature);
        return initializedFeature;
    }

}
