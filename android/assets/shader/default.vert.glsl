#version 330

in vec3 a_pos;
in vec3 a_nor;
in vec2 a_uv0;
in vec4 a_col0;

out vec3 v_pos;
out vec3 v_nor;
out vec2 v_uv0;
out vec4 v_col0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat4 u_worldTransInvTrans;

void main() {
    v_pos = a_pos;
    v_nor = a_nor;
    v_uv0 = a_uv0;
    v_col0 = a_col0;

    gl_Position = u_projViewTrans * u_worldTrans * vec4(a_pos, 1.0);
}