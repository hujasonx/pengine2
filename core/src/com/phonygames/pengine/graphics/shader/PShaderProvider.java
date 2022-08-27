package com.phonygames.pengine.graphics.shader;

import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.util.collection.PMap;
import com.phonygames.pengine.util.collection.PStringMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public abstract class PShaderProvider {
  private static boolean log = false;
  public static void init() {
  }

  public abstract PShader provide(String fragmentLayout, String layer, @NonNull PVertexAttributes vertexAttributes,
                                  PMaterial material);

  public abstract static class PMapShaderProvider extends PShaderProvider {
    @Getter
    @Setter
    private PShader defaultShader = null;
    private PStringMap<PStringMap<PMap<PVertexAttributes, PStringMap<PShader>>>>
        fragmentLayoutLayerVertexAttributesPrefixMap =
        new PStringMap<PStringMap<PMap<PVertexAttributes, PStringMap<PShader>>>>() {
          @Override protected PStringMap<PMap<PVertexAttributes, PStringMap<PShader>>> newUnpooled(/* fragmentLayout */
              String s) {
            return new PStringMap<PMap<PVertexAttributes, PStringMap<PShader>>>() {
              @Override protected PMap<PVertexAttributes, PStringMap<PShader>> newUnpooled(/* layer */ String o) {
                return new PMap<PVertexAttributes, PStringMap<PShader>>() {
                  @Override protected PStringMap<PShader> newUnpooled(PVertexAttributes o) {
                    return new PStringMap<PShader>(); // Material id
                  }
                };
              }
            };
          }
        };


    // First check if there is a material id specific shader.
    @Override public PShader provide(String fragmentLayout, String layer, @NonNull PVertexAttributes vertexAttributes,
                                     PMaterial material) {
      String layerId = layer == null ? "" : layer;
      PShader shader = fragmentLayoutLayerVertexAttributesPrefixMap.genUnpooled(fragmentLayout).genUnpooled(layerId)
                                                                   .genUnpooled(vertexAttributes)
                                                                   .get(material.getShaderPrefix());
      if (shader != null) {
        return shader;
      }
      shader = genShader(fragmentLayout, layer, vertexAttributes, material);
      if (shader != null) {
          fragmentLayoutLayerVertexAttributesPrefixMap.genUnpooled(fragmentLayout)
                                                      .genUnpooled(layer == null ? "" : layer)
                                                      .genUnpooled(vertexAttributes).put(material.getShaderPrefix(), shader);
        return shader;
      }
      return null;
    }

    public abstract PShader genShader(String fragmentLayout, String layer, PVertexAttributes vertexAttributes,
                                      PMaterial material);
  }
}
