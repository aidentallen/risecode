// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker.finder.structure;

import com.seedfinding.mcfeature.structure.RegionStructure;
import dev.riserecode.seedcracker.Features;
import dev.riserecode.seedcracker.SeedCracker;
import dev.riserecode.seedcracker.cracker.DataAddedEvent;
import dev.riserecode.seedcracker.finder.Finder;
import dev.riserecode.seedcracker.render.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OutpostFinder extends Finder {

    protected static Map<Direction, List<BlockPos>> SEARCH_POSITIONS;
    protected static final Vec3i size = new Vec3i(15, 21, 15);
    protected List<JigsawFinder> finders = new ArrayList<>();

    public OutpostFinder(Level world, ChunkPos chunkPos) {
        super(world, chunkPos);

        Direction.Plane.HORIZONTAL.forEach(direction -> {
            JigsawFinder finder = new JigsawFinder(world, chunkPos, direction, size);

            finder.searchPositions = SEARCH_POSITIONS.get(direction);

            buildStructure(finder);
            this.finders.add(finder);
        });
    }

    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = JigsawFinder.getSearchPositions(0, 0,0,0, size);
        Map<Direction, List<BlockPos>> additional = JigsawFinder.getSearchPositions(0, 0,1,0, size);
        for(Direction direction : Direction.Plane.HORIZONTAL) {
            SEARCH_POSITIONS.get(direction).addAll(additional.get(direction));
        }

    }

    public static List<Finder> create(Level world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new OutpostFinder(world, chunkPos));
        finders.add(new OutpostFinder(world, new ChunkPos(chunkPos.x(), chunkPos.z() + 1)));
        finders.add(new OutpostFinder(world, new ChunkPos(chunkPos.x() + 1, chunkPos.z())));
        finders.add(new OutpostFinder(world, new ChunkPos(chunkPos.x() + 1, chunkPos.z() + 1)));
        return finders;
    }

    private void buildStructure(JigsawFinder finder) {
        BlockState chest = Blocks.CHEST.defaultBlockState();
        BlockState birchwood = Blocks.BIRCH_PLANKS.defaultBlockState();

        finder.addBlock(chest, 9, 14, 10);

        finder.fillWithOutline(4, 0, 4, 10, 0, 10, birchwood, birchwood, false);
    }

    @Override
    public List<BlockPos> findInChunk() {

        Map<JigsawFinder, List<BlockPos>> result = this.findInChunkPieces();
        List<BlockPos> combinedResult = new ArrayList<>();

        result.forEach((pieceFinder, positions) -> {

            combinedResult.addAll(positions);

            positions.forEach(pos -> {
                RegionStructure.Data<?> data = Features.PILLAGER_OUTPOST.at(this.chunkPos.x(), this.chunkPos.z());

                if (SeedCracker.get().getDataStorage().observeForOutline(data, DataAddedEvent.POKE_LIFTING)) {
                    this.cuboids.add(new Cuboid(pos, pieceFinder.getLayout(), ARGB.color(170, 84, 3)));
                    this.cuboids.add(new Cuboid(chunkPos.getWorldPosition().offset(0, pos.getY(), 0), ARGB.color(170, 84, 3)));
                }
            });
        });

        return combinedResult;
    }

    public Map<JigsawFinder, List<BlockPos>> findInChunkPieces() {
        Map<JigsawFinder, List<BlockPos>> result = new HashMap<>();

        this.finders.forEach(pieceFinder -> {
            result.put(pieceFinder, pieceFinder.findInChunk());
        });

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }

}
