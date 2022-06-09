#include <engine/shader/bootstrap.frag>

void main() {
    #include <engine/shader/bootstrap.main.start.frag>
    o_dif = vec4(v_worldPos.xyz, 1.0);

}