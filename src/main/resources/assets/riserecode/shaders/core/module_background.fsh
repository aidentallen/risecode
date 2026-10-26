#version 330
uniform sampler2D Sampler0;
in vec2 local;
in vec2 screenUV;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
void main() {
    float packed=data.x<0.0?data.x+65536.0:data.x;
    float fit=mod(floor(packed/4096.0),4.0),motion=floor(packed/16384.0);
    vec2 size=vec2(mod(packed,4096.0),data.y);
    float radius=mod(data.z,128.0);
    vec2 q=abs(local)-size*.5+radius;
    float distance=length(max(q,0.0))+min(max(q.x,q.y),0.0)-radius;
    float mask=1.0-smoothstep(-.5,.5,distance);
    if(mask<=0.0) discard;
    vec2 uv=local/size+.5;
    vec2 tex=vec2(textureSize(Sampler0,0));float a=tex.x/tex.y,b=size.x/size.y;
    vec2 span=fit>1.5?vec2(1):fit>.5?vec2(max(1.0,b/a),max(1.0,a/b)):vec2(min(1.0,b/a),min(1.0,a/b));
    float time=(data.w<0.0?data.w+65536.0:data.w)/65536.0*6.28318530718;
    if(motion>.5) { float zoom=motion<1.5?1.1:1.08+.06*sin(time);span/=zoom; }
    uv=(uv-.5)*span+.5;
    if(motion>.5&&abs(motion-2.0)>.1){uv+=vec2(sin(time),cos(time))*(1.0-min(span,vec2(1)))*.35;}
    if(fit>.5&&fit<1.5&&(any(lessThan(uv,vec2(0)))||any(greaterThan(uv,vec2(1))))) discard;
    vec2 step=vec2(floor(data.z/128.0))/tex;
    vec4 color=texture(Sampler0,uv)*.25;
    color+=(texture(Sampler0,uv+vec2(step.x,0))+texture(Sampler0,uv-vec2(step.x,0))+texture(Sampler0,uv+vec2(0,step.y))+texture(Sampler0,uv-vec2(0,step.y)))*.125;
    color+=(texture(Sampler0,uv+step)+texture(Sampler0,uv-step)+texture(Sampler0,uv+vec2(step.x,-step.y))+texture(Sampler0,uv+vec2(-step.x,step.y)))*.0625;
    fragColor=color*tint*mask;
}
