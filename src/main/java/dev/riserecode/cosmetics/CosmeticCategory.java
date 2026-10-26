package dev.riserecode.cosmetics;

public enum CosmeticCategory {
    CAPE("Capes"), WINGS("Wings"), HEAD("Head"), TAIL("Tails"), COMPANION("Companions"),
    TRAIL("Trails"), AURA("Auras"), PARTICLES("Particles");
    public final String label;
    CosmeticCategory(String label) { this.label = label; }
}
