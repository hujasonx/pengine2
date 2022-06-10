#include <engine/shader/header/quad.vert>

uniform sampler2D u_diffuseMTex;
void main() {
    #include <engine/shader/main/start/quad.vert>

    vec4 diffuseM = texture(u_diffuseMTex, uv);
    lighted = vec4(diffuseM.rgb, 1.0);
}