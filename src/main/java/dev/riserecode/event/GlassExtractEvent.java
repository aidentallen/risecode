package dev.riserecode.event;

import dev.riserecode.render.glass.GlassPolicy;

/** Extraction hook. Only enabled modules provide a gameplay policy; previews are explicit. */
public final class GlassExtractEvent extends Event {
    private GlassPolicy policy;
    public GlassPolicy policy(){return policy;}
    public void policy(GlassPolicy value){policy=value;}
}
