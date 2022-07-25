#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[source]
#include <engine/shader/header/texture2D>[bloom]
void main() {
    #include <engine/shader/start/shared.frag>

    fxaa = sourceTex(bufferUV) + bloomTex(bufferUV);

    #include <engine/shader/end/shared.frag>
}