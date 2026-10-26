#version 330
in vec2 local;
// Keep the complete ui.vsh interface: Vulkan assigns consecutive varying locations.
in vec2 screenUV;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
vec3 hsv(float h, float s, float v) {
    vec3 p = abs(fract(vec3(h) + vec3(0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);
    return v * mix(vec3(1), clamp(p - 1.0, 0.0, 1.0), s);
}
void main() {
    vec3 color = data.y == 1.0 ? hsv(local.y, 1.0, 1.0) : hsv(data.x / 30000.0, local.x, 1.0 - local.y);
    fragColor = vec4(color, tint.a);
}
