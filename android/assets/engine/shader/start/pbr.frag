diffuseM = vec4(1.0);
emissiveI = vec4(0.0, 0.0, 0.0, 1.0);

#ifdef a_norFlag
normalR = vec4(v_worldNor, 1.0);
#else
normalR = vec4(0.0, 0.0, 0.0, 1.0);
#endif

#ifdef a_uv0Flag
vec2 pbrUV0 = v_uv0;
#else
vec2 pbrUV0 = vec2(0.0);
#endif

diffuseM = u_diffuseCol * diffuseTex(pbrUV0);
diffuseM.rgb = diffuseM.rgb * v_diffuseM.rgb * diffuseM.a;// Set the diffuse color.
diffuseM.a = v_diffuseM.a;// Set the metalicity.

emissiveI = u_emissiveCol * emissiveTex(pbrUV0);// * texture(u_emissiveTex, uv0);
emissiveI.rgb = emissiveI.rgb * v_emissiveI.rgb * emissiveI.a;// Set the emissive color.
emissiveI.a = v_emissiveI.a;// Set the roughness.

alphaBlend = vec4(0.0);