#include <engine/shader/header/instanced.frag>
#include <engine/shader/header/rendercontext.frag>
#include <engine/shader/header/pbr.frag>

in vec4 v_diffuseM;
in vec4 v_emissiveR;

#include <engine/shader/header/texture2D>[vColIndex]

void main() {
    #include <engine/shader/start/instanced.frag>
    #include <engine/shader/start/rendercontext.frag>

    vec4 diffuse = vec4(1.0);
    vec4 emissive = vec4(0.0, 0.0, 0.0, 1.0);

    #ifdef a_uv0Flag
    vec2 pbrUV0 = v_uv0;
    #else
    vec2 pbrUV0 = vec2(0.0);
    #endif

    diffuse = u_diffuseCol * diffuseTex(pbrUV0);
    diffuse.rgb = diffuse.rgb * diffuse.a;// Set the diffuse color.

    emissive = u_emissiveCol * emissiveTex(pbrUV0);// * texture(u_emissiveTex, uv0);
    emissive.rgb = emissive.rgb * emissive.a;// Set the emissive color.

    alphaBlend.rgb += diffuse.rgb + emissive.rgb;
    alphaBlend.a += diffuse.a + emissive.a;
    alphaBlend = vec4(1.0);

    #include <engine/shader/end/rendercontext.frag>
    #include <engine/shader/end/instanced.frag>
}