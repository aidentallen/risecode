package dev.riserecode.setting;

/** A GLFW key code; -1 means unbound. Used where a module owns hotkeys that live outside the module bind. */
public final class KeySetting extends Setting<Integer> {
    public KeySetting(String name, String description, int defaultKey) { super(name, description, defaultKey); }
    @Override protected Integer validate(Integer value) {
        if (value < -1 || value > 400) { throw new IllegalArgumentException("Invalid key code"); }
        return value;
    }
    public boolean isUnbound() { return getValue() < 0; }
}
