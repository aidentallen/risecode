#version 330
#moj_import <riserecode:ambience.glsl>
in vec2 skyUV;
out vec4 fragColor;
void main() {
    vec2 q=skyUV*2.0-1.0; vec3 d=normalize(vec3(-q.x*View.w*View.z,q.y*View.z,1.0));
    float cp=cos(View.y),sp=sin(View.y); d=vec3(d.x,d.y*cp-d.z*sp,d.y*sp+d.z*cp);
    float cy=cos(View.x),sy=sin(View.x); d=vec3(d.x*cy-d.z*sy,d.y,d.x*sy+d.z*cy);
    fragColor=vec4(ambSky(d),1.0);
}
