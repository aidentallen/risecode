// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker.finder.structure;

import com.seedfinding.mcfeature.structure.RegionStructure;
import dev.riserecode.seedcracker.Features;
import dev.riserecode.seedcracker.SeedCracker;
import dev.riserecode.seedcracker.cracker.DataAddedEvent;
import dev.riserecode.seedcracker.finder.BlockFinder;
import dev.riserecode.seedcracker.finder.Finder;
import dev.riserecode.seedcracker.render.Cuboid;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;

public final class BuriedTreasureFinder extends BlockFinder {

    protected static final List<BlockState> CHEST_HOLDERS = new ArrayList<>();
    protected static List<BlockPos> SEARCH_POSITIONS;

    static {
        CHEST_HOLDERS.add(Blocks.SANDSTONE.defaultBlockState());
        CHEST_HOLDERS.add(Blocks.STONE.defaultBlockState());
        CHEST_HOLDERS.add(Blocks.ANDESITE.defaultBlockState());
        CHEST_HOLDERS.add(Blocks.GRANITE.defaultBlockState());
        CHEST_HOLDERS.add(Blocks.DIORITE.defaultBlockState());

        //Population can turn stone, andesite, granite and diorite into ores...
        CHEST_HOLDERS.add(Blocks.COAL_ORE.defaultBlockState());
        CHEST_HOLDERS.add(Blocks.IRON_ORE.defaultBlockState());
        CHEST_HOLDERS.add(Blocks.GOLD_ORE.defaultBlockState());

        //Ocean can turn stone into gravel.
        CHEST_HOLDERS.add(Blocks.GRAVEL.defaultBlockState());
    }

    public BuriedTreasureFinder(Level world, ChunkPos chunkPos) {
        super(world, chunkPos, Blocks.CHEST);
        this.searchPositions = SEARCH_POSITIONS;
    }

    public static void reloadSearchPositions() {
        SEARCH_POSITIONS = buildSearchPositions(CHUNK_POSITIONS, pos -> {
            //Buried treasure chests always generate at (9, 9) within a chunk.
            int localX = pos.getX() & 15;
            int localZ = pos.getZ() & 15;
            if (localX != 9 || localZ != 9) return true;
            return pos.getY() > 90 && pos.getY() < 0;
        });
    }

    public static List<Finder> create(Level world, ChunkPos chunkPos) {
        List<Finder> finders = new ArrayList<>();
        finders.add(new BuriedTreasureFinder(world, chunkPos));
        return finders;
    }

    @Override
    public List<BlockPos> findInChunk() {


        List<BlockPos> result = super.findInChunk();

        result.removeIf(pos -> {
            BlockState chest = world.getBlockState(pos);
            if (chest.getValue(ChestBlock.WATERLOGGED)) return true;

            BlockState chestHolder = world.getBlockState(pos.below());
            return !CHEST_HOLDERS.contains(chestHolder);
        });

        result.forEach(pos -> {
            RegionStructure.Data<?> data = Features.BURIED_TREASURE.at(this.chunkPos.x(), this.chunkPos.z());

            if (SeedCracker.get().getDataStorage().observeForOutline(data, DataAddedEvent.POKE_STRUCTURES)) {
                this.cuboids.add(new Cuboid(pos, ARGB.color(255, 255, 0)));
            }
        });

        return result;
    }

    @Override
    public boolean isValidDimension(DimensionType dimension) {
        return this.isOverworld(dimension);
    }

}
