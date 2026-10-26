package dev.riserecode.seedcracker.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/** Immutable overlay value; the Risecode overlay renderer owns drawing. */
public record Cuboid(AABB box, int argb) {
    public Cuboid(BoundingBox box, int argb) { this(AABB.of(box), argb); }
    public Cuboid(BlockPos pos, int argb) { this(new AABB(pos), argb); }
    public Cuboid(BlockPos pos, Vec3i size, int argb) { this(AABB.encapsulatingFullBlocks(pos, pos.offset(size)), argb); }
    public BlockPos getCenterPos() { return BlockPos.containing(box.getCenter()); }
}
