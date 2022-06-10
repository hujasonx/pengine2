// MAIN START
v_aPos = a_pos;
v_worldPos = u_worldTransform * vec4(a_pos, 1.0);

gl_Position = u_viewProjTransform * v_worldPos;
v_worldPos.w = dot(u_cameraDir, v_worldPos.xyz - u_cameraPos);