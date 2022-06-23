#include <engine/shader/header/texture2D>[diffuseM]
#include <engine/shader/header/texture2D>[normalR]
#include <engine/shader/header/texture2D>[emissiveI]
float lightAttenuationRaw(float a, float b, float c, float cutoff, float d) {
    return clamp((1.0 / (a * d * d + b * d + c) - cutoff) / (1.0 - cutoff), 0.0, 10000.0);
}
float lightAttenuation(vec4 attenuation, float d) {
    return lightAttenuationRaw(attenuation.x, attenuation.y, attenuation.z, attenuation.w, d);
}
// Sets the output to 0 if the normal points in the same direction as the light.
vec3 reflectOrZero(vec3 lightDir, vec3 normal) {
    vec3 outVec = normalize(-reflect(lightDir, normal));
    return outVec * step(0.0, -dot(lightDir, normal));
}