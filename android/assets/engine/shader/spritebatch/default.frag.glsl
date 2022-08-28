#params [OUTPUT0]

#include <engine/shader/header/spritebatch.frag>

void main() {
    #include <engine/shader/start/spritebatch.frag>

    OUTPUT0 = vec4(v_col0) * texture0Tex(vec2(v_uv0.x, v_uv0.y));

    #include <engine/shader/end/spritebatch.frag>
}
