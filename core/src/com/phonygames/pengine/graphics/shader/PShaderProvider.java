package com.phonygames.pengine.graphics.shader;

import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public abstract class PShaderProvider {
  public static void init() {
  }

  public abstract PShader provide(String fragmentLayout, String layer, @NonNull PVertexAttributes vertexAttributes,
                                  PMaterial material);

  public abstract static class PMapShaderProvider extends PShaderProvider {
    @Getter
    @Setter
    private PShader defaultShader = null;
    private PStringMap<PStringMap<PMap<PVertexAttributes, PShader>>> fragmentLayoutLayerVertexAttributesMap =
        new PStringMap<PStringMap<PMap<PVertexAttributes, PShader>>>() {
          @Override protected PStringMap<PMap<PVertexAttributes, PShader>> newUnpooled(/* fragmentLayout */ String s) {
            return new PStringMap<PMap<PVertexAttributes, PShader>>() {
              @Override protected PMap<PVertexAttributes, PShader> newUnpooled(/* layer */ String o) {
                return new PMap<>();
              }
            };
          }
        };
    private PStringMap<PStringMap<PMap<PVertexAttributes, PStringMap<PShader>>>>
        fragmentLayoutLayerVertexAttributesMaterialIdMap =
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
    private boolean markForAnyMaterialId;

    /**
     * Call this to mark the generated shader to be used for any material, not just the one provided.
     */
    protected void markForAnyMaterialId() {
      this.markForAnyMaterialId = true;
    }

    // First check if there is a material id specific shader.
    @Override public PShader provide(String fragmentLayout, String layer, @NonNull PVertexAttributes vertexAttributes,
                                     PMaterial material) {
      String layerId = layer == null ? "" : layer;
      PShader shader = fragmentLayoutLayerVertexAttributesMaterialIdMap.genUnpooled(fragmentLayout).genUnpooled(layerId)
                                                                       .genUnpooled(vertexAttributes)
                                                                       .get(material.getId());
      if (shader != null) {
        return shader;
      }
      shader =
          fragmentLayoutLayerVertexAttributesMap.genUnpooled(fragmentLayout).genUnpooled(layerId).get(vertexAttributes);
      if (shader != null) {
        return shader;
      }
      shader = genShader(fragmentLayout, layer, vertexAttributes, material);
      if (shader != null) {
        if (markForAnyMaterialId) {
          PLog.i("PShaderProvider generating new shader any material, original: " + material.getId());
          markForAnyMaterialId = false;
          fragmentLayoutLayerVertexAttributesMap.get(fragmentLayout).genUnpooled(layerId).put(vertexAttributes, shader);
        } else {
          PLog.i("PShaderProvider generating new shader for: " + material.getId());
          fragmentLayoutLayerVertexAttributesMaterialIdMap.genUnpooled(fragmentLayout)
                                                          .genUnpooled(layer == null ? "" : layer)
                                                          .genUnpooled(vertexAttributes).put(material.getId(), shader);
        }
        return shader;
      }
      return null;
    }

    public abstract PShader genShader(String fragmentLayout, String layer, PVertexAttributes vertexAttributes,
                                      PMaterial material);
  }
}
