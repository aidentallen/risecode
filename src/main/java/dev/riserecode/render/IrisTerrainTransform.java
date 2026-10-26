package dev.riserecode.render;

/** All-or-nothing transformation of Iris-generated declarations. Never returns a partial patch. */
public final class IrisTerrainTransform {
    private static final String TRANSLATION="return _get_relative_chunk_coord(pos) * vec3(16.0f);";
    private static final String FADE="mc_chunkFade = (chunkFade < 0) ? 1.0 : fade;";
    private static final String NO_FADE="const float mc_chunkFade = -1.0;";
    private static final String REGION="uniform uint u_RegionID;";
    private IrisTerrainTransform(){ }
    public static String[] patch(String[] nativeNodes){
        String source=String.join("\n",nativeNodes);
        if(occurrences(source,TRANSLATION)!=1||occurrences(source,FADE)+occurrences(source,NO_FADE)!=1||occurrences(source,REGION)!=1||source.contains("rise_ChunkParams")){return nativeNodes;}
        var nodes=new java.util.ArrayList<String>();
        for(String node:nativeNodes){
            nodes.add(node.replace(TRANSLATION,"return _get_relative_chunk_coord(pos) * vec3(16.0f) + vec3(0.0, rise_chunk_offset(pos), 0.0);")
                    .replace(FADE,"mc_chunkFade = (rise_ChunkParams.y > 0.5 || chunkFade < 0) ? 1.0 : fade;"));
            if(node.contains(REGION)){
                nodes.add("layout(std140) uniform RiseChunkParams { vec4 rise_ChunkParams; };");
                nodes.add("uniform samplerBuffer u_RiseChunkOffsets;");
                nodes.add("float rise_chunk_offset(uint section) { return rise_ChunkParams.x > 0.5 ? texelFetch(u_RiseChunkOffsets, int(u_RegionID * 256u + section)).r : 0.0; }");
            }
        }
        return nodes.toArray(String[]::new);
    }
    private static int occurrences(String source,String needle){int count=0,offset=0;while((offset=source.indexOf(needle,offset))>=0){count++;offset+=needle.length();}return count;}
}
