#params [OUTPUT0]

#include <engine/shader/header/spritebatch.frag>

void main() {
    #include <engine/shader/start/spritebatch.frag>

    vec4 tex = texture0Tex(v_uv0);
    OUTPUT0 = vec4(tex.rgb, 1.0);
    OUTPUT0.r = smoothstep(0.3, 0.7, tex.a);

    #include <engine/shader/end/spritebatch.frag>
}
