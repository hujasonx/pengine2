#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/rendercontext.frag>
#include <engine/shader/header/pbr.frag>

void main() {
    #include <engine/shader/start/instanced.frag>
    #include <engine/shader/start/rendercontext.frag>
    #include <engine/shader/start/pbr.frag>

    alphaBlend = vec4(diffuseM.rgb * diffuseM.a + emissiveI.rgb * emissiveI.a, diffuseM.a);
    // Reset the non-alphaBlend components.
    diffuseM = vec4(0.0);
    normalR = vec4(0.0);
    emissiveI = vec4(0.0);

    #include <engine/shader/end/pbr.frag>
    #include <engine/shader/end/rendercontext.frag>
    #include <engine/shader/end/instanced.frag>
}