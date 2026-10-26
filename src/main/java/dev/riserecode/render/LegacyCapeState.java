package dev.riserecode.render;

/** A per-extraction marker; never shared between players or carried into a vanilla pose. */
public interface LegacyCapeState {
    LegacyCapeMotion.Angles risecode$legacyCape();
    void risecode$legacyCape(LegacyCapeMotion.Angles angles);
}
