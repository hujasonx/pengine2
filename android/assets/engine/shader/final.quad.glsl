#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[source]
#include <engine/shader/header/texture2D>[outline]
#include <engine/shader/header/texture2D>[bloom]
void main() {
    #include <engine/shader/start/shared.frag>

    fxaa = sourceTex(bufferUV) + bloomTex(bufferUV);
    vec4 outlineCol = outlineTex(bufferUV);
    fxaa = mix(fxaa, outlineCol, outlineCol.a);

    #include <engine/shader/end/shared.frag>
}