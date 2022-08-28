#include <engine/shader/header/shared>

#ifdef pos2dFlag
in vec2 a_pos;
#else
in vec3 a_pos;
#endif

#ifdef a_col0Flag
in vec4 a_col0;
#endif

#ifdef a_col1Flag
in vec4 a_col1;
#endif

#ifdef a_col2Flag
in vec4 a_col2;
#endif

#ifdef a_col2Flag
in vec4 a_col2;
#endif

#ifdef a_uv0Flag
in vec2 a_uv0;
#endif

#ifdef a_uv1Flag
in vec2 a_uv1;
#endif

#ifdef a_uv2Flag
in vec2 a_uv2;
#endif

#ifdef a_uv2Flag
in vec2 a_uv2;
#endif

out vec4 v_pos;
out vec4 v_col0;
out vec4 v_col1;
out vec4 v_col2;
out vec4 v_col3;
out vec2 v_uv0;
out vec2 v_uv1;
out vec2 v_uv2;
out vec2 v_uv3;
uniform mat4 u_viewProjTransform;