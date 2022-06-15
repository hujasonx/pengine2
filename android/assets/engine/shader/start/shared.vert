// Use skinning if the transform should use the boneTransforms buffer ; either because the model
// is skinned or because the model is instanced.
v_aPos = a_pos;
v_skinnedPos = a_pos;
#ifdef a_norFlag
v_aNor = a_nor;
v_skinnedNor = a_nor;
#endif

#if defined(a_bon0Flag)
mat4 skinning = mat4(0.0);// Skinned, so use an empty matrix that will be filled per bone weight.
#elif defined(instancedFlag)
mat4 skinning = boneTransformsMat4I(0);// The first and only bone transform for the instance.
#else
mat4 skinning = mat4(1.0);// Identity matrix for nonskinned, non-instanced objects.
#endif

#include <engine/shader/start/sharedvertarray>[0]
#include <engine/shader/start/sharedvertarray>[1]
#include <engine/shader/start/sharedvertarray>[2]
#include <engine/shader/start/sharedvertarray>[3]
#include <engine/shader/start/sharedvertarray>[4]
#include <engine/shader/start/sharedvertarray>[5]
#include <engine/shader/start/sharedvertarray>[6]
#include <engine/shader/start/sharedvertarray>[7]

// Apply skinning.
v_skinnedPos = (skinning * vec4(a_pos, 1.0)).xyz;
#ifdef a_norFlag
v_skinnedNor = normalize((skinning * vec4(a_nor, 0.0)).xyz);
#endif
