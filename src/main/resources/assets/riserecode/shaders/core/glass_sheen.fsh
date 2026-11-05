#version 330
#moj_import <riserecode:ui_glass.glsl>
in vec2 local;
in vec2 screenUV;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
void main() {
    vec2 halfSize = data.xy / 32.0;
    float radius = min(data.z / 256.0, min(halfSize.x, halfSize.y));
    float distance = glassDistance(local, halfSize, radius);
    vec2 normal = glassNormal(local, halfSize, radius);
    float highlight = floor((data.w - 1.0) / 101.0) / 100.0;
    float light = 0.22 + 0.78 * pow(abs(dot(normal, normalize(vec2(-0.55, -0.83)))), 3.0);
    float rim = exp(-abs(distance + 0.55) * 1.7) * light;
    float wash = 0.035 * (1.0 - smoothstep(-halfSize.y, halfSize.y, local.y));
    fragColor = vec4(mix(vec3(0.76, 0.88, 1.0), vec3(1.0), light),
            tint.a * highlight * (rim * 0.58 + wash) * glassCoverage(distance));
}
