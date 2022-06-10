in vec3 a_pos;

out vec2 v_uv;
out vec2 v_texelUV;

uniform vec2 u_sizeInv;

void main() {
    gl_Position = vec4(a_pos, 1.0);
    v_uv = a_pos.xy * .5 + .5;
    v_texelUV = v_uv * u_sizeInv;
}