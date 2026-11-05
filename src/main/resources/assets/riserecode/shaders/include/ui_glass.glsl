// Original screen-space UI material. Curvature comes from the rounded-rectangle SDF.
float glassDistance(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}
vec2 glassNormal(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    vec2 corner = max(q, 0.0);
    if (dot(corner, corner) > 0.00001) return normalize(corner) * sign(p);
    return q.x > q.y ? vec2(sign(p.x), 0.0) : vec2(0.0, sign(p.y));
}
float glassCoverage(float distance) {
    float aa = max(fwidth(distance), 0.001);
    return 1.0 - smoothstep(-aa * 0.5, aa * 0.5, distance);
}
