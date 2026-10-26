#version 330
uniform sampler2D Sampler0;
in vec2 local;
in vec2 screenUV;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
void main() {
    if (data.z > 0.0 && data.w > 0.0) {
        vec2 p = (vec2(screenUV.x,1.0-screenUV.y) - data.xy/16000.0)/(data.zw/16000.0);
        if(dot(p,p)>1.0) discard;
    }
    fragColor=texture(Sampler0,local)*tint;
}
