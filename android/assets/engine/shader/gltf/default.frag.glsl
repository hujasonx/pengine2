#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/rendercontext.frag>
#include <engine/shader/header/pbr.frag>

void main() {
    #include <engine/shader/start/instanced.frag>
    #include <engine/shader/start/rendercontext.frag>
    #include <engine/shader/start/pbr.frag>

    diffuseM.rgb += (v_worldNor * 0.5 + vec3(0.5));

    #include <engine/shader/end/pbr.frag>
    #include <engine/shader/end/rendercontext.frag>
    #include <engine/shader/end/instanced.frag>
}