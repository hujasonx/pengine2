#include <engine/shader/head/shared.frag>

#include <engine/shader/header/texture2D>[diffuseM]
void main() {
    #include <engine/shader/start/shared.frag>

    vec4 diffuseM = texture(u_diffuseMTex, uv);
    lighted = vec4(diffuseM.rgb, 1.0);
    #include <engine/shader/end/shared.frag>
}