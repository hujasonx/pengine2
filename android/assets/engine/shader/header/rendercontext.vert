uniform mat4 u_viewProjTransform;
uniform mat4 u_viewProjTransformInvTra;
uniform mat4 u_worldTransform;
uniform mat4 u_worldTransformInvTra;

uniform vec3 u_cameraPos;
uniform vec3 u_cameraDir;
uniform vec3 u_cameraUp;

out vec3 v_worldPos;
out vec3 v_worldNor;