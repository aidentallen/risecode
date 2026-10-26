// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker.finder;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;

@FunctionalInterface
public interface FinderBuilder {

    List<Finder> build(Level world, ChunkPos chunkPos);

}
