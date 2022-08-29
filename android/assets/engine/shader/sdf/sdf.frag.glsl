#params [OUTPUT0]

#include <engine/shader/header/spritebatch.frag>
in vec4 v_channel;
in float v_threshold;
in float v_borderThresholdInwardsOffset;
in vec4 v_baseColor;
in vec4 v_borderColor;

const float smoothing = .3;

void main() {
    #include <engine/shader/start/spritebatch.frag>

    vec4 tex = texture0Tex(v_uv0);
    float sdfValue = dot(vec4(1.0), v_channel * tex);
    float outputAlpha = smoothstep(v_threshold - smoothing * .5, v_threshold + smoothing * .5, sdfValue);

    float borderColorMix =
    1.0 - smoothstep(v_threshold + v_borderThresholdInwardsOffset - smoothing * .5,
    v_threshold + v_borderThresholdInwardsOffset + smoothing * .5, sdfValue);

    OUTPUT0 = mix(v_baseColor, v_borderColor, borderColorMix);
    OUTPUT0.a *= outputAlpha;

    //    OUTPUT0.rg = bufferUV * 10.0;
    OUTPUT0.a = 1.0;

    #include <engine/shader/end/spritebatch.frag>
}
