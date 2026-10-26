#version 330
in vec2 local;
in vec4 tint;
out vec4 fragColor;
void main(){float a=1.-smoothstep(.3,1.,length(local));fragColor=vec4(tint.rgb,tint.a*a);}
