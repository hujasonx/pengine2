#include <engine/shader/header/shared.frag>
#include <engine/shader/header/light.frag>

uniform vec3 u_directionalLightCol0;
uniform vec3 u_directionalLightCol1;
uniform vec3 u_directionalLightCol2;
uniform vec3 u_directionalLightCol3;

uniform vec3 u_directionalLightDir0;
uniform vec3 u_directionalLightDir1;
uniform vec3 u_directionalLightDir2;
uniform vec3 u_directionalLightDir3;

void main() {
    #include <engine/shader/start/shared.frag>
    #include <engine/shader/start/light.frag>


    lighted = diffuseM;

    #include <engine/shader/end/light.frag>
    #include <engine/shader/end/shared.frag>
}