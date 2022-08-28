#include <engine/shader/header/shared>

void main() {
    #include <engine/shader/start/shared.frag>

    color = vec4(1.0);

    #include <engine/shader/end/shared.frag>
}
