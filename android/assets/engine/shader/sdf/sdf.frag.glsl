#params [OUTPUT0]

#include <engine/shader/header/spritebatch.frag>

void main() {
    #include <engine/shader/start/spritebatch.frag>

    vec4 tex = texture0Tex(v_uv0);
    //    vec4 tex = texture0Tex(bufferUV * 10.0);
    OUTPUT0 = vec4(vec3(tex.a), 1.0);
    OUTPUT0.r = smoothstep(0.3, 0.7, tex.a);
    //    OUTPUT0.rg = bufferUV * 10.0;

    #include <engine/shader/end/spritebatch.frag>
}
