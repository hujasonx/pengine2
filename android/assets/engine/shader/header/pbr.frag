uniform vec2 u_metallicRoughness;
uniform vec4 u_diffuseCol;
uniform vec4 u_emissiveCol;

#include <engine/shader/header/texture2D>[diffuse]
uniform sampler2D u_emissiveTex;
uniform sampler2D u_metallicRoughnessTex;
