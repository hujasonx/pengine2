// MAIN START

vec2 bufferUV = gl_FragCoord.xy * u_renderBufferSize.zw;
vec4 diffuseM = texture(u_diffuseMTex, bufferUV);



lighted = vec4(1.0);
lighted.g = 0.2;
lighted.rg = diffuseM.rg;
