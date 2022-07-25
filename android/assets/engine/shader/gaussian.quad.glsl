#include <engine/shader/header/shared.frag>


uniform float u_blurHorizontal; // Set to 1 to make horizontal, 0 to make vertical.
uniform float u_bloomScale;

float weight[10] = float[] (0.09973557, 0.09666703, 0.08801633, 0.075284354, 0.06049268, 0.045662273, 0.0323794, 0.02156933, 0.013497742, 0.007934913);
int size = 10;

void main() {
    #include <engine/shader/start/shared.frag>

    vec4 source = sourceTex(bufferUV);
    float value = max(0.0, grayScale(source.rgb) - u_bloomThreshold) * u_bloomScale;
    bloom = vec4(source.rgb * value, 1.);
    vec2 tex_offset = 1.0 / u_renderBufferSize;// Gets size of a single texel.
    if (u_parameters.x > 0.5) { // Horizontal.
        for (int i = 1; i < size; ++i) {
            result += texture(u_tex, positionOnScreenNormalized + vec2(tex_offset.x * float(i) * u_parameters.y, 0.0)).rgb * weight[i];
            result += texture(u_tex, positionOnScreenNormalized - vec2(tex_offset.x * float(i) * u_parameters.y, 0.0)).rgb * weight[i];
        }
    } else { // Vertical.
        for (int i = 1; i < size; ++i) {
            result += texture(u_tex, positionOnScreenNormalized + vec2(0.0, tex_offset.y * float(i) * u_parameters.y)).rgb * weight[i];
            result += texture(u_tex, positionOnScreenNormalized - vec2(0.0, tex_offset.y * float(i) * u_parameters.y)).rgb * weight[i];
        }
    }

    #include <engine/shader/end/shared.frag>
}