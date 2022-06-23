#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/rendercontext.frag>
#include <engine/shader/header/pbr.frag>

in vec4 v_diffuseM;
in vec4 v_emissiveR;

#include <engine/shader/header/texture2D>[vColIndex]

void main() {
    #include <engine/shader/start/instanced.frag>
    #include <engine/shader/start/rendercontext.frag>
    #include <engine/shader/start/pbr.frag>

    diffuseM *= v_diffuseM;
    emissiveI.rgb += v_emissiveR.rgb;
    //    normalR.w *= v_emissiveR.w;

    #include <engine/shader/end/pbr.frag>
    #include <engine/shader/end/rendercontext.frag>
    #include <engine/shader/end/instanced.frag>
}