#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/rendercontext.frag>
#include <engine/shader/header/light.frag>

#include <engine/shader/header/floatarrayinstanced>[lightBuffer]
uniform int u_bufferVecsPerLight;

void main() {
    #include <engine/shader/start/instanced.frag>
    #include <engine/shader/start/rendercontext.frag>
    #include <engine/shader/start/light.frag>




    vec3 lightPos = lightBufferFloatArrayVec4Instance(4).xyz;
    lighted = vec4(0.1);

    #include <engine/shader/end/light.frag>
    #include <engine/shader/end/rendercontext.frag>
    #include <engine/shader/end/instanced.frag>
}