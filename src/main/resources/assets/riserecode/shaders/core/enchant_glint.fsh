#version 330
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;
void main() {
    vec3 pattern = texture(Sampler0, texCoord).rgb;
    float light = max(pattern.r, max(pattern.g, pattern.b));
    fragColor = vec4(tint.rgb * light, tint.a * light);
}
