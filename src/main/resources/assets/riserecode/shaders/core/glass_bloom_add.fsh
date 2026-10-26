#version 330
#moj_import <riserecode:glass.glsl>
uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec2 uv=gl_FragCoord.xy*Screen.xy;vec3 sum=vec3(0.0);float weight=0.0;
    for(int i=-32;i<=32;i++){
        vec4 s=texture(Sampler0,uv+vec2(0.0,float(i)*Screen.y));
        float radius=max(1.0,s.a*32.0);float w=exp(-float(i*i)/(2.0*radius*radius*0.16));
        sum+=s.rgb*w;weight+=w;
    }
    fragColor=vec4(sum/max(weight,1.0),0.0);
}
