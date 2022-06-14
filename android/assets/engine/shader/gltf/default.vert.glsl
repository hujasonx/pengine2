#include <engine/shader/header/instanced.vert>
#include <engine/shader/header/rendercontext.vert>
#include <engine/shader/header/pbr.vert>

void main() {
    #include <engine/shader/start/instanced.vert>
    #include <engine/shader/start/rendercontext.vert>
    #include <engine/shader/start/pbr.vert>

    //    worldPos = a_pos * 5.0;
    //    worldNor = a_nor;

    //    int indexToLook = int(mod(u_tdtuituidt.x * 4.0, 57.0));
    //    mat4 tempM = boneTransformsMat4I(indexToLook * 4);
    //    worldPos = (tempM * vec4(a_pos, 1.0)).xyz;
    //    worldNor = (tempM * vec4(a_nor, 0.0)).xyz;

    #include <engine/shader/end/pbr.vert>
    #include <engine/shader/end/rendercontext.vert>
    #include <engine/shader/end/instanced.vert>
}