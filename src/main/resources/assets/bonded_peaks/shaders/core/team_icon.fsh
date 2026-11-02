#version 150
in vec2 point;
flat in int icon;
out vec4 fragColor;
float segment(vec2 p, vec2 a, vec2 b, float radius) {
    vec2 v = b - a;
    return length(p - a - v * clamp(dot(p - a, v) / dot(v, v), 0.0, 1.0)) - radius;
}
float box(vec2 p, vec2 center, vec2 halfSize, float radius) {
    vec2 q = abs(p - center) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}
float peak(vec2 p, vec2 center, float size) {
    p = (p - center) / size;
    vec2 a = vec2(0.0, -1.0), b = vec2(0.8660254, 0.5), c = vec2(-0.8660254, 0.5);
    float distance = min(segment(p, a, b, 0.0), min(segment(p, b, c, 0.0), segment(p, c, a, 0.0)));
    bool inside = ((b.x-a.x)*(p.y-a.y)-(b.y-a.y)*(p.x-a.x) >= 0.0)
               && ((c.x-b.x)*(p.y-b.y)-(c.y-b.y)*(p.x-b.x) >= 0.0)
               && ((a.x-c.x)*(p.y-c.y)-(a.y-c.y)*(p.x-c.x) >= 0.0);
    return (inside ? -distance : distance) * size;
}
float shape(vec2 p) {
    if (icon == 0) {
        return min(peak(p, vec2(8,14), 6.0), peak(p, vec2(16,11), 8.0));
    }
    if (icon == 1) {
        return min(length(p-vec2(12,7))-3.5, box(p,vec2(12,17),vec2(6.5,3.5),3.0));
    }
    if (icon == 2) {
        float d = segment(p,vec2(5,17),vec2(3,7),1.5);
        d=min(d,segment(p,vec2(3,7),vec2(8,11),1.5));
        d=min(d,segment(p,vec2(8,11),vec2(12,4),1.5));
        d=min(d,segment(p,vec2(12,4),vec2(16,11),1.5));
        d=min(d,segment(p,vec2(16,11),vec2(21,7),1.5));
        d=min(d,segment(p,vec2(21,7),vec2(19,17),1.5));
        return min(d,box(p,vec2(12,18),vec2(7.5,2),1.0));
    }
    if (icon == 3) {
        return min(box(p,vec2(12,10),vec2(8.5,6),3.0),segment(p,vec2(7,14),vec2(5,19),1.5));
    }
    if (icon == 4) {
        return min(segment(p,vec2(12,5),vec2(12,19),1.5),segment(p,vec2(5,12),vec2(19,12),1.5));
    }
    if (icon == 5 || icon == 6) {
        float d=min(segment(p,vec2(5,12),vec2(19,12),1.5),segment(p,vec2(14,6),vec2(20,12),1.5));
        d=min(d,segment(p,vec2(20,12),vec2(14,18),1.5));
        return icon == 6 ? min(d,segment(p,vec2(5,4),vec2(5,20),1.5)) : d;
    }
    if (icon == 7) {
        return min(segment(p,vec2(6,6),vec2(18,18),1.5),segment(p,vec2(18,6),vec2(6,18),1.5));
    }
    if (icon == 8) {
        return min(segment(p,vec2(5,12),vec2(10,17),1.5),segment(p,vec2(10,17),vec2(20,6),1.5));
    }
    return segment(p,vec2(5,12),vec2(19,12),1.5);
}
void main() {
    float d = shape(point);
    float aa = max(fwidth(d), 0.01);
    float alpha = 1.0 - smoothstep(1.25-aa, 1.25+aa, d);
    float fill = 1.0 - smoothstep(-aa, aa, d);
    fragColor = vec4(vec3(fill), alpha);
}
