#include <engine/shader/header/shared.frag>
#include <engine/shader/header/light.frag>

#include <engine/shader/header/texture2D>[depth]

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

void main() {
    #include <engine/shader/start/shared.frag>
    #include <engine/shader/start/light.frag>

    #include <engine/shader/template/decodelocation>[worldLoc, depth, bufferUV, u_cameraViewProInv];

    vec3 light = vec3(0.0);

    light += diffuse * u_ambientLightCol;

    light += diffuse * u_directionalLightCol0 * clamp(-dot(u_directionalLightDir0, normal), 0.0, 1.0);

    lighted = vec4(light, 1.0);

    #include <engine/shader/end/light.frag>
    #include <engine/shader/end/shared.frag>
}