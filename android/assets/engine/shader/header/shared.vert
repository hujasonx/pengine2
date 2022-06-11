uniform vec4 u_tdtuituidt;
uniform vec4 u_renderBufferSize;

in vec3 a_pos;
out vec3 v_aPos;

#ifdef a_norFlag
in vec3 a_nor;
out vec3 v_aNor;
#endif

#ifdef a_uv0Flag
in vec2 a_uv0;
out vec2 v_uv0;
#endif

#ifdef a_col0Flag
in vec4 v_col0;
out vec4 v_col0;
#endif