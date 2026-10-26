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
            if (!finite(from) || !finite(to) || !Float.isFinite(width) || width <= 0 || width > 32) {
                throw new IllegalArgumentException("Line coordinates and width must be finite; width must be in (0, 32]");
            }
        }
        private static boolean finite(Vec3 vector) { return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z); }
    }
    public record Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, int colorB, int colorC, int colorD, boolean visibleOnly) {
        public Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color) { this(a, b, c, d, color, color, color, color, true); }
        public Quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,int colorB,int colorC,int colorD){this(a,b,c,d,color,colorB,colorC,colorD,true);}
        public Quad {
            for (Vec3 point : List.of(a, b, c, d)) {
                if (!Line.finite(point)) { throw new IllegalArgumentException("Quad coordinates must be finite"); }
            }
        }
    }
    private dev.riserecode.render.TargetHighlight.Request targetHighlight;
    public void targetHighlight(dev.riserecode.render.TargetHighlight.Request request) {
        if(targetHighlight!=null)throw new IllegalStateException("Multiple aimed highlight providers in one extraction");
        targetHighlight=Objects.requireNonNull(request);
    }
    public dev.riserecode.render.TargetHighlight.Request getTargetHighlight() { return targetHighlight; }
    private final List<java.util.function.Consumer<net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext>> submissions = new ArrayList<>();
    public void submit(java.util.function.Consumer<net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext> geometry) { submissions.add(geometry); }
    public List<java.util.function.Consumer<net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext>> getSubmissions() { return List.copyOf(submissions); }
    private final dev.riserecode.render.PackedGeometry.Builder geometry=new dev.riserecode.render.PackedGeometry.Builder();
    private dev.riserecode.render.PackedGeometry snapshot;
    private void changed(){snapshot=null;}
    public dev.riserecode.render.PackedGeometry geometry(){if(snapshot==null){snapshot=geometry.snapshot();}return snapshot;}
    public void addLine(double ax,double ay,double az,double bx,double by,double bz,int a,int b,float width,boolean depth){geometry.line(ax,ay,az,bx,by,bz,a,b,width,depth);changed();}
    public void addLine(Vec3 from,Vec3 to,int a,int b,float width,boolean depth){addLine(from.x,from.y,from.z,to.x,to.y,to.z,a,b,width,depth);}
    public void addQuad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color){addQuad(a,b,c,d,color,color,color,color,true);}
    public void addQuad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int ca,int cb,int cc,int cd){addQuad(a,b,c,d,ca,cb,cc,cd,true);}
    public void addQuad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int ca,int cb,int cc,int cd,boolean depth){geometry.quad(a,b,c,d,ca,cb,cc,cd,depth);changed();}
    public List<Quad> getQuads(){return geometry().quads();}
    private final LevelExtractionContext context;
    public Render3DEvent(LevelExtractionContext context) { this.context = Objects.requireNonNull(context); }
    public LevelExtractionContext getContext() { return context; }
    public void addLine(Vec3 from, Vec3 to, int color, float width) { addLine(from,to,color,color,width,true); }
    public void addLine(Vec3 from, Vec3 to, int fromColor, int toColor, float width) { addLine(from,to,fromColor,toColor,width,true); }
    public List<Line> getLines() { return geometry().lines(); }
}
