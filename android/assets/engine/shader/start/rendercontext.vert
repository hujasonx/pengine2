// Skinned and instanced objects get their data from the boneTransforms dataBuffer. Only non
// instanced, unskinned objects should be drawn using uniform transforms.
vec3 worldPos = v_skinnedPos;
#if defined(a_norFlag)
vec3 worldNor = v_skinnedNor;
#else
vec3 worldNor = vec3(0.0);
#endif// a_norFlag

//#if !defined(a_bon0Flag) && !defined(instancedFlag)
//// If there is no skinning and no instancing, use the uniform transform.
//worldPos = (u_worldTransform * vec4(a_pos, 1.0)).xyz;
//#if defined(a_norFlag)
//worldNor = normalize((u_worldTransformInvTra * vec4(a_nor, 0.0)).xyz);
//#endif// a_norFlag
//#endif// a_bon0Flag || instancedFlag

#if !defined(a_bon0Flag)// No skinning.
mat4 renderContextModelTransform;
mat4 renderContextModelTransformInvTra;
#if defined(instancedFlag)
// If there was an instanced flag but no bone flag, then get the world transform from the bone
// transform.
renderContextModelTransform = boneTransformsMat4I(0);
renderContextModelTransformInvTra = renderContextModelTransform;
#else
// Otherwise, get the transform from uniforms.
renderContextModelTransform = u_worldTransform;
renderContextModelTransformInvTra = u_worldTransformInvTra;
#endif
worldPos = (renderContextModelTransform * vec4(a_pos, 1.0)).xyz;
#if defined(a_norFlag)
worldNor = normalize((renderContextModelTransformInvTra * vec4(a_nor, 0.0)).xyz);
#endif// a_norFlag
#endif