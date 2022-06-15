uniform vec4 u_tdtuituidt;
uniform vec4 u_renderBufferSize;

in vec3 v_aPos;
in vec3 v_skinnedPos;
#ifdef a_norFlag
in vec3 v_aNor;
in vec3 v_skinnedNor;
#endif

#include <engine/shader/header/sharedfragarray>[0]
#include <engine/shader/header/sharedfragarray>[1]
#include <engine/shader/header/sharedfragarray>[2]
#include <engine/shader/header/sharedfragarray>[3]
#include <engine/shader/header/sharedfragarray>[4]
#include <engine/shader/header/sharedfragarray>[5]
#include <engine/shader/header/sharedfragarray>[6]
#include <engine/shader/header/sharedfragarray>[7]
#include <engine/shader/header/sharedfragarray>[8]

// Uniforms for skinning or instancing.
#include <engine/shader/header/texture2D>[boneTransforms]