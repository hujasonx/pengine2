#version 330

layout(location = 0) out vec4 o_dif;

in vec3 v_pos;
in vec3 v_nor;
in vec2 v_uv0;
in vec4 v_col0;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat4 u_worldTransInvTrans;

void main() {
    o_dif = v_col0;
}