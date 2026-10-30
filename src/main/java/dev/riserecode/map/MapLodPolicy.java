package dev.riserecode.map;

// Density helpers adapted from Simple Map 564eb2e, Copyright (c) 2026 Velorise (MIT).

/**
 * Shared texel-density policy for fullscreen and minimap rendering.
 *
 * <p>Level 0 is the exact 64x64 leaf texture (one block per texel). Branch
 * level {@code n} covers {@code 64 << n} world blocks with a 64x64 texture,
 * therefore one branch texel represents {@code 1 << n} blocks. The selected
 * level never becomes coarser than the current screen footprint.</p>
 */
public final class MapLodPolicy {
    public static final int LEAF_WORLD_SIZE = 64;
    public static final int MAX_BRANCH_LEVEL = 7;
    /** Most tiles one frame may ask for; the map caches must stay above this or tiles get evicted while on screen. */
    public static final int VIEWPORT_BUDGET = 900;

    private MapLodPolicy() {
    }

    public static int branchLevel(float pixelsPerBlock) {
        return branchLevel(pixelsPerBlock, MAX_BRANCH_LEVEL);
    }

    public static int branchLevel(float pixelsPerBlock, int maximumLevel) {
        double footprint = blocksPerScreenPixel(pixelsPerBlock);
        if (footprint < 2.0) return 0;
        int level = (int) Math.floor(Math.log(footprint) / Math.log(2.0));
        return Math.max(0, Math.min(maximumLevel, level));
    }


    /**
     * Adds a small density hysteresis around adjacent LOD boundaries.
     *
     * <p>Without this guard a tiny wheel/viewport rounding change can alternate
     * between two branch levels every frame, making the renderer swap between
     * independently resident snapshots. Budget-forced coarser levels are never
     * delayed, because protecting frame time is more important than hysteresis.</p>
     */
    public static int stabilizeBranchLevel(int candidateLevel, int previousLevel,
            float pixelsPerBlock) {
        if (previousLevel < 0 || candidateLevel == previousLevel) return candidateLevel;
        if (Math.abs(candidateLevel - previousLevel) > 1) return candidateLevel;

        int densityLevel = branchLevel(pixelsPerBlock);
        // The viewport-node budget may require a coarser level than density alone.
        if (candidateLevel > previousLevel && candidateLevel > densityLevel) {
            return candidateLevel;
        }
        // When a previous budget-forced level is no longer necessary, return to the
        // density-correct level immediately rather than retaining excessive blur.
        if (candidateLevel < previousLevel && previousLevel > densityLevel) {
            return candidateLevel;
        }

        double footprint = blocksPerScreenPixel(pixelsPerBlock);
        if (candidateLevel > previousLevel) {
            double enterCoarser = (1 << candidateLevel) * 1.12;
            return footprint >= enterCoarser ? candidateLevel : previousLevel;
        }
        double leaveCoarser = (1 << previousLevel) * 0.88;
        return footprint <= leaveCoarser ? candidateLevel : previousLevel;
    }

    /** Adapted viewport-budget selection: powers of four match risecode's persisted overviews. */
    public static int viewportLevel(float logicalPixelsPerBlock, double width, double height) {
        int level = Math.min(8, branchLevel(logicalPixelsPerBlock) / 2 * 2);
        while (level < 8) {
            double tilePixels = (16 << level) * Math.max(0.0001, logicalPixelsPerBlock);
            long columns = (long) Math.ceil(width / tilePixels) + 2;
            long rows = (long) Math.ceil(height / tilePixels) + 2;
            if (columns * rows <= VIEWPORT_BUDGET) { break; }
            level += 2;
        }
        return level;
    }
    public static int leafMipLevel(float pixelsPerBlock, int maximumMip) {
        double footprint = blocksPerScreenPixel(pixelsPerBlock);
        int level = footprint < 2.0 ? 0
                : (int) Math.floor(Math.log(footprint) / Math.log(2.0));
        return Math.max(0, Math.min(maximumMip, level));
    }

    public static double blocksPerScreenPixel(float pixelsPerBlock) {
        return 1.0 / Math.max(0.0001, pixelsPerBlock);
    }

    public static int worldSizeForBranch(int level) {
        if (level <= 0) return LEAF_WORLD_SIZE;
        return LEAF_WORLD_SIZE << level;
    }

    public static int pageSpanForBranch(int level) {
        return level <= 0 ? 1 : 1 << level;
    }
}
