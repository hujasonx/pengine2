uniform vec4 u_tdtuituidt;
uniform vec4 u_renderBufferSize;

in vec3 a_pos;
out vec3 v_aPos;

#ifdef a_norFlag
in vec3 a_nor;
out vec3 v_aNor;
#endif


#include <engine/shader/header/sharedvertarray>[0]
#include <engine/shader/header/sharedvertarray>[1]
#include <engine/shader/header/sharedvertarray>[2]
#include <engine/shader/header/sharedvertarray>[3]
#include <engine/shader/header/sharedvertarray>[4]
#include <engine/shader/header/sharedvertarray>[5]
#include <engine/shader/header/sharedvertarray>[6]
#include <engine/shader/header/sharedvertarray>[7]
#include <engine/shader/header/sharedvertarray>[8]

// Uniforms for skinning.
#ifdef a_bon0Flag
#include <engine/shader/header/texture2D>[boneTransforms]
#endif