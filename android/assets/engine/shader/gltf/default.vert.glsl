#include <engine/shader/bootstrap.vert>

void main() {
    #include <engine/shader/bootstrap.main.start.vert>

    gl_Position = vec4(v_worldPos.xy, 0.0, 1.0);
}