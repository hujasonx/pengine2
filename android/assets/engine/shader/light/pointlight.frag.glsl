#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/light.frag>

void main() {
    #include <engine/shader/main/start/instanced.frag>
    #include <engine/shader/main/start/light.frag>

    #include <engine/shader/main/end/light.frag>
}