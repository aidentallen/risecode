#version 330
#moj_import <riserecode:glass.glsl>
#moj_import <minecraft:projection.glsl>
uniform sampler2D Sampler0;
uniform sampler2D GlassScene;
uniform sampler2D GlassDepth;
uniform sampler2D GlassCoverage;
in vec2 texCoord;
in vec4 tint;
in vec3 viewPosition;
out vec4 fragColor;
vec3 scene(vec2 uv,float radius) {
    vec2 px=Screen.xy*radius;
    vec3 c=texture(GlassScene,uv).rgb*0.2;
    for(int i=0;i<8;i++) {
        float a=float(i)*0.78539816;
        c+=texture(GlassScene,clamp(uv+vec2(cos(a),sin(a))*px,Screen.xy,vec2(1.0)-Screen.xy)).rgb*0.1;
    }
    return c;
}
void main() {
    vec4 original=texture(Sampler0,texCoord)*tint;
    if(original.a<0.1)discard;
    vec3 n=normalize(cross(dFdx(viewPosition),dFdy(viewPosition)));
    vec3 eye=ProjMat[3][3]>0.5?vec3(0.0,0.0,1.0):normalize(-viewPosition+vec3(0.0,0.0,0.00001));
    if(dot(n,eye)<0.0)n=-n;
    if(Screen.z>1.5){fragColor=vec4(n*0.5+0.5,original.a);return;}
    float facing=clamp(dot(n,eye),0.0,1.0);
    float fresnel=pow(1.0-facing,2.5);
    vec2 uv=gl_FragCoord.xy*Screen.xy;
    float wave=sin(dot(viewPosition,vec3(2.3,3.7,1.1))-Motion.w*Motion.y*1.7);
    float shimmer=pow(max(0.0,wave),24.0)*Motion.x;
    vec2 offset=(n.xy*14.0+vec2(wave,-wave)*Motion.x*2.0)*Material.z*Screen.xy*Screen.w;
    vec2 refracted=clamp(uv+offset,Screen.xy,vec2(1.0)-Screen.xy);
    // Reverse depth: don't pull foreground objects across a visible glass surface.
    if(texture(GlassDepth,refracted).r>gl_FragCoord.z+0.0001)refracted=uv;
    vec4 glass=glassPaint(GlassColor,GlassColor2,GlassColorMode,gl_FragCoord.xy);
    vec4 edgeColor=glassPaint(EdgeColor,EdgeColor2,EdgeColorMode,gl_FragCoord.xy);
    float rim=pow(1.0-facing,max(0.18,3.2/max(Lighting.y,0.25)));
    float screenEdge=0.0;
    vec4 center=texture(GlassCoverage,uv);
    for(int i=0;i<8;i++){
        float a=float(i)*0.78539816;
        vec4 nearby=texture(GlassCoverage,uv+vec2(cos(a),sin(a))*Screen.xy*Lighting.y*Screen.w);
        screenEdge=max(screenEdge,max(center.a-nearby.a,length(center.rgb-nearby.rgb)*0.9));
    }
    rim=max(rim,smoothstep(0.10,0.65,screenEdge));
    float gloss=pow(max(dot(reflect(-normalize(vec3(-0.4,0.8,0.6)),n),eye),0.0),48.0)*Material.w;
    vec3 light=edgeColor.rgb*edgeColor.a*(rim*Lighting.x+shimmer*0.65)+vec3(gloss*0.65);
    if(Screen.z>0.5) {
        float spread=clamp(Lighting.w*Screen.w/32.0,0.01,1.0);
        fragColor=vec4(light*Lighting.z*0.45/spread,spread);
        return;
    }
    vec3 behind=scene(refracted,Material.y*12.0*Screen.w);
    vec3 absorption=mix(vec3(1.0),glass.rgb,Material.x*glass.a*0.8);
    vec3 body=behind*absorption;
    body=mix(body,glass.rgb,Material.x*glass.a*0.24+Material.y*0.06);
    body=mix(body,original.rgb,Motion.z);
    body+=light*(0.4+fresnel*0.6);
    fragColor=vec4(body,original.a);
}
