#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/rendercontext.frag>
#include <engine/shader/header/light.frag>

#include <engine/shader/header/texture2D>[depth]
#include <engine/shader/header/texture2D>[lightBuffer]
uniform mat4 u_cameraViewProInv;

uniform int u_bufferVecsPerLight;


void main() {
    #include <engine/shader/start/instanced.frag>
    #include <engine/shader/start/rendercontext.frag>
    #include <engine/shader/start/light.frag>

    #include <engine/shader/template/decodelocation>[worldPos, depth, bufferUV, u_cameraViewProInv];

    vec3 lightPos = lightBufferVec4I(4).xyz;
    vec3 lightCol = lightBufferVec4I(5).rgb;
    vec4 attenuation = lightBufferVec4I(6);

    vec3 worldPosDeltaToCenter = lightPos - worldPos;
    float attenuationFactor = lightAttenuation(attenuation, length(worldPosDeltaToCenter));
    float normalFactor = clamp(dot(worldPosDeltaToCenter, normal), 0.0, 1.0);
    lighted = vec4(attenuationFactor * normalFactor * diffuse, 0.0);
    lighted.r += 0.01;

    #include <engine/shader/end/light.frag>
    #include <engine/shader/end/rendercontext.frag>
    #include <engine/shader/end/instanced.frag>
}