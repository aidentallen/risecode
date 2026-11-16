package dev.riserecode.performance;

/** Immutable reductions, published on the client thread and captured once per extraction. */
public record PerformancePolicy(boolean blur, boolean glow, boolean softShadows, boolean menuParticles,
        boolean reducedDetail, Density particleDensity, boolean suppressTrails, boolean backgroundWork,
        boolean mapCpu, boolean mapGpu, boolean previews, boolean cosmeticCache) {
    public enum Density { FULL, HALF, OFF;
        public double multiplier(){return switch(this){case FULL->1;case HALF->.5;case OFF->0;};}
    }
    public static final PerformancePolicy OFF=new PerformancePolicy(false,false,false,false,false,Density.FULL,false,false,false,false,false,false);
    private static volatile PerformancePolicy published=OFF;
    private static PerformancePolicy extraction=OFF;
    private static long tick;
    public static PerformancePolicy current(){return published;}
    public static PerformancePolicy frame(){return extraction;}
    public static void publish(PerformancePolicy policy){published=java.util.Objects.requireNonNull(policy);}
    public static void beginExtraction(){extraction=published;}
    public static void tick(){tick++;}
    public static long ticks(){return tick;}
    public int mapColumns(){return backgroundWork?64:128;}
    public long mapCaptureNanos(){return backgroundWork?500_000:1_500_000;}
    public int mapCpuEntries(){return mapCpu?2048:4096;}
    public long mapCpuBytes(){return (mapCpu?64L:128L)*1024*1024;}
    public int mapGpuEntries(){return mapGpu?1024:1536;}
    public int previewEntries(){return previews?24:48;}
    public int previewPending(){return previews?8:16;}
    public int unusedCosmeticEntries(){return cosmeticCache?8:16;}
    public long unusedCosmeticBytes(){return (cosmeticCache?32L:64L)*1024*1024;}
}
