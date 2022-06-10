// MAIN END

gl_Position = u_viewProjTransform * vec4(v_worldPos.xyz, 1.0);
v_worldPos.w = dot(u_cameraDir, v_worldPos.xyz - u_cameraPos);