#include <engine/shader/header/light.vert>
#include <engine/shader/header/instanced.vert>

void main() {
    #include <engine/shader/main/start/light.vert>
    #include <engine/shader/main/start/instanced.vert>

    mat4 lightTransform = lightBufferFloatArrayMat4Instance(0);

    v_worldPos = lightTransform * vec4(a_pos, 1.0);

    #include <engine/shader/main/end/light.vert>
}