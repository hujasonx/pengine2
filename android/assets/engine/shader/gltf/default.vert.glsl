#include <engine/shader/bootstrap.vert>

void main() {
    v_pos = a_pos;
    gl_Position = vec4(a_pos, 1.0);
}