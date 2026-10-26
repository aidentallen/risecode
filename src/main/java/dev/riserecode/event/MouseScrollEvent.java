package dev.riserecode.event;
/** Gameplay scroll is offered before vanilla hotbar handling. */
public final class MouseScrollEvent extends Event {
    private final double horizontal, vertical;
    public MouseScrollEvent(double horizontal, double vertical) { super(true); this.horizontal=horizontal;this.vertical=vertical; }
    public double horizontal() { return horizontal; }
    public double vertical() { return vertical; }
}
