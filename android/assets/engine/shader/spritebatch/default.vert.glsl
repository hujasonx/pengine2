#include <engine/shader/header/shared>

in vec2 a_pos;
uniform mat4 u_viewProjTransform;

void main() {

    gl_Position = u_viewProjTransform * vec4(a_pos, 0.0, 1.0);

}