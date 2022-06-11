vec3 worldPos = (u_worldTransform * vec4(a_pos, 1.0)).xyz;


#ifdef a_norFlag
vec3 worldNor = normalize((u_worldTransformInvTra * vec4(a_nor, 0.0)).xyz);
#else
vec3 worldNor = vec3(0.0);
#endif