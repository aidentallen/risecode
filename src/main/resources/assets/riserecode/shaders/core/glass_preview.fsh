#version 330
in vec2 texCoord;
out vec4 fragColor;
void main(){
    vec2 p=gl_FragCoord.xy;
    float checker=mod(floor(p.x/24.0)+floor(p.y/24.0),2.0);
    vec3 c=mix(vec3(0.055,0.085,0.14),vec3(0.14,0.22,0.32),checker);
    float stripe=1.0-smoothstep(1.0,3.0,abs(mod(p.x+p.y*.3,110.0)-55.0));
    c+=vec3(0.1,0.3,0.4)*stripe;
    fragColor=vec4(c,1.0);
}
