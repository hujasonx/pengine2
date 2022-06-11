vec3 worldPos = (u_worldTransform * skinnedLocalPos4).xyz;


#ifdef a_norFlag
vec3 worldNor = normalize((u_worldTransformInvTra * vec4(skinnedLocalNor, 0.0)).xyz);
#else
vec3 worldNor = vec3(0.0);
#endif