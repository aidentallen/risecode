layout(std140) uniform RiseChunkParams { vec4 rise_ChunkParams; };
uniform samplerBuffer u_RiseChunkOffsets;
float rise_chunk_offset(uint section) {
    return rise_ChunkParams.x > 0.5 ? texelFetch(u_RiseChunkOffsets, int(u_RegionID * 256u + section)).r : 0.0;
}
