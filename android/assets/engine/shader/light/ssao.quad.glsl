#include <engine/shader/header/shared.frag>

#include <engine/shader/header/texture2D>[normalR]
#include <engine/shader/header/texture2D>[depth]
uniform mat4 u_cameraViewProInv;
uniform mat4 u_cameraViewPro;
uniform vec3 u_cameraPos;
uniform vec3 u_cameraDir;


#define NUM_SAMPLES 10
#define NUM_NOISE   4

uniform vec3 u_noise[NUM_NOISE];
uniform vec3 u_samples[NUM_SAMPLES];

uniform float u_ssaoRadius;
uniform float u_ssaoBias;
uniform float u_ssaoMagnitude;
uniform float u_ssaoContrast;

float getDepthFromWorldLocation(vec3 worldLocation) {
    return dot(worldLocation - u_cameraPos, u_cameraDir);
}

vec2 getUVFromWorldSpaceCoordinates(vec3 worldLocation) {
    vec4 loc = (u_cameraViewPro * vec4(worldLocation, 1.0));
    loc.xyz /= loc.w;
    return vec2(loc.x * .5 + .5, loc.y * .5 + .5);
}

void main() {
    #include <engine/shader/start/shared.frag>

    #include <engine/shader/template/decodelocation>[worldPos, depth, bufferUV, u_cameraViewProInv];
    vec3 normal = normalRTex(bufferUV).xyz;
    if (length(normal) < 0.1) {
        ssao = vec4(1.0);
    } else {

            int  noiseX = int(gl_FragCoord.x - 0.5) % 4;
            int  noiseY = int(gl_FragCoord.y - 0.5) % 4;
            vec3 randomVec = u_noise[noiseX + (noiseY * 4)];
            vec3 tangent  = normalizeSafe(randomVec - normal * dot(randomVec, normal));
            vec3 binormal = cross(normal, tangent);
            mat3 tbn      = mat3(tangent, binormal, normal);

            float occlusion = float(NUM_SAMPLES);
            float samplesTaken = 0.0;

            for (int i = 0; i < NUM_SAMPLES; ++i) {
                vec3 samplePosition = tbn * u_samples[i];
                samplePosition = worldPos.xyz + samplePosition * u_ssaoRadius;
                float samplePositionDepth = getDepthFromWorldLocation(samplePosition);
                vec2 sampleProjected = getUVFromWorldSpaceCoordinates(samplePosition);
                #include <engine/shader/template/decodelocation>[worldLocationOfSample, depth, sampleProjected, u_cameraViewProInv];

                float worldLocationOfSampleDepth = getDepthFromWorldLocation(worldLocationOfSample);

            float occluded = 0.0;
            // TODO: use a smoothstep or something here.
            if (samplePositionDepth + u_ssaoBias <= worldLocationOfSampleDepth) { occluded = 0.0; } else { occluded = 1.0; }
            float intensity = smoothstep(0.0, 1.0, u_ssaoRadius / abs(samplePositionDepth - worldLocationOfSampleDepth));
            occluded *= intensity;
            occlusion -= occluded;
        }

        occlusion /= float(NUM_SAMPLES);
        occlusion  = pow(occlusion, u_ssaoMagnitude);
        occlusion  = u_ssaoContrast * (occlusion - 0.5) + 0.5;
        float rampBottom = 0.1;
        occlusion = smoothstep(0.0, 1.0, clamp((occlusion - rampBottom) / (1.0 - rampBottom), 0.0, 1.0));

        ssao = vec4(vec3(occlusion), 1.0);
    }


    #include <engine/shader/end/shared.frag>
}