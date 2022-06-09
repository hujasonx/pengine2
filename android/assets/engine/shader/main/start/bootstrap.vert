// MAIN START
v_aPos = a_pos;
v_worldPos = u_worldTransform * vec4(a_pos, 1.0);

#ifdef a_norFlag
v_worldNor = normalize((u_worldTransformInvTra * vec4(a_nor, 0.0)).xyz);
//v_worldPos.xyz += a_nor * .5;
#endif

gl_Position = u_viewProjTransform * v_worldPos;
v_worldPos.w = dot(u_cameraDir, v_worldPos.xyz - u_cameraPos);