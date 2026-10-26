#version 330
uniform sampler2D Sampler0;
in vec2 local;
in vec2 screenUV;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
void main() {
    vec2 halfSize = data.xy / 32.0;
    float radius = min(data.z / 256.0, min(halfSize.x, halfSize.y));
    vec2 q = abs(local) - halfSize + radius;
    float distance = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(distance), 0.001);
    float coverage = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, distance);
    fragColor = vec4(texture(Sampler0, screenUV).rgb, tint.a * coverage);
}
