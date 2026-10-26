#version 430
// AMD reference EASU, pinned and attributed in include/fsr1/source.json.
#define A_GPU 1
#define A_GLSL 1
#define FSR_EASU_F 1
#moj_import <riserecode:fsr1/ffx_a.glsl>
#moj_import <riserecode:fsr1/ffx_fsr1.glsl>
uniform sampler2D Sampler0;
layout(std140) uniform ScaleParams { vec4 Extents; vec4 Options; };
in vec2 texCoord;
out vec4 fragColor;
AF4 FsrEasuRF(AF2 p) { return textureGather(Sampler0,p,0); }
AF4 FsrEasuGF(AF2 p) { return textureGather(Sampler0,p,1); }
AF4 FsrEasuBF(AF2 p) { return textureGather(Sampler0,p,2); }
void main() {
    AU4 c0,c1,c2,c3;
    FsrEasuCon(c0,c1,c2,c3,Extents.x,Extents.y,Extents.x,Extents.y,Extents.z,Extents.w);
    AF3 color;
    FsrEasuF(color,AU2(gl_FragCoord.xy),c0,c1,c2,c3);
    fragColor=vec4(color,1.0);
}
