package dev.riserecode.setting;

/** A finite numeric setting with inclusive bounds. */
public final class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;

    public DoubleSetting(String name, String description, double defaultValue, double min, double max) {
        super(name, description, checked(defaultValue, min, max));
        this.min = min;
        this.max = max;
    }

    @Override
    protected Double validate(Double value) {
        return checked(value, min, max);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    private static double checked(double value, double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Numeric bounds must be finite and ordered");
        }
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Value must be finite and within [" + min + ", " + max + "]");
        }
        return value;
    }
}
