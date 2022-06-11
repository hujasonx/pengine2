// FRAGMENT
#include <engine/shader/header/bootstrap.shared>

in vec3 v_aPos;
in vec4 v_worldPos;

#ifdef a_norFlag
in vec3 v_worldNor;
#endif

#ifdef a_col0Flag
in vec4 v_col0;
#endif

#ifdef a_uv0Flag
in vec2 v_uv0;
#endif

#ifdef pbrFlag
uniform vec2 u_metallicRoughness;
uniform vec4 u_diffuseCol;
uniform vec4 u_emissiveCol;

#include <engine/shader/header/texture2D>[diffuse]
uniform sampler2D u_emissiveTex;
uniform sampler2D u_metallicRoughnessTex;
#endif

