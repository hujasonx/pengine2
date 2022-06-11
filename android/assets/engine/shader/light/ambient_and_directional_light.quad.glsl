#include <engine/shader/header/quad.frag>

uniform sampler2D u_diffuseMTex;
void main() {
    #include <engine/shader/main/start/quad.frag>

    vec4 diffuseM = texture(u_diffuseMTex, uv);
    lighted = vec4(diffuseM.rgb, 1.0);
}