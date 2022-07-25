#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[source]
#include <engine/shader/header/texture2D>[depth]
#include <engine/shader/header/texture2D>[normalR]

uniform mat4 u_cameraViewProInv;
uniform vec3 u_cameraDir;
uniform vec3 u_cameraPos;
void main() {
    #include <engine/shader/start/shared.frag>

    float outlineAmount = 0.0;

    vec2 lookUV = bufferUV;
    #include <engine/shader/template/decodelocation>[cPos, depth, lookUV, u_cameraViewProInv];
    vec4 cNorR = normalRTex(lookUV);
    float cDep = dot(cPos - u_cameraPos, u_cameraDir);
    float pixelDepthDeltaForOutline = .1;
    // As the distance increases, increase the required depth delta.
    pixelDepthDeltaForOutline += cDep * .1;
    // Reduce the required delta if the surface is facing the camera.
    pixelDepthDeltaForOutline *= (1. - .2 * abs(dot(cNorR.rgb, u_cameraDir)));
    float pixelNormalDotForOutline = .4f;

    lookUV = bufferUV + vec2(-u_renderBufferSize.z, 0.);
    #include <engine/shader/template/decodelocation>[lPos, depth, lookUV, u_cameraViewProInv];
    vec4 lNorR = normalRTex(lookUV);
    float lDep = dot(lPos - u_cameraPos, u_cameraDir);

    lookUV = bufferUV + vec2(u_renderBufferSize.z, 0.);
    #include <engine/shader/template/decodelocation>[rPos, depth, lookUV, u_cameraViewProInv];
    vec4 rNorR = normalRTex(lookUV);
    float rDep = dot(rPos - u_cameraPos, u_cameraDir);

    lookUV = bufferUV + vec2(0., -u_renderBufferSize.w);
    #include <engine/shader/template/decodelocation>[dPos, depth, lookUV, u_cameraViewProInv];
    vec4 dNorR = normalRTex(lookUV);
    float dDep = dot(dPos - u_cameraPos, u_cameraDir);

    lookUV = bufferUV + vec2(0., u_renderBufferSize.w);
    #include <engine/shader/template/decodelocation>[uPos, depth, lookUV, u_cameraViewProInv];
    vec4 uNorR = normalRTex(lookUV);
    float uDep = dot(uPos - u_cameraPos, u_cameraDir);

    outlineAmount += step(pixelDepthDeltaForOutline, abs(rDep - lDep));
    outlineAmount += step(pixelNormalDotForOutline, 1. - abs(dot(rNorR.xyz, lNorR.xyz)));

    outlineAmount += step(pixelDepthDeltaForOutline, abs(uDep - dDep));
    outlineAmount += step(pixelNormalDotForOutline, 1. - abs(dot(uNorR.xyz, dNorR.xyz)));


    outline = vec4(vec3(.01), clamp(outlineAmount, 0., 1.));
    //    outline = vec4(vec3(rDep), 1.);

    #include <engine/shader/end/shared.frag>
}