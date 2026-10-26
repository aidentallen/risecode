#version 330
uniform sampler2D Sampler0;
layout(std140) uniform BlurParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec3 color;
    if(Params.z==0.0){
        vec2 d=.5/vec2(textureSize(Sampler0,0));
        color=(texture(Sampler0,texCoord+d).rgb+texture(Sampler0,texCoord-d).rgb+texture(Sampler0,texCoord+vec2(d.x,-d.y)).rgb+texture(Sampler0,texCoord+vec2(-d.x,d.y)).rgb)*.25;
    }else{color=texture(Sampler0,texCoord).rgb;}
    fragColor=vec4(color,Params.w);
}
