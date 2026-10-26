// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker.util;

public class HeightContext {
    private final int bottomY;
    private final int topY;

    public HeightContext(int minY, int maxY) {
        this.bottomY = minY;
        this.topY = maxY;
    }

    public int getTopY() {
        return topY;
    }

    public int getBottomY() {
        return bottomY;
    }

    public int getHeight() {
        return topY - bottomY;
    }

    public int getDistanceToBottom(int yValue) {
        return yValue - bottomY;
    }

    public int getDistanceToTop(int yValue) {
        return topY - yValue - 1;
    }
}
