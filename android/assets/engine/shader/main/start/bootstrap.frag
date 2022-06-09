// MAIN START

vec4 diffuseM = vec4(1.0);
vec4 emissiveR = vec4(0.0, 0.0, 0.0, 1.0);

#ifdef a_norFlag
vec3 nor = v_worldNor;
#else
vec3 nor = vec3(0.0);
#endif

#ifdef a_uv0Flag
vec2 uv0 = v_uv0;
#else
vec2 uv0 = vec2(0.0);
#endif

#ifdef pbrFlag
diffuseM = u_diffuseCol * texture(u_diffuseTex, uv0);
diffuseM.rgb = diffuseM.rgb * diffuseM.a;// Set the diffuse color.
diffuseM.a = 1.0;// Set the metalicity.
emissiveR = u_emissiveCol;// * texture(u_emissiveTex, uv0);
emissiveR.rgb = emissiveR.rgb * emissiveR.a;// Set the emissive color.
emissiveR.a = 1.0;// Set the roughness.
#endif