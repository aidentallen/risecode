package dev.riserecode.cosmetics;

import com.google.gson.JsonParser;
import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.io.GltfModelReader;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Restricted self-contained GLB reader. The library never resolves network/file references. */
public final class CosmeticModel {
    public record Part(int texture, float[] vertices, int[] indices, boolean unlit) { }
    private final GltfModel model;
    private final Map<NodeModel,float[][]> rest=new IdentityHashMap<>();
    private final List<byte[]> images=new ArrayList<>();
    private record Topology(int[] indices,int texture,boolean unlit) { }
    private final Map<MeshPrimitiveModel,Topology> topology=new IdentityHashMap<>();
    private final long[] sampleTicks={Long.MIN_VALUE,Long.MIN_VALUE};
    private final double[] sampleSpeeds=new double[2];
    private final List<List<Part>> tickSamples=new ArrayList<>(java.util.Arrays.asList(null,null));
    private float centerX,centerZ,floor,normalization=1;
    private CosmeticModel(GltfModel model) {
        this.model=model;
        for(var node:model.getNodeModels()) { rest.put(node,new float[][]{copy(node.getTranslation(),new float[]{0,0,0}),copy(node.getRotation(),new float[]{0,0,0,1}),copy(node.getScale(),new float[]{1,1,1})}); }
        for(var image:model.getImageModels()) {var data=image.getImageData().duplicate();byte[] bytes=new byte[data.remaining()];data.get(bytes);png(bytes,2048);images.add(bytes);}
        var min=new Vector3f(Float.POSITIVE_INFINITY);var max=new Vector3f(Float.NEGATIVE_INFINITY);
        for(var part:sampleRaw(0,false)){for(int i=0;i<part.vertices.length;i+=8){var p=new Vector3f(part.vertices[i],part.vertices[i+1],part.vertices[i+2]);min.min(p);max.max(p);}}
        if(!Float.isFinite(min.x)||max.distance(min)>10000){throw new IllegalArgumentException("Invalid model bounds");}
        centerX=(min.x+max.x)/2;centerZ=(min.z+max.z)/2;floor=min.y;normalization=.55F/Math.max(.001F,max.y-min.y);
    }
    private static float[] copy(float[] value,float[] fallback){return value==null?fallback:value.clone();}
    public List<byte[]> images(){return List.copyOf(images);}
    /** Retained primitive payload, excluding JVM object headers and library object graphs. */
    public synchronized long decodedBytes(){
        long bytes=0;for(var buffer:model.getBufferModels()){bytes+=buffer.getBufferData().capacity();}
        for(var image:images){bytes+=image.length;}for(var value:rest.values()){for(var a:value){bytes+=a.length*4L;}}
        for(var value:topology.values()){bytes+=value.indices().length*4L;}
        for(var sample:tickSamples){if(sample!=null){for(var part:sample){bytes+=part.vertices().length*4L;}}}return bytes;
    }
    public static void png(byte[] bytes,int max) {
        if(bytes.length<24||bytes.length>16_777_216||ByteBuffer.wrap(bytes).getLong()!=0x89504e470d0a1a0aL){throw new IllegalArgumentException("Expected PNG");}
        var b=ByteBuffer.wrap(bytes);int w=b.getInt(16),h=b.getInt(20);if(w<1||h<1||w>max||h>max){throw new IllegalArgumentException("PNG dimensions out of range");}
        try{var image=javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));if(image==null||image.getWidth()!=w||image.getHeight()!=h){throw new IllegalArgumentException("Invalid PNG image");}}
        catch(IOException e){throw new IllegalArgumentException("Invalid PNG image",e);}
    }
    public static CosmeticModel read(byte[] bytes) throws IOException {
        if(bytes.length<24||bytes.length>16_777_216){throw new IOException("Model must be below 16 MiB");}
        var b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if(b.getInt()!=0x46546c67||b.getInt()!=2||b.getInt()!=bytes.length){throw new IOException("Expected self-contained GLB 2");}
        int length=b.getInt();if(b.getInt()!=0x4e4f534a||length<2||length>4_194_304||length>bytes.length-20){throw new IOException("Invalid GLB JSON");}
        var json=JsonParser.parseString(new String(bytes,20,length,StandardCharsets.UTF_8)).getAsJsonObject();
        for(String key:List.of("buffers","images")){if(json.has(key)){for(var e:json.getAsJsonArray(key)){if(e.getAsJsonObject().has("uri")){throw new IOException("External model references are unsupported");}}}}
        for(String key:List.of("nodes","meshes","animations","skins","accessors")){if(json.has(key)&&json.getAsJsonArray(key).size()>2048){throw new IOException("Model complexity limit");}}
        if(json.has("accessors")){for(var a:json.getAsJsonArray("accessors")){int n=a.getAsJsonObject().get("count").getAsInt();if(n<0||n>65536){throw new IOException("Accessor limit");}}}
        if(json.has("nodes")){var nodes=json.getAsJsonArray("nodes");for(int i=0;i<nodes.size();i++){checkTree(nodes,i,new HashSet<>(),0);}}
        var model=new GltfModelReader().readWithoutReferences(new ByteArrayInputStream(bytes));
        if(model.getImageModels().size()>8||model.getNodeModels().size()>256){throw new IOException("Model complexity limit");}
        for(var skin:model.getSkinModels()){if(skin.getJoints().size()>128){throw new IOException("Joint limit");}}
        for(var mesh:model.getMeshModels()){for(var p:mesh.getMeshPrimitiveModels()){if(p.getMode()!=4||!p.getTargets().isEmpty()){throw new IOException("Only triangle meshes without morph targets are supported");}}}
        return new CosmeticModel(model);
    }
    private static void checkTree(com.google.gson.JsonArray nodes,int index,Set<Integer> path,int depth)throws IOException{
        if(index<0||index>=nodes.size()||depth>64||!path.add(index)){throw new IOException("Invalid model hierarchy");}
        var node=nodes.get(index).getAsJsonObject();if(node.has("children")){for(var c:node.getAsJsonArray("children")){checkTree(nodes,c.getAsInt(),path,depth+1);}}path.remove(index);
    }
    public synchronized List<Part> sample(float time,boolean walking){
        var parts=sampleRaw(time,walking);var result=new ArrayList<Part>();
        for(var part:parts){float[] v=part.vertices;for(int i=0;i<v.length;i+=8){v[i]=(v[i]-centerX)*normalization;v[i+1]=(v[i+1]-floor)*normalization;v[i+2]=(v[i+2]-centerZ)*normalization;}result.add(part);}return List.copyOf(result);
    }
    /** Reuse immutable transformed parts within a game tick; deferred draws never mutate cached arrays. */
    public synchronized List<Part> sampleTick(float age,double speed,boolean walking){
        int slot=walking?1:0;long tick=(long)Math.floor(age);
        if(sampleTicks[slot]!=tick||sampleSpeeds[slot]!=speed){
            tickSamples.set(slot,sample((float)(tick*speed/20),walking));sampleTicks[slot]=tick;sampleSpeeds[slot]=speed;
        }
        return tickSamples.get(slot);
    }
    private List<Part> sampleRaw(float time,boolean walking){
        rest.forEach((n,r)->{n.setTranslation(r[0].clone());n.setRotation(r[1].clone());n.setScale(r[2].clone());});
        AnimationModel animation=null;
        for(String name:walking?List.of("Walk","Flying","Idle"):List.of("Idle","Flying","Walk")){for(var a:model.getAnimationModels()){if(name.equalsIgnoreCase(a.getName())){animation=a;break;}}if(animation!=null){break;}}
        if(animation!=null){for(var c:animation.getChannels()){var input=AccessorDatas.createFloat(c.getSampler().getInput());var output=AccessorDatas.createFloat(c.getSampler().getOutput());
            int count=input.getNumElements();if(count==0){continue;}float duration=input.get(count-1,0),t=duration>0?Math.max(0,time)%duration:0;int a=0;while(a+1<count&&input.get(a+1,0)<=t){a++;}int next=Math.min(a+1,count-1);
            float blend=next==a?0:(t-input.get(a,0))/Math.max(.000001F,input.get(next,0)-input.get(a,0));
            if(c.getSampler().getInterpolation()==AnimationModel.Interpolation.STEP){blend=0;}else if(c.getSampler().getInterpolation()!=AnimationModel.Interpolation.LINEAR){throw new IllegalArgumentException("Bake non-linear animations before distribution");}
            int components=output.getNumComponentsPerElement();float[] value=new float[components];for(int j=0;j<components;j++){value[j]=output.get(a,j)+(output.get(next,j)-output.get(a,j))*blend;}
            if(c.getPath().equals("rotation")){var q=new Quaternionf(output.get(a,0),output.get(a,1),output.get(a,2),output.get(a,3));q.slerp(new Quaternionf(output.get(next,0),output.get(next,1),output.get(next,2),output.get(next,3)),blend);value=new float[]{q.x,q.y,q.z,q.w};c.getNodeModel().setRotation(value);}
            else if(c.getPath().equals("translation")){c.getNodeModel().setTranslation(value);}else if(c.getPath().equals("scale")){c.getNodeModel().setScale(value);}
        }}
        var result=new ArrayList<Part>();int total=0;
        for(var node:model.getNodeModels()){for(var mesh:node.getMeshModels()){for(var primitive:mesh.getMeshPrimitiveModels()){
            var attributes=primitive.getAttributes();var positions=AccessorDatas.createFloat(attributes.get("POSITION"));int count=positions.getNumElements();total+=count;if(total>65536){throw new IllegalArgumentException("Vertex limit");}
            var normals=attributes.containsKey("NORMAL")?AccessorDatas.createFloat(attributes.get("NORMAL")):null;
            var uv=attributes.containsKey("TEXCOORD_0")?AccessorDatas.createFloat(attributes.get("TEXCOORD_0")):null;
            var weights=attributes.containsKey("WEIGHTS_0")?AccessorDatas.createFloat(attributes.get("WEIGHTS_0")):null;
            var joints=attributes.containsKey("JOINTS_0")?attributes.get("JOINTS_0").getAccessorData():null;
            var transform=new Matrix4f().set(node.computeGlobalTransform(null));var skin=node.getSkinModel();Matrix4f[] bones=null;
            if(skin!=null){bones=new Matrix4f[skin.getJoints().size()];for(int i=0;i<bones.length;i++){bones[i]=new Matrix4f().set(skin.getJoints().get(i).computeGlobalTransform(null)).mul(new Matrix4f().set(skin.getInverseBindMatrix(i,null)));}}
            float[] vertices=new float[count*8];
            for(int i=0;i<count;i++){var pos=new Vector3f(positions.get(i,0),positions.get(i,1),positions.get(i,2));var normal=normals==null?new Vector3f(0,1,0):new Vector3f(normals.get(i,0),normals.get(i,1),normals.get(i,2));
                if(bones!=null&&weights!=null&&joints!=null){var p=new Vector3f();var n=new Vector3f();for(int j=0;j<4;j++){float w=weights.get(i,j);int joint=integer(joints,i,j);if(joint<0||joint>=bones.length){throw new IllegalArgumentException("Invalid joint");}p.add(bones[joint].transformPosition(new Vector3f(pos)).mul(w));n.add(bones[joint].transformDirection(new Vector3f(normal)).mul(w));}pos=p;normal=n;}
                else{transform.transformPosition(pos);transform.transformDirection(normal);}normal.normalize();
                int v=i*8;vertices[v]=pos.x;vertices[v+1]=pos.y;vertices[v+2]=pos.z;vertices[v+3]=normal.x;vertices[v+4]=normal.y;vertices[v+5]=normal.z;vertices[v+6]=uv==null?0:uv.get(i,0);vertices[v+7]=uv==null?0:uv.get(i,1);
                for(int j=0;j<8;j++){if(!Float.isFinite(vertices[v+j])){throw new IllegalArgumentException("Non-finite mesh");}}
            }
            Topology fixed=topology.computeIfAbsent(primitive,p->{
                int[] indices=new int[p.getIndices()==null?count:p.getIndices().getCount()];if(indices.length>196608||indices.length%3!=0){throw new IllegalArgumentException("Triangle limit");}
                for(int i=0;i<indices.length;i++){indices[i]=p.getIndices()==null?i:integer(p.getIndices().getAccessorData(),i,0);if(indices[i]<0||indices[i]>=count){throw new IllegalArgumentException("Invalid triangle index");}}
                int texture=0;if(p.getMaterialModel() instanceof MaterialModelV2 material&&material.getBaseColorTexture()!=null){texture=model.getImageModels().indexOf(material.getBaseColorTexture().getImageModel());}
                boolean unlit=p.getMaterialModel()!=null&&p.getMaterialModel().getExtensions()!=null&&p.getMaterialModel().getExtensions().containsKey("KHR_materials_unlit");
                return new Topology(indices,texture,unlit);
            });
            result.add(new Part(fixed.texture,vertices,fixed.indices,fixed.unlit));
        }}}return result;
    }
    private static int integer(AccessorData data,int element,int component){
        if(data instanceof AccessorByteData b){return b.getInt(element,component);}if(data instanceof AccessorShortData s){return s.getInt(element,component);}if(data instanceof AccessorIntData i){return i.get(element,component);}throw new IllegalArgumentException("Expected integer accessor");
    }
}
