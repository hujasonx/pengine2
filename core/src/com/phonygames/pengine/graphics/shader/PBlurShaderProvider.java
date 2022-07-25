package com.phonygames.pengine.graphics.shader;

import com.phonygames.pengine.graphics.PRenderBuffer;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringMap;

/** Helper class for blurring RenderBuffers.*/
public class PBlurShaderProvider {
  private static final PStringMap<PShader> shaders = new PStringMap<>();

  public static PShader genShader(PRenderBuffer renderBuffer) {
    if (shaders.has(renderBuffer.fragmentLayout())) { return shaders.get(PRenderBuffer.activeBuffer().fragmentLayout());}
    return null;


  }
}
