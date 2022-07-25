#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[source]
#include <engine/shader/header/texture2D>[outline]
#include <engine/shader/header/texture2D>[bloom]
void main() {
    #include <engine/shader/start/shared.frag>

    vec4 outlineCol = outlineTex(bufferUV);
    fxaa = mix(sourceTex(bufferUV), outlineCol, outlineCol.a) + bloomTex(bufferUV);

    #include <engine/shader/end/shared.frag>
}