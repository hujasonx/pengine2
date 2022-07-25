package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.Gdx;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.logging.PLog;

public class PGltf {
  public static PShaderProvider DEFAULT_SHADER_PROVIDER = new PShaderProvider.PMapShaderProvider() {
    @Override public PShader genShader(String fragmentLayout, String layer, PVertexAttributes vertexAttributes,
                                       PMaterial material) {
      if (Layer.PBR.equals(layer)) {
          return new PShader(material.getShaderPrefix(), fragmentLayout, vertexAttributes,
                             Gdx.files.local("engine/shader/gltf/vcolindex.vert.glsl"),
                             Gdx.files.local("engine/shader/gltf/vcolindex.frag.glsl"), null);
      } else if (Layer.AlphaBlend.equals(layer)) {
          return new PShader(material.getShaderPrefix(), fragmentLayout, vertexAttributes,
                             Gdx.files.local("engine/shader/gltf/vcolindex.vert.glsl"),
                             Gdx.files.local("engine/shader/gltf/vcolindex.alphablend.frag.glsl"), null);
      } else {
        PLog.w("PShaderProvider Unsupported layer: " + layer);
        return null;
      }
    }
  };

  public static final class Layer {
    public static final String AlphaBlend = "AlphaBlend";
    public static final String PBR = "PBR";
  }
}
