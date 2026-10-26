#version 330
#moj_import <riserecode:panorama.glsl>
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec2 p=(texCoord*2.-1.)*vec2(Options.x,1.);vec3 ray=normalize(vec3(p*tan(.57596),-1.));
    float cp=cos(Camera.y),sp=sin(Camera.y);ray.yz=mat2(cp,-sp,sp,cp)*ray.yz;
    float cy=cos(Camera.x),sy=sin(Camera.x);ray.xz=mat2(cy,sy,-sy,cy)*ray.xz;
    if(Options.z>.5)ray.y=-ray.y;
    fragColor=vec4(sceneSky(ray),1.);
}
