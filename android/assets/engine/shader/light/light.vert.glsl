#include <engine/shader/header/instanced.vert>
#include <engine/shader/header/rendercontext.vert>
#include <engine/shader/header/light.vert>

#include <engine/shader/header/texture2D>[lightBuffer]

void main() {
    #include <engine/shader/start/instanced.vert>
    #include <engine/shader/start/rendercontext.vert>
    #include <engine/shader/start/light.vert>

    mat4 lightTransform = lightBufferMat4I(0);
    worldPos = (lightTransform * vec4(a_pos, 1.0)).xyz;

    #include <engine/shader/end/rendercontext.vert>
    #include <engine/shader/end/light.vert>
    #include <engine/shader/end/instanced.vert>
}