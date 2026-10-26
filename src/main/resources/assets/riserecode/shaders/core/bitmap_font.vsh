#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec4 Clip;
in vec2 UV0;
in vec4 Color;
in ivec2 UV1;
in ivec2 UV2;
out vec2 local;
out vec2 screenUV;
out vec4 tint;
flat out vec4 data;
flat out vec4 clipBounds;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    local = UV0;
    screenUV = gl_Position.xy / gl_Position.w * 0.5 + 0.5;
    tint = Color;
    data = vec4(UV1, UV2);
    clipBounds = Clip;
}
