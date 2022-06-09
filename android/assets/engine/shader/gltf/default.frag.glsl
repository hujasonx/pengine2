#include <engine/shader/bootstrap.frag>

void main() {
    o_dif = vec4(v_worldPos.xyz, 1.0);

}