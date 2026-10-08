#version 330
uniform sampler2D Sampler0;
layout(std140) uniform BlurParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
void main() { fragColor = texture(Sampler0, vec2(texCoord.x, 1.0 - texCoord.y) * Params.xy + Params.zw); }
