#version 330
#moj_import <riserecode:panorama.glsl>
uniform sampler2D Sampler0;
in vec2 uv;
in vec4 tint;
in vec3 world;
in vec3 normal;
in vec2 material;
in vec4 projected;
out vec4 fragColor;
void main(){
    if(Options.z>.5&&world.y<.13)discard;
    vec4 texel=texture(Sampler0,uv)*tint;if(texel.a<.12)discard;
    float direct=max(0.,dot(normalize(normal),normalize(vec3(-.5,.85,-.3))));
    float light=(.26+Environment.w*.48+direct*Environment.w*.32)*mix(1.,material.y,Environment.w);
    vec3 warmth=mix(vec3(.52,.64,1.),vec3(1.,.99,.91),Environment.w);
    float sunset=exp(-pow((Camera.z-18.)/1.6,2.))+exp(-pow((Camera.z-6.)/1.6,2.));warmth=mix(warmth,vec3(1.,.70,.42),sunset*.3);
    vec3 color=texel.rgb*light*warmth;
    float dist=distance(EyeTime.xyz,world);
    float fog=1.-exp(-dist*(.0015+Environment.z*.011));color=mix(color,HorizonColor.rgb,fog);
    fragColor=vec4(color,texel.a);
}
