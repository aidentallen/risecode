package dev.riserecode.cosmetics;

/** Only immutable outfit snapshots cross from extraction into rendering. */
public interface CosmeticRenderState {
    CosmeticLoadout risecode$outfit();
    void risecode$outfit(CosmeticLoadout value);
    boolean risecode$preview();
    void risecode$preview(boolean value);
}
