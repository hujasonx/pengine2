#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[lighted]
#include <engine/shader/header/texture2D>[alphaBlend]
uniform float u_bloomThreshold;
uniform float u_bloomScale;
void main() {
    #include <engine/shader/start/shared.frag>

    vec4 lighted = lightedTex(bufferUV) + alphaBlendTex(bufferUV);
    float value = max(0.0, grayScale(lighted.rgb) - u_bloomThreshold) * u_bloomScale;
    bloom = vec4(lighted.rgb * value, 1.);

    #include <engine/shader/end/shared.frag>
}