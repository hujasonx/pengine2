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
    vec3 cNor = normalRTex(lookUV).xyz;
    float cDep = dot(cPos - u_cameraPos, u_cameraDir);
    float pixelDepthDeltaForOutline = .1;
    // As the distance increases, increase the required depth delta.
    pixelDepthDeltaForOutline += cDep * .1;
    // Reduce the required delta if the surface is facing the camera.
    pixelDepthDeltaForOutline *= (1. - .2 * abs(dot(cNor, u_cameraDir)));
    float pixelNormalDotForOutline = .4f;

    lookUV = bufferUV + vec2(-u_renderBufferSize.z, 0.);
    #include <engine/shader/template/decodelocation>[lPos, depth, lookUV, u_cameraViewProInv];
    vec3 lNor = normalRTex(lookUV).xyz;
    float lDep = dot(lPos - u_cameraPos, u_cameraDir);

    lookUV = bufferUV + vec2(u_renderBufferSize.z, 0.);
    #include <engine/shader/template/decodelocation>[rPos, depth, lookUV, u_cameraViewProInv];
    vec3 rNor = normalRTex(lookUV).xyz;
    float rDep = dot(rPos - u_cameraPos, u_cameraDir);

    lookUV = bufferUV + vec2(0., -u_renderBufferSize.w);
    #include <engine/shader/template/decodelocation>[dPos, depth, lookUV, u_cameraViewProInv];
    vec3 dNor = normalRTex(lookUV).xyz;
    float dDep = dot(dPos - u_cameraPos, u_cameraDir);

    lookUV = bufferUV + vec2(0., u_renderBufferSize.w);
    #include <engine/shader/template/decodelocation>[uPos, depth, lookUV, u_cameraViewProInv];
    vec3 uNor = normalRTex(lookUV).xyz;
    float uDep = dot(uPos - u_cameraPos, u_cameraDir);

    // Prevent outlines from appearing if both normals are 0.
    float lrInclude = step(.5, dot(rNor, rNor) + dot(lNor, lNor));
    lNor = normalize(lNor);
    rNor = normalize(rNor);
    outlineAmount += step(pixelDepthDeltaForOutline, abs(rDep - lDep)) * lrInclude;
    outlineAmount += step(pixelNormalDotForOutline, 1. - abs(dot(rNor, lNor))) * lrInclude;

    float udInclude = step(.5, dot(uNor, uNor) + dot(dNor, dNor));
    dNor = normalize(dNor);
    uNor = normalize(uNor);
    outlineAmount += step(pixelDepthDeltaForOutline, abs(uDep - dDep)) * udInclude;
    outlineAmount += step(pixelNormalDotForOutline, 1. - abs(dot(uNor, dNor))) * udInclude;

    outline = vec4(vec3(.01), clamp(outlineAmount, 0., 1.));
    //    outline = vec4(vec3(rDep), 1.);

    #include <engine/shader/end/shared.frag>
}