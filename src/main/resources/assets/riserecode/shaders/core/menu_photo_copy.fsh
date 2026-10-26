#version 330
uniform sampler2D Sampler0;
layout(std140) uniform BlurParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
void main() { vec2 uv=vec2(texCoord.x,1.0-texCoord.y)*Params.xy+Params.zw;fragColor=any(lessThan(uv,vec2(0)))||any(greaterThan(uv,vec2(1)))?vec4(0,0,0,1):texture(Sampler0,uv); }
