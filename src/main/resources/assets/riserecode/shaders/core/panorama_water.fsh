#version 330
#moj_import <riserecode:panorama.glsl>
uniform sampler2D Sampler0;
uniform sampler2D Reflection;
in vec2 uv;
in vec4 tint;
in vec3 world;
in vec3 normal;
in vec2 material;
in vec4 projected;
out vec4 fragColor;
void main(){
    float wave=(sin(world.x*1.7+EyeTime.w*.9)+cos(world.z*1.4-EyeTime.w*.7))*.012*Environment.x;
    for(int i=0;i<12;i++){float age=EyeTime.w-Ripples[i].z;float d=length(world.xz-Ripples[i].xy);wave+=sin((d-age*3.)*12.)*exp(-pow((d-age*3.)*2.,2.))*exp(-age*.9)*Ripples[i].w*.075;}
    vec2 cell=floor(world.xz*1.7);float drop=fract(EyeTime.w*1.3+hashScene(cell));float ring=length(fract(world.xz*1.7)-.5);
    wave+=sin((ring-drop)*40.)*exp(-abs(ring-drop)*18.)*(1.-drop)*Environment.y*.013;
    vec3 viewDir=normalize(world-EyeTime.xyz);vec3 reflected=reflect(viewDir,normalize(vec3(wave,1.,wave*.7)));
    vec3 sky=sceneSky(reflected);vec2 screen=projected.xy/projected.w*.5+.5;
    if(Options.y>.5)sky=mix(sky,texture(Reflection,screen+vec2(wave*.025)).rgb,.74);
    float fresnel=.30+.65*pow(1.-max(0.,-viewDir.y),3.);
    vec3 water=WaterColor.rgb*(.65+texture(Sampler0,uv+vec2(wave*.002)).r*.35);
    vec3 color=mix(water,sky,fresnel)+vec3(.45,.7,.7)*max(0.,wave)*.6;
    float fog=1.-exp(-distance(world,EyeTime.xyz)*(.0015+Environment.z*.011));
    fragColor=vec4(mix(color,HorizonColor.rgb,fog),1.);
}
