#version 330

in vec3 a_pos;

out vec3 v_pos;

void main() {
    v_pos = a_pos;
    gl_Position = vec4(a_pos, 1.0);
}