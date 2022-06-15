v_instanceID = gl_InstanceID;
#include <engine/shader/start/shared.vert>

//#ifdef a_bon0Flag// Skinning
//#else// No skinning, but instanced; still, use the bone transforms matrices.
//mat4 transform = mat4(1.0);
//v_skinnedPos = (transform * vec4(a_pos, 1.0)).xyz;
//v_skinnedPos.z += 3.0;
//#ifdef a_norFlag
//v_skinnedNor = normalize((transform * vec4(a_nor, 0.0)).xyz);
//#endif
//#endif
