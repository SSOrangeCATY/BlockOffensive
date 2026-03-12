#version 150

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;

uniform mat4 ProjMat;

void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    texCoord0 = UV0;
}
