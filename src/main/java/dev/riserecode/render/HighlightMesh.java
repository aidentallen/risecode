package dev.riserecode.render;

import java.util.*;
import java.util.function.UnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Selection union topology. Edges and exposed faces share vertices, never component-box seams. */
public record HighlightMesh(List<Vec3> vertices,List<Edge> edges,List<Face> faces) {
    public record Edge(int a,int b) { }
    public record Face(int a,int b,int c,int d) { }
    public HighlightMesh {vertices=List.copyOf(vertices);edges=List.copyOf(edges);faces=List.copyOf(faces);}
    public HighlightMesh map(UnaryOperator<Vec3> transform){return new HighlightMesh(vertices.stream().map(transform).toList(),edges,faces);}
    public HighlightMesh withVertices(List<Vec3> points){return new HighlightMesh(points,edges,faces);}
    public boolean sameTopology(HighlightMesh other){return vertices.size()==other.vertices.size()&&edges.equals(other.edges)&&faces.equals(other.faces);}
    public static HighlightMesh box(List<Vec3> v){return new HighlightMesh(v,List.of(new Edge(0,1),new Edge(1,2),new Edge(2,3),new Edge(3,0),new Edge(4,5),new Edge(5,6),new Edge(6,7),new Edge(7,4),new Edge(0,4),new Edge(1,5),new Edge(2,6),new Edge(3,7)),List.of(new Face(0,3,2,1),new Face(4,5,6,7),new Face(0,1,5,4),new Face(3,7,6,2),new Face(0,4,7,3),new Face(1,2,6,5)));}
    private static final Comparator<Vec3> ORDER=Comparator.comparingDouble((Vec3 v)->v.x).thenComparingDouble(v->v.y).thenComparingDouble(v->v.z);
    private static final Map<VoxelShape,HighlightMesh> CACHE=new IdentityHashMap<>();
    public static void clearCache(){CACHE.clear();}
    public static HighlightMesh selection(VoxelShape shape){
        var cached=CACHE.get(shape);if(cached!=null)return cached;
        var mesh=build(shape);if(CACHE.size()>=512)CACHE.clear();CACHE.put(shape,mesh);return mesh;
    }
    private static HighlightMesh build(VoxelShape shape){
        if(shape.isEmpty())return new HighlightMesh(List.of(),List.of(),List.of());
        var boxes=shape.toAabbs();
        if(boxes.size()==1)return box(BlockHighlightGeometry.corners(shape.bounds().inflate(.002)));
        // Dilate the union before extracting its boundary. Scaling its overall bounds pushes
        // inward steps into the real model, causing depth flicker on concave fill surfaces.
        VoxelShape padded=Shapes.empty();
        for(var box:boxes){var b=box.inflate(.002);padded=Shapes.or(padded,Shapes.box(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ));}
        shape=padded.optimize();
        var builder=new Builder();var segments=new ArrayList<List<Vec3>>();
        shape.forAllEdges((x,y,z,xx,yy,zz)->{var a=new Vec3(x,y,z);var b=new Vec3(xx,yy,zz);segments.add(ORDER.compare(a,b)<=0?List.of(a,b):List.of(b,a));});
        segments.sort((a,b)->{int first=ORDER.compare(a.getFirst(),b.getFirst());return first==0?ORDER.compare(a.getLast(),b.getLast()):first;});
        for(var segment:segments)if(!segment.getFirst().equals(segment.getLast()))builder.edge(segment.getFirst(),segment.getLast());
        double[][] coordinates={shape.getCoords(Direction.Axis.X).toDoubleArray(),shape.getCoords(Direction.Axis.Y).toDoubleArray(),shape.getCoords(Direction.Axis.Z).toDoubleArray()};
        int[] sizes={coordinates[0].length-1,coordinates[1].length-1,coordinates[2].length-1};
        var occupied=new HashSet<Cell>();
        for(AABB box:shape.toAabbs()){
            int[] lo={Arrays.binarySearch(coordinates[0],box.minX),Arrays.binarySearch(coordinates[1],box.minY),Arrays.binarySearch(coordinates[2],box.minZ)};
            int[] hi={Arrays.binarySearch(coordinates[0],box.maxX),Arrays.binarySearch(coordinates[1],box.maxY),Arrays.binarySearch(coordinates[2],box.maxZ)};
            for(int x=lo[0];x<hi[0];x++)for(int y=lo[1];y<hi[1];y++)for(int z=lo[2];z<hi[2];z++)occupied.add(new Cell(x,y,z));
        }
        // Merge adjacent exposed grid faces on each plane into rectangles. This removes alpha seams
        // without assuming a block's selection shape fits a single box or a unit cube.
        for(int axis=0;axis<3;axis++)for(int side:new int[]{-1,1}){
            int u=(axis+1)%3,v=(axis+2)%3;
            for(int plane=0;plane<=sizes[axis];plane++){
                boolean[][] mask=new boolean[sizes[u]][sizes[v]];
                for(int a=0;a<sizes[u];a++)for(int b=0;b<sizes[v];b++){
                    int[] cell=new int[3];cell[axis]=side<0?plane:plane-1;cell[u]=a;cell[v]=b;
                    var current=new Cell(cell[0],cell[1],cell[2]);cell[axis]+=side;
                    mask[a][b]=occupied.contains(current)&&!occupied.contains(new Cell(cell[0],cell[1],cell[2]));
                }
                for(int a=0;a<sizes[u];a++)for(int b=0;b<sizes[v];b++)if(mask[a][b]){
                    int endA=a+1;while(endA<sizes[u]&&mask[endA][b])endA++;
                    int endB=b+1;
                    outer:while(endB<sizes[v]){for(int aa=a;aa<endA;aa++)if(!mask[aa][endB])break outer;endB++;}
                    for(int aa=a;aa<endA;aa++)for(int bb=b;bb<endB;bb++)mask[aa][bb]=false;
                    var points=new ArrayList<Vec3>();
                    for(int[] corner:new int[][]{{a,b},{endA,b},{endA,endB},{a,endB}}){double[] xyz=new double[3];xyz[axis]=coordinates[axis][plane];xyz[u]=coordinates[u][corner[0]];xyz[v]=coordinates[v][corner[1]];points.add(new Vec3(xyz[0],xyz[1],xyz[2]));}
                    if(side<0)Collections.reverse(points);builder.face(points);
                }
            }
        }
        return builder.mesh();
    }
    private record Cell(int x,int y,int z) { }
    private static final class Builder {
        private final Map<Vec3,Integer> indices=new LinkedHashMap<>();
        private final Set<Edge> edges=new LinkedHashSet<>();private final List<Face> faces=new ArrayList<>();
        int index(Vec3 p){return indices.computeIfAbsent(p,ignored->indices.size());}
        void edge(Vec3 a,Vec3 b){edges.add(new Edge(index(a),index(b)));}
        void face(List<Vec3> points){faces.add(new Face(index(points.get(0)),index(points.get(1)),index(points.get(2)),index(points.get(3))));}
        HighlightMesh mesh(){return new HighlightMesh(new ArrayList<>(indices.keySet()),new ArrayList<>(edges),faces);}
    }
}
