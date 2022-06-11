#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/light.frag>

void main() {
    #include <engine/shader/main/start/instanced.frag>
    #include <engine/shader/main/start/light.frag>




    vec3 lightPos = lightBufferFloatArrayVec4Instance(4).xyz;

    #include <engine/shader/main/end/light.frag>
}