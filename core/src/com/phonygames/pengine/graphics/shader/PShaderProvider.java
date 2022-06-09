package com.phonygames.pengine.graphics.shader;

import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PSet;

import lombok.Getter;
import lombok.Setter;

public abstract class PShaderProvider {

  public static void init() {
  }

  public PShader provide(String fragmentLayout, PGlNode node) {
    return provide(fragmentLayout, node.getVertexAttributes(), node.getMaterial());
  }

  public abstract PShader provide(String fragmentLayout, PVertexAttributes vertexAttributes, PMaterial material);

  public abstract static class PMapShaderProvider extends PShaderProvider {
    private PMap<String, PMap<PVertexAttributes, PShader>> fragmentLayoutVertexAttributesMap = new PMap<String, PMap<PVertexAttributes, PShader>>() {
      @Override
      protected Object makeNew(String o) {
        return new PMap<PVertexAttributes, PShader>();
      }
    };
    private PMap<String, PMap<PVertexAttributes, PMap<String, PShader>>> fragmentLayoutVertexAttributesMaterialIdMap =
        new PMap<String, PMap<PVertexAttributes, PMap<String, PShader>>>() {
          @Override
          protected Object makeNew(String o) {
            return new PMap<PVertexAttributes, PMap<String, PShader>>() {
              @Override
              protected Object makeNew(PVertexAttributes o) {
                return new PMap<String, PShader>();
              }
            };
          }
        };

    @Getter
    @Setter
    private PShader defaultShader = null;

    @Override
    public PShader provide(String fragmentLayout, PVertexAttributes vertexAttributes, PMaterial material) {
      PShader shader = fragmentLayoutVertexAttributesMaterialIdMap.getOrMake(fragmentLayout).getOrMake(vertexAttributes).get(material.getId());
      if (shader != null) {
        return shader;
      }

      shader = fragmentLayoutVertexAttributesMap.getOrMake(fragmentLayout).get(vertexAttributes);
      if (shader != null) {
        return shader;
      }

      shader = genShader(fragmentLayout, vertexAttributes, material);
      if (shader != null) {
        fragmentLayoutVertexAttributesMaterialIdMap.getOrMake(fragmentLayout).getOrMake(vertexAttributes).put(material.getId(), shader);
        return shader;
      }

      shader = genShader(fragmentLayout, vertexAttributes);
      if (shader != null) {
        fragmentLayoutVertexAttributesMap.getOrMake(fragmentLayout).put(vertexAttributes, shader);
        return shader;
      }

      return null;
    }

    public PMapShaderProvider set(String fragmentLayout, PVertexAttributes vertexAttributes, PShader shader) {
      fragmentLayoutVertexAttributesMap.getOrMake(fragmentLayout).put(vertexAttributes, shader);
      return this;
    }

    public PMapShaderProvider set(String fragmentLayout, PVertexAttributes vertexAttributes, String materialId, PShader shader) {
      fragmentLayoutVertexAttributesMaterialIdMap.getOrMake(fragmentLayout).getOrMake(vertexAttributes).put(materialId, shader);
      return this;
    }

    public boolean has(String fragmentLayout, PVertexAttributes vertexAttributes) {
      return fragmentLayoutVertexAttributesMap.getOrMake(fragmentLayout).containsKey(vertexAttributes);
    }

    public boolean has(String fragmentLayout, PVertexAttributes vertexAttributes, String materialId) {
      return fragmentLayoutVertexAttributesMaterialIdMap.getOrMake(fragmentLayout).getOrMake(vertexAttributes).containsKey(materialId);
    }

    public PShader genShader(String fragmentLayout, PVertexAttributes vertexAttributes) {
      return null;
    }

    public PShader genShader(String fragmentLayout, PVertexAttributes vertexAttributes, PMaterial material) {
      return null;
    }
  }
}
