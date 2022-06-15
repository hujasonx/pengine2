// Skinned and instanced objects get their data from the boneTransforms dataBuffer. Only non
// instanced, unskinned objects should be drawn using uniform transforms.
#if defined(a_bon0Flag) || defined(instancedFlag)
vec3 worldPos = v_skinnedPos;
#ifdef a_norFlag
vec3 worldNor = v_skinnedNor;
#else// No a_norFlag
vec3 worldNor = vec3(0.0);
#endif// a_norFlag
#else// No a_bon0Flag and no instancedFlag.
vec3 worldPos = (u_worldTransform * vec4(a_pos, 1.0)).xyz;
#ifdef a_norFlag
vec3 worldNor = normalize((u_worldTransformInvTra * vec4(a_nor, 0.0)).xyz);
#else// No a_norFlag
vec3 worldNor = vec3(0.0);
#endif// a_norFlag
#endif// a_bon0Flag