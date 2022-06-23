#include <engine/shader/header/instanced.vert>
#include <engine/shader/header/rendercontext.vert>
#include <engine/shader/header/pbr.vert>

out vec4 v_diffuseM;
out vec4 v_emissiveR;

#include <engine/shader/header/texture2D>[vColIndex]

void main() {
    #include <engine/shader/start/instanced.vert>
    #include <engine/shader/start/rendercontext.vert>
    #include <engine/shader/start/pbr.vert>

    v_diffuseM = vec4(1.0);
    v_emissiveR = vec4(1.0);
    #ifdef a_col0Flag
    // Find the lookup index using the vertex color attribute.
    const float vColIndexLookupSteps = 16.0;
    float vColIndexLookupIndexR = round(v_col0.r * vColIndexLookupSteps);
    float vColIndexLookupIndexG = round(v_col0.g * vColIndexLookupSteps);
    float vColIndexLookupIndexB = round(v_col0.b * vColIndexLookupSteps);
    int vColIndexLookupIndex = int(vColIndexLookupIndexR + (vColIndexLookupIndexG * vColIndexLookupSteps) + (vColIndexLookupIndexB * vColIndexLookupSteps * vColIndexLookupSteps));
    v_diffuseM = vColIndexVec4I(vColIndexLookupIndex * 2 + 0);
    v_emissiveR = vColIndexVec4I(vColIndexLookupIndex * 2 + 1);
    #endif

    #include <engine/shader/end/pbr.vert>
    #include <engine/shader/end/rendercontext.vert>
    #include <engine/shader/end/instanced.vert>
}