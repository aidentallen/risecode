layout(std140) uniform AmbienceParams {
    vec4 SkyFirst; vec4 SkySecond; vec4 WaterShallow; vec4 WaterDeep;
    vec4 Atmosphere; // time, speed, intensity, mode
    vec4 Detail; // softness, stars, octaves, sky enabled
    vec4 Waves; // strength, speed, transparency, reflection
    vec4 View; // yaw, pitch, tan half FOV, aspect
    vec4 WaterStill; vec4 WaterFlow; vec4 WaterOverlay;
    vec4 CameraPosition;
};
float ambHash(vec3 p) { return fract(sin(dot(p, vec3(127.1,311.7,74.7))) * 43758.5453); }
float ambNoise(vec3 p) {
    vec3 i=floor(p), f=fract(p); f=f*f*(3.0-2.0*f);
    return mix(mix(mix(ambHash(i),ambHash(i+vec3(1,0,0)),f.x),mix(ambHash(i+vec3(0,1,0)),ambHash(i+vec3(1,1,0)),f.x),f.y),
        mix(mix(ambHash(i+vec3(0,0,1)),ambHash(i+vec3(1,0,1)),f.x),mix(ambHash(i+vec3(0,1,1)),ambHash(i+vec3(1,1,1)),f.x),f.y),f.z);
}
float ambFbm(vec3 p) { float result=0.0, weight=0.5; for(int i=0;i<5;i++){ if(float(i)>=Detail.z) break; result+=ambNoise(p)*weight; p=p*2.03+vec3(19.7); weight*=0.5; } return result; }
vec3 ambSky(vec3 d) {
    d=normalize(d); float t=Atmosphere.x*Atmosphere.y, h=clamp(d.y*0.5+0.5,0.0,1.0);
    vec3 base=mix(vec3(0.035,0.055,0.10),vec3(0.006,0.012,0.045),h), c=base;
    float n=ambFbm(d*4.0+vec3(t*0.13,0,t*0.06));
    if(Atmosphere.w<0.5) {
        float curtain=sin(d.x*9.0+d.z*7.0+n*6.0+t)*0.12+0.25;
        float glow=exp(-abs(d.y-curtain)/(0.018+Detail.x*0.11))*(0.45+n);
        c+=mix(SkyFirst.rgb,SkySecond.rgb,clamp(n+d.y*0.5,0.0,1.0))*glow*Atmosphere.z;
    } else if(Atmosphere.w<1.5) {
        float nebula=smoothstep(0.25,0.72,n)*smoothstep(-0.4,0.2,d.y);
        c+=mix(SkyFirst.rgb,SkySecond.rgb,ambFbm(d*7.0+vec3(t*0.04)))*nebula*Atmosphere.z;
    } else if(Atmosphere.w<2.5) {
        float sunset=exp(-abs(d.y-0.02)/(0.12+Detail.x*0.25));
        c=mix(base,mix(SkySecond.rgb,SkyFirst.rgb,sunset),0.55*Atmosphere.z);
        c+=vec3(1.0,0.65,0.28)*pow(max(dot(d,normalize(vec3(0.3,0.12,1))),0.0),300.0)*Atmosphere.z;
    } else if(Atmosphere.w<3.5) {
        c=mix(vec3(0.06,0.16,0.31),SkySecond.rgb*0.5,h);
        float cloud=smoothstep(0.42-Detail.x*0.1,0.65,n+max(d.y,0.0)*0.15);
        c=mix(c,mix(vec3(0.4),SkyFirst.rgb,0.45),cloud*Atmosphere.z);
    } else {
        c=mix(vec3(0.018,0.028,0.055),SkySecond.rgb*0.13,n);
        float flash=pow(max(sin(t*1.7+sin(t*0.23)*4.0),0.0),70.0);
        c+=mix(vec3(0.20),SkyFirst.rgb,0.4)*n*0.22*Atmosphere.z+vec3(0.55,0.65,0.85)*flash*n;
    }
    if(Detail.y>0.5 && Atmosphere.w<2.5) {
        vec3 cell=floor(d*420.0); float star=step(0.9982,ambHash(cell));
        float point=pow(max(0.0,1.0-length(fract(d*420.0)-0.5)*2.0),3.0);
        c+=vec3(star*point*smoothstep(0.02,0.25,d.y)*(0.7+0.3*sin(t+ambHash(cell)*30.0)));
    }
    return c;
}
bool ambInSprite(vec2 uv, vec4 r) { return all(greaterThanEqual(uv,r.xy)) && all(lessThanEqual(uv,r.zw)); }
vec4 ambWater(vec4 color,vec2 uv,vec3 relativePosition) {
    if(!ambInSprite(uv,WaterStill) && !ambInSprite(uv,WaterFlow) && !ambInSprite(uv,WaterOverlay)) return color;
    vec3 p=relativePosition+CameraPosition.xyz;
    float t=Atmosphere.x*Waves.y;
    vec3 normal=normalize(cross(dFdx(relativePosition),dFdy(relativePosition))); if(normal.y<0.0) normal=-normal;
    float surface=abs(normal.y);
    vec2 ripple=vec2(cos(p.x*0.45+p.z*0.27+t*1.2),sin(p.z*0.57-p.x*0.21+t*0.9))*0.35;
    ripple+=vec2(cos(p.x*2.3-p.z*1.7+t*2.1),sin(p.z*2.7+p.x*1.4+t*1.8))*0.08;
    normal=normalize(normal+vec3(ripple.x,0,ripple.y)*Waves.x*surface);
    vec3 eye=normalize(-relativePosition); float fresnel=0.025+0.975*pow(1.0-abs(dot(normal,eye)),5.0);
    float opticalDepth=1.0-exp(-length(relativePosition)*0.025-(1.0-surface)*0.6-CameraPosition.w*0.7);
    vec3 tint=mix(WaterShallow.rgb,WaterDeep.rgb,opticalDepth);
    vec3 reflected=Detail.w>0.5 ? ambSky(reflect(-eye,normal)) : mix(SkySecond.rgb*0.18,SkyFirst.rgb*0.45,normal.y*0.5+0.5);
    vec3 lit=tint*(0.55+0.45*dot(color.rgb,vec3(0.333)))+color.rgb*0.25;
    color.rgb=mix(lit,reflected,clamp(Waves.w*(0.18+fresnel)*surface,0.0,1.0));
    color.a*=1.0-Waves.z;
    return color;
}
