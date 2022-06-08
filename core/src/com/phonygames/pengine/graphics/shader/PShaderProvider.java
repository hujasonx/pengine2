package com.phonygames.pengine.graphics.shader;

import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PSet;

import lombok.Getter;
import lombok.Setter;

public abstract class PShaderProvider {
  static class Defaults {
    public static PShaderProvider gltfShaderProvider = new PMapShaderProvider();
  }

  public static void init() {
    PMapShaderProvider gltfShaderProvider = (PMapShaderProvider) Defaults.gltfShaderProvider;
  }

  public PShader provide (PGlNode node) {
    return provide(node.getVertexAttributes(), node.getMaterial());
  }

  public abstract PShader provide(PVertexAttributes vertexAttributes, PMaterial material);

  public static class PMapShaderProvider extends PShaderProvider {
    private PMap<PVertexAttributes, PShader> vertexAttributesMap = new PMap<>();
    private PMap<String, PShader> materialIdMap = new PMap<>();
    private PMap<String, PMap<PVertexAttributes, PShader>> combinedMap = new PMap<>();

    @Getter @Setter
    private PShader defaultShader = null;

    @Override
    public PShader provide(PVertexAttributes vertexAttributes, PMaterial material) {
      PShader shader = null;
      if (combinedMap.containsKey(material.getId())) {
        shader = combinedMap.get(material.getId()).get(vertexAttributes);
        if (shader != null) {
          return shader;
        }
      }

      shader = vertexAttributesMap.get(vertexAttributes);
      if (shader != null) {
        return shader;
      }

      shader = materialIdMap.get(material.getId());
      if (shader != null) {
        return shader;
      }

      return defaultShader;
    }

    public PMapShaderProvider set(PVertexAttributes vertexAttributes, String materialId, PShader shader) {
      PMap<PVertexAttributes, PShader> shaderPMap = combinedMap.get(materialId);

      if (shaderPMap == null) {
        combinedMap.put(materialId, shaderPMap = new PMap<PVertexAttributes, PShader>());
      }

      shaderPMap.put(vertexAttributes, shader);
      return this;
    }

    public PMapShaderProvider set(PVertexAttributes vertexAttributes, PShader shader) {
      vertexAttributesMap.put(vertexAttributes, shader);
      return this;
    }

    public PMapShaderProvider set(String materialId, PShader shader) {
      materialIdMap.put(materialId, shader);
      return this;
    }
  }
}
