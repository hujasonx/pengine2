#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/rendercontext.frag>
#include <engine/shader/header/pbr.frag>

#include <engine/shader/header/texture2D>[test]

void main() {
    #include <engine/shader/start/instanced.frag>
    #include <engine/shader/start/rendercontext.frag>
    #include <engine/shader/start/pbr.frag>

    //    diffuseM = boneTransformsVec4(2);

    #ifdef a_bon0Flag
    //    int indexToLook = int(mod(u_tdtuituidt.x * 12.0, 64.0)) + 2;
    //    diffuseM = boneTransformsVec4I(indexToLook);
    //    diffuseM.r = diffuseM.a;
    //    diffuseM.g = float(indexToLook) * .02;

    //    diffuseM.rgb = diffuseTex(v_uv0);
    #endif


    #include <engine/shader/end/pbr.frag>
    #include <engine/shader/end/rendercontext.frag>
    #include <engine/shader/end/instanced.frag>
}