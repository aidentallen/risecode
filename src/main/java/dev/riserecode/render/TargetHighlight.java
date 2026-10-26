package dev.riserecode.render;

import dev.riserecode.event.Render3DEvent;
import dev.riserecode.setting.ColorSetting;
import dev.riserecode.ui.theme.ModuleAppearance;
import dev.riserecode.ui.theme.Theme;
import dev.riserecode.util.ColorUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Immutable requests shared by block and aimed-entity highlighting. */
public final class TargetHighlight {
    public enum Kind { BLOCK, ENTITY }
    public record Key(Kind kind, Object identity) { }
    public record Motion(boolean smooth, double duration, boolean pop, boolean popOnSwitch,
                         double popDuration, boolean reduced) {
        public double switchingDuration() { return smooth && !reduced ? duration : 0; }
    }
    public record Layer(ColorSetting.Paint paint, double weight,boolean spatial3d) {
        public Layer(ColorSetting.Paint paint,double weight){this(paint,weight,false);}
    }
    public record Style(List<Layer> edges, List<Layer> faces, double width, double gap, boolean through) {
        public Style { edges=List.copyOf(edges);faces=List.copyOf(faces); }
        public Style outlineOnly() { return new Style(edges,List.of(),width,gap,through); }
        public static Style of(ColorSetting outline, double opacity, double width, double gap, boolean through,
                               ColorSetting fill, double fillOpacity) {
            return new Style(layers(outline,opacity),layers(fill,fillOpacity),width,gap,through);
        }
        private static List<Layer> layers(ColorSetting color,double opacity) {
            return color==null ? List.of() : List.of(new Layer(ModuleAppearance.capture(color.paint()),opacity));
        }
        public static Style blend(Style from,Style to,double progress) {
            if(progress>=1)return to;if(progress<=0)return new Style(from.edges,from.faces,from.width,from.gap,to.through);
            return new Style(mix(from.edges,to.edges,progress),mix(from.faces,to.faces,progress),
                    from.width+(to.width-from.width)*progress,from.gap+(to.gap-from.gap)*progress,to.through);
        }
        private static List<Layer> mix(List<Layer> from,List<Layer> to,double t) {
            var layers=new ArrayList<Layer>();
            for(var layer:from)layers.add(new Layer(layer.paint,layer.weight*(1-t),layer.spatial3d));
            for(var layer:to)layers.add(new Layer(layer.paint,layer.weight*t,layer.spatial3d));
            // Rapid retargets cannot accumulate an unbounded chain of old palettes.
            return layers.stream().filter(l->l.weight>0.00001).sorted(Comparator.comparingDouble(Layer::weight).reversed()).limit(32).toList();
        }
    }
    public record Request(String owner, Key key, Object shapeKey, List<List<Vec3>> corners,
                          Motion motion, Style style, boolean suppressed,List<HighlightMesh> meshes) {
        public Request { corners=corners.stream().map(List::copyOf).toList();meshes=List.copyOf(meshes); }
        public Request(String owner,Key key,Object shapeKey,List<List<Vec3>> corners,Motion motion,Style style,boolean suppressed){this(owner,key,shapeKey,corners,motion,style,suppressed,corners.stream().map(HighlightMesh::box).toList());}
        public static Request mesh(String owner,Key key,Object shapeKey,HighlightMesh mesh,Motion motion,Style style,boolean suppressed){return new Request(owner,key,shapeKey,List.of(mesh.vertices()),motion,style,suppressed,List.of(mesh));}
    }
    private TargetHighlight() { }
    public static void draw(Render3DEvent event,List<Vec3> vertices,Style style,double alpha,boolean fullScale) {
        draw(event,HighlightMesh.box(vertices),style,alpha,fullScale);
    }
    public static void draw(Render3DEvent event,HighlightMesh mesh,Style style,double alpha,boolean fullScale){
        var vertices=mesh.vertices();double seconds=ColorUtil.time();
        if(style.faces.stream().anyMatch(layer->layer.weight>0.00001))for(var face:mesh.faces()){
            var a=vertices.get(face.a());var b=vertices.get(face.b());var c=vertices.get(face.c());var d=vertices.get(face.d());
            event.addQuad(a,b,c,d,color(style.faces,a,seconds,alpha),color(style.faces,b,seconds,alpha),color(style.faces,c,seconds,alpha),color(style.faces,d,seconds,alpha),fullScale);
        }
        if(style.edges.stream().anyMatch(layer->layer.weight>0.00001))for(var edge:mesh.edges())for(var segment:BlockHighlightMotion.edge(vertices.get(edge.a()),vertices.get(edge.b()),style.gap)){
            event.addLine(segment[0],segment[1],color(style.edges,segment[0],seconds,alpha),color(style.edges,segment[1],seconds,alpha),(float)style.width,!style.through);
        }
    }
    private static int color(List<Layer> layers,Vec3 point,double seconds,double opacity) {
        double alpha=0,red=0,green=0,blue=0;
        for(var layer:layers){int c=layer.paint.sample((point.x+point.z+(layer.spatial3d?point.y:0))*40,point.y*40,seconds);double a=(c>>>24)/255.0*layer.weight;
            alpha+=a;red+=(c>>16&255)*a;green+=(c>>8&255)*a;blue+=(c&255)*a;}
        if(alpha<=0)return 0;
        return Theme.opacity(ColorSetting.argb((int)Math.round(Math.clamp(alpha,0,1)*255),(int)Math.round(red/alpha),(int)Math.round(green/alpha),(int)Math.round(blue/alpha)),opacity);
    }
}
