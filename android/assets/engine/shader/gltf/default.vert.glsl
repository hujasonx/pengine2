#include <engine/shader/header/shared.vert>
#include <engine/shader/header/rendercontext.vert>
#include <engine/shader/header/pbr.vert>

void main() {
    #include <engine/shader/start/shared.vert>
    #include <engine/shader/start/rendercontext.vert>
    #include <engine/shader/start/pbr.vert>

    #include <engine/shader/end/pbr.vert>
    #include <engine/shader/end/rendercontext.vert>
    #include <engine/shader/end/shared.vert>
}