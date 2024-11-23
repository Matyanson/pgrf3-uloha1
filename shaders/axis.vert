#version 330

in vec3 inPosition;
in vec3 inColor;

uniform mat4 uView;
uniform mat4 uProj;

out vec3 color;

void main() {
    color = inColor;

    gl_Position = uProj * uView * vec4(inPosition, 1);
}
