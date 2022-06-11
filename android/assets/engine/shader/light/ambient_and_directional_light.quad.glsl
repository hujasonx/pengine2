#include <engine/shader/header/shared.frag>
#include <engine/shader/header/light.frag>

void main() {
    #include <engine/shader/start/shared.frag>
    #include <engine/shader/start/light.frag>

    lighted = diffuseM;

    #include <engine/shader/end/light.frag>
    #include <engine/shader/end/shared.frag>
}