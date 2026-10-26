// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker.cracker;

import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mcseed.rand.JRand;

public class HashedSeedData {

    private final long hashedSeed;

    public HashedSeedData(long hashedSeed) {
        this.hashedSeed = hashedSeed;
    }

    public boolean test(long seed, JRand rand) {
        return WorldSeed.toHash(seed) == this.hashedSeed;
    }

    public long getHashedSeed() {
        return this.hashedSeed;
    }

}
