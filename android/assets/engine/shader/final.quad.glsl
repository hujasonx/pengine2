#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[lighted]
#include <engine/shader/header/texture2D>[alphaBlend]
#include <engine/shader/header/texture2D>[outline]
#include <engine/shader/header/texture2D>[bloom]
void main() {
    #include <engine/shader/start/shared.frag>

    vec4 outlineCol = outlineTex(bufferUV);
    fxaa = mix(lightedTex(bufferUV), outlineCol, outlineCol.a);
    fxaa += bloomTex(bufferUV) + alphaBlendTex(bufferUV);

    #include <engine/shader/end/shared.frag>
}