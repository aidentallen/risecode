// Derived from SeedCrackerX 2.16.1 (MIT), commit b72a829f768967fa737a4f1f63b32e37b9619152.
// Copyright (c) 2020 KaptainWutax; SeedCrackerX contributors. See licenses/seedcracker.
package dev.riserecode.seedcracker.util;

public class FeatureToggle {

    //This is for The featureToggles in the config object
    //It allows for the booleans to be passed around by reference
    //(I know that it's a hacky workaround)
    private boolean enabled;

    public FeatureToggle(boolean flag) {
        enabled = flag;
    }

    public void set(boolean flag) {
        enabled = flag;
    }

    public boolean get() {
        return enabled;
    }
}
