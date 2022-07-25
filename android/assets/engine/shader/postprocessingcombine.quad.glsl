#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[lighted]
#include <engine/shader/header/texture2D>[alphaBlend]
void main() {
    #include <engine/shader/start/shared.frag>

    vec4 alphaBlendIn = alphaBlendTex(bufferUV);
    combine = vec4(mix(lightedTex(bufferUV).rgb, alphaBlendIn.rgb, clamp(alphaBlendIn.a, 0.0, 1.0)), 1.0);
    //    final = vec4(lightedTex(bufferUV).rgb + alphaBlendIn.a * alphaBlendIn.rgb, 1.0);
    //    final.rgb = vec3(alphaBlendIn.a);

    #include <engine/shader/end/shared.frag>
}