package dev.riserecode.module;

import dev.riserecode.event.Event;

/** Optional presentation boundary. Headless module lifecycle has no theme or renderer dependency. */
public interface ModulePresentation {
    ModulePresentation NONE = new ModulePresentation() { };
    default double duration(Module module) { return 0; }
    default double scale(Module module, double progress) { return 1; }
    default void dispatch(Module module, Event event, Runnable listener) { listener.run(); }
}
