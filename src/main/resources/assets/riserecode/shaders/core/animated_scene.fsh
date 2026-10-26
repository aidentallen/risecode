#version 330
layout(std140) uniform BlurParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
void main(){
    vec2 uv=texCoord;float t=Params.x;vec3 c;
    if(Params.y<0.5){
        float a=.5+.5*sin(uv.x*3.0+uv.y*2.0+t*.3);
        c=mix(vec3(.025,.04,.11),vec3(.18,.06,.28),a);
        c+=vec3(.02,.15,.17)*(.5+.5*sin(uv.y*5.0-t*.2));
    }else if(Params.y<1.5){
        c=mix(vec3(.015,.02,.07),vec3(.03,.08,.12),uv.y);
        for(int i=0;i<3;i++){
            float wave=.45+.13*sin(uv.x*4.0+t*.18+float(i))+.05*sin(uv.x*13.0-t*.11);
            float band=exp(-abs(uv.y-wave-float(i)*.04)*23.0);
            c+=mix(vec3(.05,.4,.22),vec3(.25,.08,.36),uv.x)*band*.55;
        }
    }else{
        c=vec3(.009,.014,.035);
        vec2 p=uv*vec2(160,90)+vec2(t*.8,t*.3);vec2 cell=floor(p);vec2 q=fract(p)-.5;
        float star=step(.982,hash(cell))*exp(-dot(q,q)*80.0);
        c+=vec3(.7,.82,1)*star*(.65+.35*sin(t+hash(cell)*25.0));
    }
    fragColor=vec4(c,1);
}
