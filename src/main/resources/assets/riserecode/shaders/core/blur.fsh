#version 330
uniform sampler2D Sampler0;
layout(std140) uniform BlurParams { vec4 Params; };
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 color;
    if (Params.z == 0.0) {
        vec2 halfTexel = 0.5 / vec2(textureSize(Sampler0, 0));
        color = (texture(Sampler0, texCoord + halfTexel).rgb + texture(Sampler0, texCoord - halfTexel).rgb
               + texture(Sampler0, texCoord + vec2(halfTexel.x, -halfTexel.y)).rgb
               + texture(Sampler0, texCoord + vec2(-halfTexel.x, halfTexel.y)).rgb) * 0.25;
    } else if (Params.z == 1.0) {
        // Nine-tap separable Gaussian collapsed to five bilinear texture fetches.
        color = texture(Sampler0, texCoord).rgb * 0.227027027;
        color += (texture(Sampler0, texCoord + Params.xy * 1.384615385).rgb
                + texture(Sampler0, texCoord - Params.xy * 1.384615385).rgb) * 0.316216216;
        color += (texture(Sampler0, texCoord + Params.xy * 3.230769231).rgb
                + texture(Sampler0, texCoord - Params.xy * 3.230769231).rgb) * 0.070270270;
    } else { color = texture(Sampler0, texCoord).rgb; }
    fragColor = vec4(color, Params.w);
}
