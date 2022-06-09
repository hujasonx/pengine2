#include <engine/shader/bootstrap.shared>

in vec3 a_pos;
out vec4 v_worldPos;
out vec3 v_aPos;

// Should be called at least once, at the beginning.
void genGlPos() {
    v_worldPos = u_worldTrans * vec4(a_pos, 1.0);
    v_aPos = a_pos;


    gl_Position = u_viewProjTransform * v_worldPos;
    v_worldPos.w = dot(u_cameraDir, v_worldPos.xyz - u_cameraPos);
}

    #ifdef a_norFlag
    #define v_norFlag
in vec3 a_nor;
out vec3 v_nor;
#endif

#ifdef a_uv0Flag
#define v_uv0Flag
in vec2 a_uv0;
out vec2 v_uv0;
#endif

#ifdef a_col0Flag
#define v_col0Flag
in vec4 a_col0;
out vec4 v_col0;
#endif