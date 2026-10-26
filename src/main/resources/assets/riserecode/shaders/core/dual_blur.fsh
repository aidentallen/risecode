#version 330
uniform sampler2D Sampler0;
layout(std140) uniform BlurParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec2 d=Params.xy*Params.w;
    vec3 c;
    if(Params.z<0.5){
        c=texture(Sampler0,texCoord).rgb*4.0;
        c+=texture(Sampler0,texCoord+d).rgb+texture(Sampler0,texCoord-d).rgb;
        c+=texture(Sampler0,texCoord+vec2(d.x,-d.y)).rgb+texture(Sampler0,texCoord+vec2(-d.x,d.y)).rgb;
        c/=8.0;
    }else{
        c=texture(Sampler0,texCoord+vec2(-2.0*d.x,0)).rgb+texture(Sampler0,texCoord+vec2(2.0*d.x,0)).rgb;
        c+=texture(Sampler0,texCoord+vec2(0,-2.0*d.y)).rgb+texture(Sampler0,texCoord+vec2(0,2.0*d.y)).rgb;
        c+=2.0*(texture(Sampler0,texCoord+d).rgb+texture(Sampler0,texCoord-d).rgb+texture(Sampler0,texCoord+vec2(d.x,-d.y)).rgb+texture(Sampler0,texCoord+vec2(-d.x,d.y)).rgb);
        c/=12.0;
    }
    fragColor=vec4(c,1);
}
