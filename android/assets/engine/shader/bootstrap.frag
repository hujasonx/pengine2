#include <engine/shader/bootstrap.shared>



#ifdef u_pos;
in vec3 v_pos;
#endif// u_pos;

#ifdef u_nor;
in vec3 v_nor;
#endif// u_nor;

#ifdef u_uv0;
in vec2 v_uv0;
#endif// u_uv0;

#ifdef u_uv1;
in vec2 v_uv1;
#endif// u_uv1;

#ifdef u_uv2;
in vec2 v_uv2;
#endif// u_uv2;

#ifdef u_uv3;
in vec2 v_uv3;
#endif// u_uv3;

#ifdef u_col0;
in vec4 v_col0;
#endif// u_col0;

#ifdef u_col1;
in vec4 v_col1;
#endif// u_col1;

#ifdef u_col2;
in vec4 v_col2;
#endif// u_col2;

#ifdef u_col3;
in vec4 v_col3;
#endif// u_col3;

layout(location = 0) out vec4 o_dif;

uniform mat4 u_projViewTrans;
uniform mat4 u_worldTrans;
uniform mat4 u_worldTransInvTrans;