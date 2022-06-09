// MAIN START
v_aPos = a_pos;
v_worldPos = u_worldTransform * vec4(a_pos, 1.0);

#ifdef a_norFlag
v_worldNor = normalize((u_worldTransformInvTra * vec4(a_nor, 0.0)).xyz);
#endif

#ifdef a_uv0Flag
v_uv0 = a_uv0;
#endif

#ifdef a_col0Flag
v_col0 = a_col0;
#endif

gl_Position = u_viewProjTransform * v_worldPos;
v_worldPos.w = dot(u_cameraDir, v_worldPos.xyz - u_cameraPos);