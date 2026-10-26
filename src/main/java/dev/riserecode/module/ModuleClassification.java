package dev.riserecode.module;

/** Describes behavior, not server policy or a detection guarantee. */
public enum ModuleClassification {
    CLIENT("Client", "Local rendering or information display"),
    GHOST("Ghost", "Subtle automation; server compatibility depends on settings"),
    BLATANT("Blatant", "Changes gameplay or protocol behavior visibly");
    private final String label, description;
    ModuleClassification(String label,String description) { this.label=label;this.description=description; }
    public String label() { return label; }
    public String description() { return description; }
}
