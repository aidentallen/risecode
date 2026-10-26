#version 330
uniform sampler2D Sampler0;
layout(std140) uniform ScaleParams { vec4 Extents; vec4 Options; };
in vec2 texCoord;
out vec4 fragColor;
void main() { fragColor = vec4(texture(Sampler0, gl_FragCoord.xy / Extents.zw).rgb, 1.0); }
