#include <engine/shader/header/shared>

#ifdef pos2dFlag
in vec2 a_pos;
#else
in vec3 a_pos;
#endif

out vec4 v_pos;
uniform mat4 u_viewProjTransform;