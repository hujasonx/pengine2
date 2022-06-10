#include <engine/shader/header/light.vert>
#include <engine/shader/header/instanced.vert>

void main() {
    #include <engine/shader/main/start/instanced.vert>
    #include <engine/shader/main/start/light.vert>

    #include <engine/shader/main/end/light.vert>
}