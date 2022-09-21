v_diffuseM = vec4(1.0);
v_emissiveI = vec4(0.0);

#ifdef a_col0Flag
#ifdef vColIndexFlag
// Find the lookup index using the vertex color attribute.
const float vColIndexLookupSteps = 16.0;
float vColIndexLookupIndexR = round(v_col0.r * vColIndexLookupSteps);
float vColIndexLookupIndexG = round(v_col0.g * vColIndexLookupSteps);
float vColIndexLookupIndexB = round(v_col0.b * vColIndexLookupSteps);
int vColIndexLookupIndex = int(vColIndexLookupIndexR + (vColIndexLookupIndexG * vColIndexLookupSteps) + (vColIndexLookupIndexB * vColIndexLookupSteps * vColIndexLookupSteps));
v_diffuseM = vColIndexVec4I(vColIndexLookupIndex * 2 + 0);
v_emissiveI = vColIndexVec4I(vColIndexLookupIndex * 2 + 1);
#else
// Just use the vertex colors like normal.
v_diffuseM = a_col0;
v_emissiveI = vec4(0.0);
#endif
#endif