#ifdef pos2dFlag
v_pos = vec4(a_pos, 0.0, 1.0);
#else
v_pos = vec4(a_pos, 1.0);
#endif

gl_Position = u_viewProjTransform * v_pos;