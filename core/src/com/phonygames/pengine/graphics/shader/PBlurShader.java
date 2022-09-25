package com.phonygames.pengine.graphics.shader;

import com.badlogic.gdx.Gdx;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.util.collection.PStringMap;
import com.phonygames.pengine.util.PStringUtils;

/** Helper class for blurring RenderBuffers. */
public class PBlurShader {
  private static final PStringMap<PBlurShader> shaders = new PStringMap<>();
  private final PShader shader;
  private float[] weights =
      new float[]{0.09973557f, 0.09666703f, 0.08801633f, 0.075284354f, 0.06049268f, 0.045662273f, 0.0323794f,
                  0.02156933f, 0.013497742f, 0.007934913f};

  private PBlurShader(PRenderBuffer renderBuffer) {
    String frag =
        "#include <engine/shader/header/shared.frag>\n" + "#include <engine/shader/header/texture2D>[source]\n" +
        "uniform float u_blurHorizontal; // Set to 1 to make horizontal, 0 to make vertical.\n" +
        "uniform float u_spread; // Set to 1 to make horizontal, 0 to make vertical.\n";
    for (int a = 0; a < renderBuffer.numTextures(); a++) {
      frag += "#include <engine/shader/header/texture2D>[" + renderBuffer.getTextureName(a) + "]\n";
    }
    frag += "float weight[10] = float[] (0.09973557, 0.09666703, 0.08801633, 0.075284354, 0.06049268, 0.045662273, 0" +
            ".0323794, 0.02156933, 0.013497742, 0.007934913);\n" + "void main() {\n" + "const int size = 10;\n" +
            "    #include <engine/shader/start/shared.frag>\n";
    for (int a = 0; a < renderBuffer.numTextures(); a++) {
      frag += getBlurBodyForLayout(renderBuffer.getTextureName(a));
    }
    frag += "\n" + "    #include <engine/shader/end/shared.frag>\n" + "}\n";
    this.shader = new PShader("", renderBuffer.fragmentLayout(), PVertexAttributes.Templates.POS,
                              Gdx.files.local("engine/shader/quad.vert.glsl"), frag, null);
  }

  private String getBlurBodyForLayout(String layout) {
    String ret = "    " + layout + " = " + layout + "Tex(bufferUV) * weight[0];\n" +
                 "    if (u_blurHorizontal > 0.5) { // Horizontal.\n" + "        for (int i = 1; i < size; ++i) {\n" +
                 "            " + layout + " += " + layout +
                 "Tex(bufferUV + vec2(u_renderBufferSize.z * float(i) * u_spread, 0.0)) * weight[i];\n" +
                 "            " + layout + " += " + layout +
                 "Tex(bufferUV - vec2(u_renderBufferSize.z * float(i) * u_spread, 0.0)) * weight[i];\n" +
                 "        }\n" + "    } else { // Vertical.\n" + "        for (int i = 1; i < size; ++i) {\n" +
                 "            " + layout + " += " + layout +
                 "Tex(bufferUV + vec2(0.0, u_renderBufferSize.w * float(i) * u_spread)) * weight[i];\n" +
                 "            " + layout + " += " + layout +
                 "Tex(bufferUV - vec2(0.0, u_renderBufferSize.w * float(i) * u_spread)) * weight[i];\n" +
                 "        }\n" + "    }\n";
    return ret;
  }

  public static PBlurShader gen(PRenderBuffer renderBuffer) {
    if (!shaders.has(renderBuffer.fragmentLayout())) {
      shaders.put(renderBuffer.fragmentLayout(), new PBlurShader(renderBuffer));
    }
    return shaders.get(renderBuffer.fragmentLayout());
  }

  public void apply(PRenderBuffer renderBuffer, float spreadHorizontal, float spreadVertical) {
    PAssert.isFalse(renderBuffer.active());
    renderBuffer.begin(true);
    // Horizontal.
    shader.start(PRenderContext.activeContext());
    for (int a = 0; a < renderBuffer.numTextures(); a++) {
      shader.setWithUniform(PStringUtils.concat("u_", PStringUtils.concat(renderBuffer.getTextureName(a), "Tex")),
                            renderBuffer.getTexturePrev(a));
    }
    shader.set("u_blurHorizontal", 1f);
    shader.set("u_spread", spreadHorizontal);
    renderBuffer.renderQuad(shader);
    shader.end();
    renderBuffer.end();
    // Vertical.
    renderBuffer.begin(true);
    shader.start(PRenderContext.activeContext());
    for (int a = 0; a < renderBuffer.numTextures(); a++) {
      shader.setWithUniform(PStringUtils.concat("u_", PStringUtils.concat(renderBuffer.getTextureName(a), "Tex")),
                            renderBuffer.getTexturePrev(a));
    }
    shader.set("u_blurHorizontal", 0f);
    shader.set("u_spread", spreadVertical);
    renderBuffer.renderQuad(shader);
    shader.end();
    renderBuffer.end();
  }
}
