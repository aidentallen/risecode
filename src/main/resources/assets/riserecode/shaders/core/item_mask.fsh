#version 330
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;
void main() {
    // The atlas's alpha defines the union silhouette; texture RGB never becomes an edge.
    if (texture(Sampler0, texCoord).a < 0.1) discard;
    fragColor = vec4(1.0);
}
