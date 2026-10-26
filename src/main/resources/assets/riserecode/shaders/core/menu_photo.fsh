#version 330
uniform sampler2D Sampler0;
in vec2 local;
// Keep the complete ui.vsh interface: Vulkan assigns consecutive varying locations.
in vec2 screenUV;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
void main() { fragColor = texture(Sampler0, vec2(local.x, 1.0 - local.y)) * tint; }
