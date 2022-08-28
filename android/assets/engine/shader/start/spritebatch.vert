#ifdef pos2dFlag
v_pos = vec4(a_pos, 0.0, 1.0);
#else
v_pos = vec4(a_pos, 1.0);
#endif

#ifdef a_col0Flag
v_col0 = a_col0;
#else
v_col0 = vec4(1.0);
#endif

#ifdef a_col1Flag
v_col1 = a_col1;
#else
v_col1 = vec4(1.0);
#endif

#ifdef a_col2Flag
v_col2 = a_col2;
#else
v_col2 = vec4(1.0);
#endif

#ifdef a_col3Flag
v_col3 = a_col3;
#else
v_col3 = vec4(1.0);
#endif

#ifdef a_uv0Flag
v_uv0 = a_uv0;
#else
v_uv0 = vec2(0.0);
#endif

#ifdef a_uv1Flag
v_uv1 = a_uv1;
#else
v_uv1 = vec2(0.0);
#endif

#ifdef a_uv2Flag
v_uv2 = a_uv2;
#else
v_uv2 = vec2(0.0);
#endif

#ifdef a_uv3Flag
v_uv3 = a_uv3;
#else
v_uv3 = vec2(0.0);
#endif

gl_Position = u_viewProjTransform * v_pos;