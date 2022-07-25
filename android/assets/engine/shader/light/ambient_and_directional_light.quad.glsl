#include <engine/shader/header/shared.frag>
#include <engine/shader/header/light.frag>

#include <engine/shader/header/texture2D>[depth]
#include <engine/shader/header/texture2D>[ssao]

uniform vec3 u_ambientLightCol;
uniform vec3 u_directionalLightCol0;
uniform vec3 u_directionalLightCol1;
uniform vec3 u_directionalLightCol2;
uniform vec3 u_directionalLightCol3;

uniform vec3 u_directionalLightDir0;
uniform vec3 u_directionalLightDir1;
uniform vec3 u_directionalLightDir2;
uniform vec3 u_directionalLightDir3;

uniform mat4 u_cameraViewProInv;
uniform vec3 u_cameraPos;
uniform vec3 u_cameraDir;

void main() {
    #include <engine/shader/start/shared.frag>
    #include <engine/shader/start/light.frag>

    #include <engine/shader/template/decodelocation>[worldPos, depth, bufferUV, u_cameraViewProInv];

    vec3 light = vec3(0.0);

    vec3 albedo = diffuse;// Metalness is not being used because it relies on reflection. However,
    // it would go here, subtracting from the diffuse.
    light += albedo * u_ambientLightCol * ssaoTex(bufferUV).rgb;

    float diffuseStrength = clamp(-dot(u_directionalLightDir0, normal), 0.0, 1.0);
    light += albedo * u_directionalLightCol0 * celShadeStrength(diffuseStrength, .2, .4, .01, .7);// TODO: use a uniform for the cel shade settings.

    vec3 reflectedDirection = reflectOrZero(u_directionalLightDir0, normal);
    float specularStrength = max(dot(reflectedDirection, u_cameraDir), 0.0);
    // Spcular strength is just 1 - roughness, which is stored in the w component of normalR.
    // TODO: use a real BRDF function here.
    light += albedo * u_directionalLightCol0 * pow(specularStrength, 24.0) * (1.0 - normalR.w);

    light.rgb += emissive;
    lighted = vec4(light, 1.0);

    #include <engine/shader/end/light.frag>
    #include <engine/shader/end/shared.frag>
}