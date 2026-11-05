#version 330
#moj_import <riserecode:ui_glass.glsl>
uniform sampler2D Sampler0;
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
    float refraction = mod(data.w - 1.0, 101.0) / 50.0;
    float bevel = min(12.0, min(halfSize.x, halfSize.y) * 0.55);
    float edge = 1.0 - smoothstep(0.0, max(bevel, 0.01), max(-distance, 0.0));
    // Convert local displacement through the actual pose, including opening/press scaling.
    mat2 jacobian = mat2(dFdx(local), dFdy(local));
    vec2 displacement = normal * min(8.0, bevel * 0.45 * refraction) * edge;
    vec2 pixelOffset = inverse(jacobian) * displacement;
    vec2 uvOffset = dFdx(screenUV) * pixelOffset.x + dFdy(screenUV) * pixelOffset.y;
    vec2 texel = 0.5 / vec2(textureSize(Sampler0, 0));
    vec3 behind = texture(Sampler0, clamp(screenUV - uvOffset, texel, 1.0 - texel)).rgb * tint.rgb;
    // Local luminosity adaptation is entirely on GPU, with no framebuffer readback.
    float luma = dot(behind, vec3(0.2126, 0.7152, 0.0722));
    vec3 body = mix(behind, vec3(0.055, 0.075, 0.095), 0.22 + 0.68 * smoothstep(0.18, 0.85, luma));
    fragColor = vec4(body, tint.a * glassCoverage(distance));
}
