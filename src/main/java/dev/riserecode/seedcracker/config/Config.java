package dev.riserecode.seedcracker.config;

import com.seedfinding.mccore.version.MCVersion;
import dev.riserecode.seedcracker.util.FeatureToggle;

/** Adapter only; all persisted choices belong to the Risecode module. */
public final class Config {
    private static final Config INSTANCE = new Config();
    public final FeatureToggle buriedTreasure = new FeatureToggle(true), desertTemple = new FeatureToggle(true),
            endCity = new FeatureToggle(true), jungleTemple = new FeatureToggle(true), monument = new FeatureToggle(true),
            swampHut = new FeatureToggle(true), shipwreck = new FeatureToggle(true), outpost = new FeatureToggle(true),
            igloo = new FeatureToggle(true), trialChambers = new FeatureToggle(true), endPillars = new FeatureToggle(true);
    public boolean debug;
    private Config() { }
    public static Config get() { return INSTANCE; }
    // The upstream library calls its current generation rules v1_21_3. Validate
    // their salts, spacing and placement against native 26.2 in SeedCrackerIntegrationTest.
    public MCVersion getVersion() { return MCVersion.v1_21_3; }
}
