#version 330
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
layout(std140) uniform OutlineParams {
    vec4 Metrics; // texel x/y, width and outer glow in framebuffer pixels
    vec4 Primary;
    vec4 Secondary;
    vec4 Policy; // color mode, direction, speed, spacing
    vec4 Frame; // continuous time, intensity, pass, GUI scale
};
in vec2 texCoord;
out vec4 fragColor;
vec3 hsv(float h, float s) {
    vec3 rgb = clamp(abs(fract(h + vec3(0.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0) - 1.0, 0.0, 1.0);
    return mix(vec3(1.0), rgb, s);
}
vec4 colorAt() {
    vec2 pixels = vec2(texCoord.x, 1.0 - texCoord.y) / Metrics.xy / Frame.w;
    float offset = Policy.y < 0.5 ? pixels.x : pixels.y;
    float phase = fract(offset / Policy.w + Frame.x * Policy.z);
    if (Policy.x < 0.5) return Primary;
    if (Policy.x < 1.5) return mix(Primary, Secondary, 1.0 - abs(phase * 2.0 - 1.0));
    if (Policy.x < 2.5) return vec4(hsv(phase, 0.65), Primary.a);
    if (Policy.x < 3.5) return vec4(hsv(0.5 + 0.35 * (0.5 + 0.5 * sin(phase * 6.2831853)), 0.45), Primary.a);
    return mix(Primary, Secondary, 0.5 + 0.5 * sin(phase * 6.2831853));
}
void main() {
    bool horizontal = Frame.z < 0.5;
    // Interior rejection precedes dilation: there is NEVER a colored item-face overlay.
    if (!horizontal && texture(Sampler1, texCoord).a > 0.05) {
        fragColor = vec4(0.0); return;
    }
    float border = 0.0, glow = 0.0;
    int radius = min(80,int(ceil(Metrics.z + Metrics.w)));
    for (int i = -radius; i <= radius; i++) {
        if (abs(i) > radius) continue;
        vec2 stepUV = horizontal ? vec2(Metrics.x * float(i), 0.0) : vec2(0.0, Metrics.y * float(i));
        vec4 sampleColor = texture(Sampler0, texCoord + stepUV);
        float coverage = horizontal ? sampleColor.a : sampleColor.r;
        float soft = horizontal ? sampleColor.a : sampleColor.g;
        float distance = abs(float(i));
        float edgeWeight = clamp(Metrics.z + 0.5 - distance, 0.0, 1.0);
        border = max(border, coverage * edgeWeight);
        if (Metrics.w > 0.0) {
            float outer = max(0.0, distance - Metrics.z);
            glow = max(glow, soft * exp(-3.0 * outer * outer / (Metrics.w * Metrics.w)));
        }
    }
    if (horizontal) { fragColor = vec4(border, glow, 0.0, 1.0); return; }
    vec4 color = colorAt();
    fragColor = vec4(color.rgb, color.a * Frame.y * max(border, glow * 0.28));
}
