#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[source]
uniform float u_bloomThreshold;
uniform float u_bloomScale;
void main() {
    #include <engine/shader/start/shared.frag>

    vec4 source = sourceTex(bufferUV);
    float value = max(0.0, grayScale(source.rgb) - u_bloomThreshold) * u_bloomScale;
    bloom = vec4(source.rgb * value, 1.);

    #include <engine/shader/end/shared.frag>
}