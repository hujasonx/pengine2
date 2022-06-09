// MAIN START

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