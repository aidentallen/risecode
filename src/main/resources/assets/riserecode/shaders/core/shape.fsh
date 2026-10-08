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
    float coverage;
    if (data.w < 0.0) {
        float sigma = max((-data.w - 1.0) / 768.0, 0.001);
        coverage = exp(-pow(max(distance, 0.0), 2.0) / (2.0 * sigma * sigma));
    } else {
        coverage = 1.0 - smoothstep(-aa * 0.5, aa * 0.5, distance);
        if (data.w > 0.0) {
            float thickness = data.w / 256.0;
            coverage *= smoothstep(-thickness - aa * 0.5, -thickness + aa * 0.5, distance);
        }
    }
    fragColor = vec4(tint.rgb, tint.a * coverage);
}
