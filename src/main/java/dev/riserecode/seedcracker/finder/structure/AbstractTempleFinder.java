// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker.finder.structure;

import dev.riserecode.seedcracker.finder.Finder;
import dev.riserecode.seedcracker.render.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("this-escape")
public abstract class AbstractTempleFinder extends Finder {

    protected static List<BlockPos> SEARCH_POSITIONS;
    protected final Vec3i size;
    protected List<PieceFinder> finders = new ArrayList<>();

    public AbstractTempleFinder(Level world, ChunkPos chunkPos, Vec3i size) {
        super(world, chunkPos);

        Direction.Plane.HORIZONTAL.forEach(direction -> {
            PieceFinder finder = new PieceFinder(world, chunkPos, direction, size);

            finder.searchPositions = SEARCH_POSITIONS;

            buildStructure(finder);
            this.finders.add(finder);
        });

        this.size = size;
    }

    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> {
            if (pos.getX() != 0) return true;
            if (pos.getY() < 0) return true;
            if (pos.getY() > 200) return true;
            return pos.getZ() != 0;
        });
    }

    public List<BlockPos> findInChunkPiece(PieceFinder pieceFinder) {


        return pieceFinder.findInChunk();
    }

    protected abstract boolean isValidBiome(Biome biome);

    public void addRenderers(PieceFinder pieceFinder, BlockPos origin, int argb) {
        this.cuboids.add(new Cuboid(origin, pieceFinder.getLayout(), argb));
        BlockPos chunkStart = new BlockPos(origin.getX() & -16, origin.getY(), origin.getZ() & -16);
        this.cuboids.add(new Cuboid(chunkStart, argb));
    }

    public Map<PieceFinder, List<BlockPos>> findInChunkPieces() {
        Map<PieceFinder, List<BlockPos>> result = new HashMap<>();

        this.finders.forEach(pieceFinder -> {
            result.put(pieceFinder, this.findInChunkPiece(pieceFinder));
        });

        return result;
    }

    public abstract void buildStructure(PieceFinder finder);

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }
}
