package dev.riserecode.event;

import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.material.FogType;

/** Native fog after environment and render-distance setup, before GPU upload. */
public final class FogEvent extends Event {
    public enum Type { OVERWORLD, NETHER, END, OTHER_DIMENSIONS, WATER, LAVA, POWDER_SNOW, BLINDNESS, DARKNESS }
    private final Type type;
    private final FogData fog;

    public FogEvent(Type type, FogData fog) { this.type = type; this.fog = fog; }
    public Type type() { return type; }
    public void disableFog() {
        // Match Minecraft's empty fog buffer. Keep its clear/sky color unchanged.
        fog.environmentalStart = Float.MAX_VALUE;
        fog.environmentalEnd = Float.MAX_VALUE;
        fog.renderDistanceStart = Float.MAX_VALUE;
        fog.renderDistanceEnd = Float.MAX_VALUE;
        fog.skyEnd = Float.MAX_VALUE;
        fog.cloudEnd = Float.MAX_VALUE;
    }

    /** Match vanilla's environment precedence; immersion/effects precede atmosphere. */
    public static Type classify(FogType fluid, String dimension, boolean blindness, boolean darkness) {
        if (fluid == FogType.LAVA) { return Type.LAVA; }
        if (fluid == FogType.POWDER_SNOW) { return Type.POWDER_SNOW; }
        if (blindness) { return Type.BLINDNESS; }
        if (darkness) { return Type.DARKNESS; }
        if (fluid == FogType.WATER) { return Type.WATER; }
        return switch (dimension) {
            case "minecraft:overworld" -> Type.OVERWORLD;
            case "minecraft:the_nether" -> Type.NETHER;
            case "minecraft:the_end" -> Type.END;
            default -> Type.OTHER_DIMENSIONS;
        };
    }
}
