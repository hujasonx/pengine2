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

layout(location = 0) out vec4 o_dif;