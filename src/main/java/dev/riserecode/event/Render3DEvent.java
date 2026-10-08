package dev.riserecode.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.world.phys.Vec3;

/** Extraction collects immutable primitives; no entity references survive into drawing. */
public final class Render3DEvent extends Event {
    public record Line(Vec3 from, Vec3 to, int color, float width, int endColor, boolean visibleOnly) {
        public Line(Vec3 from, Vec3 to, int color, float width) { this(from, to, color, width, color, true); }
        public Line(Vec3 from, Vec3 to, int color, float width, int endColor) { this(from, to, color, width, endColor, true); }
        public Line {
            Objects.requireNonNull(from); Objects.requireNonNull(to);
            if (!finite(from) || !finite(to) || !Float.isFinite(width) || width <= 0 || width > 16) {
                throw new IllegalArgumentException("Line coordinates and width must be finite; width must be in (0, 16]");
            }
        }
        private static boolean finite(Vec3 vector) { return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z); }
    }
    public record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int colorB, int colorC, int colorD) {
        public Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) { this(a, b, c, d, color, color, color, color); }
        public Quad {
            for (Vec3 point : List.of(a, b, c, d)) {
                if (!Line.finite(point)) { throw new IllegalArgumentException("Quad coordinates must be finite"); }
            }
        }
    }
    private final List<java.util.function.Consumer<net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext>> submissions = new ArrayList<>();
    public void submit(java.util.function.Consumer<net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext> geometry) { submissions.add(geometry); }
    public List<java.util.function.Consumer<net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext>> getSubmissions() { return List.copyOf(submissions); }
    public void addLine(Vec3 from, Vec3 to, int fromColor, int toColor, float width, boolean visibleOnly) { lines.add(new Line(from, to, fromColor, width, toColor, visibleOnly)); }
    private final List<Quad> quads = new ArrayList<>();
    public void addQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) { quads.add(new Quad(a, b, c, d, color)); }
    public void addQuad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int colorA, int colorB, int colorC, int colorD) { quads.add(new Quad(a, b, c, d, colorA, colorB, colorC, colorD)); }
    public List<Quad> getQuads() { return List.copyOf(quads); }
    private final LevelExtractionContext context;
    private final List<Line> lines = new ArrayList<>();
    public Render3DEvent(LevelExtractionContext context) { this.context = Objects.requireNonNull(context); }
    public LevelExtractionContext getContext() { return context; }
    public void addLine(Vec3 from, Vec3 to, int color, float width) { lines.add(new Line(from, to, color, width)); }
    public void addLine(Vec3 from, Vec3 to, int fromColor, int toColor, float width) { lines.add(new Line(from, to, fromColor, width, toColor)); }
    public List<Line> getLines() { return List.copyOf(lines); }
}
