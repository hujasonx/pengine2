#include <engine/shader/header/shared.frag>

uniform float u_useAlpha;
#include <engine/shader/header/texture2D>[data]
void main() {
    #include <engine/shader/start/shared.frag>

    vec4 dataIn = dataTex(bufferUV);
    diffuse = vec4(dataIn.rgb, 1.0);
    if (u_useAlpha > 0.5) {
        diffuse = vec4(vec3(dataIn.a), 1.0);
    };

    #include <engine/shader/end/shared.frag>
}