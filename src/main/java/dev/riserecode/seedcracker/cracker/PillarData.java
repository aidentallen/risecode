// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker.cracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PillarData {

    private final List<Integer> heights;

    public PillarData(List<Integer> heights) {
        this.heights = List.copyOf(heights);
    }

    public List<Integer> heights() { return heights; }

    public boolean test(long seed) {
        List<Integer> h = this.getPillarHeights((int) seed);
        return h.equals(this.heights);
    }

    public List<Integer> getPillarHeights(int pillarSeed) {
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            indices.add(i);
        }

        Collections.shuffle(indices, new Random(pillarSeed));

        List<Integer> heights = new ArrayList<>();

        for (Integer index : indices) {
            heights.add(76 + index * 3);
        }

        return heights;
    }

}
