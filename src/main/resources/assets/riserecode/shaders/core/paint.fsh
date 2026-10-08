#version 330
in vec2 local; in vec2 screenPosition;
flat in vec4 shape; flat in vec4 first; flat in vec4 second; flat in vec4 policy;
flat in vec4 nextFirst; flat in vec4 nextSecond; flat in vec4 nextPolicy; flat in vec4 info;
out vec4 fragColor;
vec3 hsv(float h,float s){ vec3 k=abs(fract(vec3(h)+vec3(0,0.666666667,0.333333333))*6.0-3.0); return mix(vec3(1),clamp(k-1.0,0.0,1.0),s); }
vec4 samplePaint(vec4 a,vec4 b,vec4 p){
 if(p.x<0.5)return a;
 float coord=p.y<0.5?screenPosition.x:screenPosition.y;
 float phase=fract(coord/p.w+info.y*p.z);vec4 c=a;
 if(p.x<1.5)c=mix(a,b,1.0-abs(phase*2.0-1.0));
 else if(p.x<2.5)c=vec4(hsv(phase,0.65),a.a);
 else if(p.x<3.5)c=vec4(hsv(0.5+0.35*(0.5+0.5*sin(phase*6.28318530718)),0.45),a.a);
 else if(p.x<4.5)c=mix(a,b,0.5+0.5*sin(phase*6.28318530718));
 return floor(c*255.0+0.5)/255.0;
}
void main(){
 vec2 q=abs(local)-shape.xy+shape.z;
 float distance=length(max(q,0.0))+min(max(q.x,q.y),0.0)-shape.z;
 float aa=max(fwidth(distance),0.001), coverage;
 if(shape.w<0.0){float sigma=max(-shape.w/3.0,0.001);coverage=exp(-pow(max(distance,0.0),2.0)/(2.0*sigma*sigma));}
 else {coverage=1.0-smoothstep(-aa*0.5,aa*0.5,distance);if(shape.w>0.0)coverage*=smoothstep(-shape.w-aa*0.5,-shape.w+aa*0.5,distance);}
 vec4 c=floor(mix(samplePaint(first,second,policy),samplePaint(nextFirst,nextSecond,nextPolicy),info.x)*255.0+0.5)/255.0;
 fragColor=vec4(c.rgb,c.a*coverage);
}
