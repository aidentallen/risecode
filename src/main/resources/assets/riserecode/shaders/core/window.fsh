#version 330
in vec2 local;
// Keep the complete ui.vsh interface: Vulkan assigns consecutive varying locations.
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
    vec2 uv = (local + halfSize) / (halfSize * 2.0);
    vec3 color = local.x + halfSize.x < 107.0
        ? mix(vec3(23,31,34), vec3(20,28,30), uv.y) / 255.0
        : mix(vec3(30,34,44), vec3(24,31,37), (uv.x + uv.y) * 0.5) / 255.0;
    fragColor = vec4(color, tint.a * (1.0 - smoothstep(-aa * 0.5, aa * 0.5, distance)));
}
