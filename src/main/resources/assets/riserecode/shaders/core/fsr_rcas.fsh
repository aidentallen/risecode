#version 430
// AMD reference RCAS, pinned and attributed in include/fsr1/source.json.
#define A_GPU 1
#define A_GLSL 1
#define FSR_RCAS_F 1
#moj_import <riserecode:fsr1/ffx_a.glsl>
#moj_import <riserecode:fsr1/ffx_fsr1.glsl>
uniform sampler2D Sampler0;
layout(std140) uniform ScaleParams { vec4 Extents; vec4 Options; };
in vec2 texCoord;
out vec4 fragColor;
AF4 FsrRcasLoadF(ASU2 p) { return texelFetch(Sampler0,clamp(p,ivec2(0),ivec2(Extents.zw)-1),0); }
void FsrRcasInputF(inout AF1 r,inout AF1 g,inout AF1 b) { }
void main() {
    if(Options.x<=0.0) { fragColor=FsrRcasLoadF(ASU2(gl_FragCoord.xy)); return; }
    // Reference con[0] is the linear strength. Slider 0..1 maps directly to this strength.
    AU4 con=AU4(floatBitsToUint(Options.x),0u,0u,0u);
    AF1 r,g,b;
    FsrRcasF(r,g,b,AU2(gl_FragCoord.xy),con);
    fragColor=vec4(r,g,b,1.0);
}
