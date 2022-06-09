#include <engine/shader/header/bootstrap.frag>

void main() {
    #include <engine/shader/main/start/bootstrap.frag>
    o_dif = vec4(nor, 1.0);

    #include <engine/shader/main/end/bootstrap.frag>
}