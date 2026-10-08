package dev.riserecode.setting;

/** Bounded single-line Unicode text, shared by module and appearance persistence. */
public final class StringSetting extends Setting<String> {
    private final int maximum;
    public StringSetting(String name, String description, String value) { this(name, description, value, 512); }
    public StringSetting(String name, String description, String value, int maximum) {
        super(name, description, value);
        if (maximum < 1 || value.length() > maximum) { throw new IllegalArgumentException("Invalid text length"); }
        this.maximum = maximum;
    }
    @Override protected String validate(String value) {
        if (value.length() > maximum || value.codePoints().anyMatch(Character::isISOControl)) { throw new IllegalArgumentException("Use up to " + maximum + " single-line characters"); }
        return value;
    }
    public int maximum() { return maximum; }
}
