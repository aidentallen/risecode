layout(std140) uniform Scene {
    mat4 SceneMatrix;
    vec4 EyeTime;
    vec4 Environment;
    vec4 SkyColor;
    vec4 HorizonColor;
    vec4 WaterColor;
    vec4 Options;
    vec4 Camera;
    vec4 Cursor;
    vec4 Gusts[8];
    vec4 Ripples[12];
};
float hashScene(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
vec3 sceneSky(vec3 ray){
    vec3 color=mix(HorizonColor.rgb,SkyColor.rgb,smoothstep(-.06,.8,ray.y));
    float angle=(Camera.z-6.)*.261799;
    vec3 sun=normalize(vec3(-.5,sin(angle),-.7));
    float glow=pow(max(0.,dot(ray,sun)),90.);
    color+=vec3(1.,.65,.28)*glow*Environment.w*.7;
    if(Camera.w>.25){
        vec2 star=ray.xz/max(.1,ray.y)*70.;vec2 cell=floor(star);
        float point=1.-smoothstep(.015,.075,length(fract(star)-.5));
        color+=vec3(.65,.78,1.)*point*step(.985,hashScene(cell))*Camera.w;
        float aurora=pow(max(0.,sin(ray.x*9.+sin(ray.z*5.+EyeTime.w*.025)*2.+EyeTime.w*.035)),8.);
        float midnight=clamp(smoothstep(20.,23.,Camera.z)+1.-smoothstep(1.,4.,Camera.z),0.,1.);
        color+=vec3(.08,.45,.26)*midnight*aurora*smoothstep(.08,.45,ray.y)*(1.-smoothstep(.5,.95,ray.y))*Camera.w*.55;
        float t=mod(EyeTime.w,97.);float streak=max(0.,1.-abs(ray.y-(.56-ray.x*.32))/.003)*smoothstep(48.,48.12,t)*(1.-smoothstep(48.3,48.65,t));
        color+=vec3(.7,.8,1.)*streak*exp(-pow((ray.x-(t-48.)*2.+.4)*9.,2.))*Camera.w;
    }
    return color;
}
