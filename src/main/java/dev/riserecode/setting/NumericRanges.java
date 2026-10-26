package dev.riserecode.setting;

import java.util.Set;

/** Limits for extensible user controls; protocol values and normalized quantities stay bounded. */
public final class NumericRanges {
    private NumericRanges() { }
    private static final Set<String> BOUNDED = Set.of("Font Boldness", "Edge Gap", "Minimum Durability", "MinCPS", "MaxCPS", "Cave Altitude", "RotationX", "RotationY", "RotationZ", "Press Rebound", "Click Volume", "Click Pitch");
    public static double[] bounds(String name, double min, double max) {
        if (BOUNDED.contains(name) || name.startsWith("Destination ") || name.contains("Rotation ")
                || name.equals("Alpha") || name.equals("Red") || name.equals("Green") || name.equals("Blue")
                || min >= 0 && max <= 1 && !name.equals("Background Darkness") && !name.equals("Press Compression")) { return new double[]{min,max}; }
        double next = switch (name) {
            case "Width", "OutlineWidth" -> 32;
            case "GUI Blur Radius", "World Blur Radius" -> 128;
            case "Font Scale" -> 4;
            case "Interface Scale" -> 3;
            case "Particle Density" -> 1536;
            case "Precision" -> 6;
            case "Background Darkness" -> 1;
            case "Press Compression" -> .9;
            default -> name.contains("Duration") || name.equals("Press Timing") ? 60 : max * 4;
        };
        return new double[]{min < 0 ? min * 4 : min, Math.max(max,next)};
    }
}
