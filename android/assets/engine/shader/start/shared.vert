// Use skinning if the transform should use the boneTransforms buffer ; either because the model
// is skinned or because the model is instanced.
v_aPos = a_pos;
v_skinnedPos = a_pos;
#ifdef a_norFlag
v_aNor = a_nor;
v_skinnedNor = a_nor;
#endif
#ifdef a_bon0Flag
mat4 skinning = mat4(0.0);
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
#ifdef a_bon0Flag
v_skinnedPos = (skinning * vec4(a_pos, 1.0)).xyz;
#ifdef a_norFlag
v_skinnedNor = normalize((skinning * vec4(a_nor, 0.0)).xyz);
#endif
#endif