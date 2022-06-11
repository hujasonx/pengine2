diffuseM = vec4(1.0);
emissiveI = vec4(0.0, 0.0, 0.0, 1.0);

#ifdef a_norFlag
normalR = vec4(v_worldNor, 1.0);
#else
normalR = vec4(0.0, 0.0, 0.0, 1.0);
#endif

#ifdef a_uv0Flag
vec2 uv0 = v_uv0;
#else
vec2 uv0 = vec2(0.0);
#endif

diffuseM = u_diffuseCol * texture(u_diffuseTex, uv0);
diffuseM.rgb = diffuseM.rgb * diffuseM.a;// Set the diffuse color.
diffuseM.a = 1.0;// Set the metalicity.
emissiveI = u_emissiveCol;// * texture(u_emissiveTex, uv0);
emissiveI.rgb = emissiveI.rgb * emissiveI.a;// Set the emissive color.
emissiveI.a = 1.0;// Set the roughness.
