layout(std140) uniform GlassParams {
    vec4 GlassColor; vec4 GlassColor2; vec4 GlassColorMode;
    vec4 EdgeColor; vec4 EdgeColor2; vec4 EdgeColorMode;
    vec4 Material; // opacity, frosting, refraction, highlights
    vec4 Lighting; // edges, thickness, glow, spread
    vec4 Motion; // shimmer, speed, original texture, time
    vec4 Screen; // inverse dimensions, pass, resolution scale
};
vec3 glassHsv(float h,float s) {
    return mix(vec3(1.0),clamp(abs(fract(vec3(h)+vec3(0.0,2.0/3.0,1.0/3.0))*6.0-3.0)-1.0,0.0,1.0),s);
}
vec4 glassPaint(vec4 a,vec4 b,vec4 mode,vec2 p) {
    float phase=fract((mode.y<0.5?p.x:p.y)/max(mode.w,1.0)+Motion.w*mode.z);
    if(mode.x<0.5)return a;
    if(mode.x<1.5)return mix(a,b,1.0-abs(phase*2.0-1.0));
    if(mode.x<2.5)return vec4(glassHsv(phase,0.65),a.a);
    if(mode.x<3.5)return vec4(glassHsv(0.5+0.35*(0.5+0.5*sin(phase*6.283185)),0.45),a.a);
    return mix(a,b,0.5+0.5*sin(phase*6.283185));
}
