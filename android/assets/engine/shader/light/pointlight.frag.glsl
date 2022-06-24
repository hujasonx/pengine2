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

    vec3 albedo = diffuse;// Metalness is not being used because it relies on reflection. However,
    // it would go here, subtracting from the diffuse.
    vec3 worldPosDeltaToCenter = lightPos - worldPos;
    float attenuationFactor = lightAttenuation(attenuation, length(worldPosDeltaToCenter));
    float normalFactor = clamp(dot(worldPosDeltaToCenter, normal), 0.0, 1.0);
    float diffuseStrength = celShadeStrength(normalFactor, .2, .4, .01, .7);// TODO: use a uniform.
    lighted = vec4(lightCol * attenuationFactor * diffuseStrength * albedo, 0.0);


    vec3 reflectedDirection = reflectOrZero(-worldPosDeltaToCenter, normal);
    float specularStrength = max(dot(reflectedDirection, u_cameraDir), 0.0);
    // Spcular strength is just 1 - roughness, which is stored in the w component of normalR.
    // TODO: use a real BRDF function here.
    lighted.rgb += albedo * lightCol * pow(specularStrength, 24.0) * attenuationFactor * (1.0 - normalR.w);
    lighted.a = 1.0;

    #include <engine/shader/end/light.frag>
    #include <engine/shader/end/rendercontext.frag>
    #include <engine/shader/end/instanced.frag>
}