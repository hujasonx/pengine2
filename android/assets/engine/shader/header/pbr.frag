uniform vec2 u_metallicRoughness;
uniform vec4 u_diffuseCol;
uniform vec4 u_emissiveCol;

in vec4 v_diffuseM;
in vec4 v_emissiveI;

#include <engine/shader/header/texture2D>[diffuse]
#include <engine/shader/header/texture2D>[emissive]
#include <engine/shader/header/texture2D>[metallicRoughness]

#include <engine/shader/header/texture2D>[vColIndex]
