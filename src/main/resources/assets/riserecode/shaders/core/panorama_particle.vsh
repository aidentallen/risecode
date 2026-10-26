#version 330
#moj_import <riserecode:panorama.glsl>
out vec2 local;
out vec4 tint;
void main(){
    int id=gl_VertexID/6;int vertex=gl_VertexID%6;vec2 corners[6]=vec2[6](vec2(-1,-1),vec2(-1,1),vec2(1,1),vec2(-1,-1),vec2(1,1),vec2(1,-1));
    local=corners[vertex];float seed=float(id);float h=hashScene(vec2(seed,3.));float j=hashScene(vec2(seed,7.));float t=EyeTime.w;
    vec3 p;float size;float stretch=1.;
    if(id<96){
        p=vec3((h-.5)*43.,1.2+hashScene(vec2(seed,9.))*3.,-j*48.+12.);p+=vec3(sin(t*.6+seed),cos(t*.4+seed)*.4,sin(t*.3+seed)*.7);
        vec2 away=p.xz-Cursor.xy;float d=length(away);p.xz+=away/max(.1,d)*exp(-d*d*.08)*Cursor.z*3.;
        size=.035+.025*sin(t*1.7+seed);tint=vec4(1.,.91,.34,Camera.w*(.55+.45*sin(seed+t*1.5)));
    }else if(id<128){
        p=vec3((h-.5)*55.,18.-mod(t*(.5+j*.5)+seed*3.,18.),-j*48.+12.);p.x=mod(p.x+Environment.x*t*.16+27.5,55.)-27.5+sin(p.y*.45+seed)*2.;
        size=.07;tint=vec4(.57,.47,.15,.65);stretch=.65;
    }else{
        p=vec3((h-.5)*64.,20.-mod(t*(12.+j*7.)+seed,20.),-j*65.+19.);p.x+=Environment.x*p.y*.2;
        size=.014;stretch=22.;tint=vec4(.7,.8,.92,Environment.y*.35);
    }
    vec4 clip=SceneMatrix*vec4(p,1.);clip.xy+=local*vec2(size,size*stretch)*vec2(1.,Options.x);gl_Position=clip;
}
