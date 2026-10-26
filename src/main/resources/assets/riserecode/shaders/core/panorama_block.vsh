#version 330
#moj_import <riserecode:panorama.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
in vec2 UV1;
out vec2 uv;
out vec4 tint;
out vec3 world;
out vec3 normal;
out vec2 material;
out vec4 projected;
void main(){
    vec3 p=Position;
    if(UV1.x>0.&&UV1.x<1.){
        float wave=sin(p.x*.31+p.z*.23+EyeTime.w*1.5)*.10+sin(p.z*.8+EyeTime.w*2.1)*.035;
        float bend=wave*Environment.x;
        for(int i=0;i<8;i++){float age=EyeTime.w-Gusts[i].z;float d=length(p.xz-Gusts[i].xy);bend+=sin(age*7.)*exp(-age*1.8)*exp(-d*d*.045)*Gusts[i].w*.65;}
        p.x+=bend*UV1.x;p.z+=bend*.3*UV1.x;
    }
    world=p;uv=UV0;tint=Color;normal=Normal;material=UV1;projected=SceneMatrix*vec4(p,1.);gl_Position=projected;
}
