#version 330
#moj_import <riserecode:glass.glsl>
uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec2 uv=gl_FragCoord.xy*Screen.xy;
    vec4 sum=vec4(0.0);float weight=0.0;
    for(int i=-32;i<=32;i++){
        vec4 s=texture(Sampler0,uv+vec2(float(i)*Screen.x,0.0));
        float radius=max(1.0,s.a*32.0);
        float w=exp(-float(i*i)/(2.0*radius*radius*0.16));
        sum+=s*w;weight+=w;
    }
    fragColor=sum/max(weight,1.0);
}
