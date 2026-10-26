#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 texCoord;
out vec4 tint;
out vec3 viewPosition;
void main() {
    vec4 p = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * p;
    viewPosition = p.xyz;
    texCoord = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
    tint = Color * ColorModulator;
}
