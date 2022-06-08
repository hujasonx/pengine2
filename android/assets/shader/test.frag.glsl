#version 330

layout(location = 0) out vec4 o_dif;

in vec3 v_pos;

void main() {
    o_dif = vec4(v_pos, 1.0);
}