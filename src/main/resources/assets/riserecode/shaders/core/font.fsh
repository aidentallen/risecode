#version 330
uniform sampler2D Sampler0;
in vec2 local;
// Keep the complete ui.vsh interface: Vulkan assigns consecutive varying locations.
in vec2 screenUV;
in vec4 tint;
flat in vec4 data;
out vec4 fragColor;
// STB stores 128 at the outline and 2.5 intensity units per baked pixel; density is two.
void main() {
    // Qraftys body ink occupies texels 48..63 of each 128-texel cell. Retain
    // a two-texel guard and the original quad/UV interpolation, so fractional
    // nearest-sampling boundaries stay identical while empty padding costs no blend.
    if (data.y == 2.0) {
        vec2 cell = fract(local * 16.0) * 128.0;
        if (any(lessThan(cell, vec2(46.0 - data.z / 128.0))) || any(greaterThan(cell, vec2(66.0 + data.z / 128.0)))) { discard; }
    }
    float sampleValue = texture(Sampler0, local).r;
    // Normal TrueType text is filled 8x raster coverage, never a thresholded low-res SDF.
    if (data.y == 3.0 || data.y == 4.0) {
        float coverage;
        if (data.y == 4.0) { coverage = step(0.5, sampleValue); }
        else {
            // Integrate the screen-pixel footprint when reducing the oversampled atlas.
            // Four bilinear taps keep diagonal stems smooth without a post-render blur.
            vec2 dx = dFdx(local) * 0.25, dy = dFdy(local) * 0.25;
            coverage = 0.25 * (texture(Sampler0, local - dx - dy).r
                    + texture(Sampler0, local + dx - dy).r
                    + texture(Sampler0, local - dx + dy).r
                    + texture(Sampler0, local + dx + dy).r);
        }
        fragColor = vec4(tint.rgb, tint.a * coverage);
        return;
    }
    float distance = (sampleValue * 255.0 - 128.0) / 5.0 + data.z / 256.0;
    float coverage;
    if (data.y == 1.0 && data.x > 0.0) {
        float sigma = max(data.x / 256.0, 0.01);
        // Abramowitz-Stegun erf approximation: analytical Gaussian silhouette shadow.
        float z = abs(distance) / (sigma * 1.41421356237);
        float t = 1.0 / (1.0 + 0.3275911 * z);
        float erf = 1.0 - (((((1.061405429*t - 1.453152027)*t) + 1.421413741)*t - 0.284496736)*t + 0.254829592)*t*exp(-z*z);
        coverage = 0.5 + 0.5 * sign(distance) * erf;
    } else if (data.y == 2.0) { coverage = step(0.0, distance); }
    else { float aa = max(fwidth(distance), 0.001); coverage = smoothstep(-aa * 0.5, aa * 0.5, distance); }
    fragColor = vec4(tint.rgb, tint.a * coverage);
}
