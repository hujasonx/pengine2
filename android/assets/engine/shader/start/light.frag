vec4 diffuseM = texture(u_diffuseMTex, bufferUV);
vec3 diffuse = diffuseM.rgb;
vec4 normalR = texture(u_normalRTex, bufferUV);
vec3 normal = diffuseM.xyz;
vec4 emissiveI = texture(u_emissiveITex, bufferUV);
vec3 emissive = emissiveI.rgb;
