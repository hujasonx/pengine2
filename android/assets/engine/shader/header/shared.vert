#include <engine/shader/header/shared>

in vec3 a_pos;
out vec3 v_aPos;
out vec3 v_skinnedPos;
#ifdef a_norFlag
in vec3 a_nor;
out vec3 v_aNor;
out vec3 v_skinnedNor;
#endif
#ifdef a_vColIFlag
in float a_vColI;
out float v_vColI;
#endif

#include <engine/shader/header/sharedvertarray>[0]
#include <engine/shader/header/sharedvertarray>[1]
#include <engine/shader/header/sharedvertarray>[2]
#include <engine/shader/header/sharedvertarray>[3]
#include <engine/shader/header/sharedvertarray>[4]
#include <engine/shader/header/sharedvertarray>[5]
#include <engine/shader/header/sharedvertarray>[6]
#include <engine/shader/header/sharedvertarray>[7]

// Uniforms for skinning or instancing.
#include <engine/shader/header/texture2D>[boneTransforms]