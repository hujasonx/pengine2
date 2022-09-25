v_diffuseM = vec4(1.0);
v_emissiveI = vec4(0.0);

#ifdef a_vColIFlag
// Find the lookup index using the vertex color attribute.
int vColIndexLookupIndex = int(a_vColI);
v_diffuseM = vColIndexVec4I(vColIndexLookupIndex * 2 + 0);
v_emissiveI = vColIndexVec4I(vColIndexLookupIndex * 2 + 1);
#else
// Just use the vertex colors like normal.
#ifdef a_col0Flag
v_diffuseM = a_col0;
#else
v_diffuseM = vec4(1.0);
#endif// a_col0Flag
v_emissiveI = vec4(0.0);
#endif// a_vColIFlag