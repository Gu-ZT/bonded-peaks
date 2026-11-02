#version 150
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
in vec3 Position;
in vec2 UV0;
in vec4 Color;
out vec2 point;
flat out int icon;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    point = UV0 * 24.0;
    icon = int(floor(Color.r * 255.0 + 0.5));
}
