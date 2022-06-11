gl_Position = u_viewProjTransform * vec4(worldPos, 1.0);
v_worldPos = worldPos;
v_worldNor = worldNor;
//v_worldPos.w = dot(u_cameraDir, v_worldPos.xyz - u_cameraPos);