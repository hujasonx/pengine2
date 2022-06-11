// HEADER
#include <engine/shader/header/base.shared>

#include <engine/shader/header/floatarrayinstanced>[lightBuffer]

#include <engine/shader/header/texture2D>[diffuseM]
#include <engine/shader/header/texture2D>[normalR]
#include <engine/shader/header/texture2D>[emissiveI]
in vec4 v_worldPos;
in vec3 v_aPos;

uniform int u_bufferVecsPerLight;
uniform vec4 u_lightColor;

