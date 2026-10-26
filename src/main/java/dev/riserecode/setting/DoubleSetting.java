package dev.riserecode.setting;

/** A finite numeric setting with inclusive bounds. */
public final class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double normalMin, normalMax;

    public DoubleSetting(String name, String description, double defaultValue, double min, double max) {
        this(name,description,defaultValue,min,max,min,max);
    }

    private DoubleSetting(String name,String description,double value,double min,double max,double normalMin,double normalMax) {
        super(name, description, checked(value, min, max));
        this.min = min;
        this.max = max;
        this.normalMin=normalMin;this.normalMax=normalMax;
    }

    public static DoubleSetting extended(String name,String description,double value,double min,double max) {
        double[] bounds=NumericRanges.bounds(name,min,max);
        return new DoubleSetting(name,description,value,bounds[0],bounds[1],min,max);
    }
    public DoubleSetting copy() { return new DoubleSetting(getName(),getDescription(),getValue(),min,max,normalMin,normalMax); }
    public double keyboardIncrement() { return Math.min(1,(normalMax-normalMin)/100); }
    private double leftWeight() { return min<normalMin ? (max>normalMax?.125:.25):0; }
    private double rightWeight() { return max>normalMax ? (min<normalMin?.125:.25):0; }
    public double position(double value) {
        double left=leftWeight(),right=rightWeight();
        if(value<normalMin) { return left*(value-min)/(normalMin-min); }
        if(value>normalMax) { return 1-right+right*(value-normalMax)/(max-normalMax); }
        return normalMax==normalMin?left:left+(1-left-right)*(value-normalMin)/(normalMax-normalMin);
    }
    public double valueAt(double position) {
        double p=Math.clamp(position,0,1),left=leftWeight(),right=rightWeight();
        if(left>0 && p<left) { return min+(normalMin-min)*p/left; }
        if(right>0 && p>1-right) { return normalMax+(max-normalMax)*(p-1+right)/right; }
        return normalMin+(normalMax-normalMin)*(p-left)/(1-left-right);
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
