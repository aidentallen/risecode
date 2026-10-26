#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
in vec3 Position; in vec2 UV0; in vec4 Shape; in vec4 First; in vec4 Second; in vec4 Policy;
in vec4 NextFirst; in vec4 NextSecond; in vec4 NextPolicy; in vec4 MixInfo;
out vec2 local; out vec2 screenPosition;
flat out vec4 shape; flat out vec4 first; flat out vec4 second; flat out vec4 policy;
flat out vec4 nextFirst; flat out vec4 nextSecond; flat out vec4 nextPolicy; flat out vec4 info;
void main(){ gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0); local=UV0; screenPosition=Position.xy;
shape=Shape;first=First;second=Second;policy=Policy;nextFirst=NextFirst;nextSecond=NextSecond;nextPolicy=NextPolicy;info=MixInfo; }
