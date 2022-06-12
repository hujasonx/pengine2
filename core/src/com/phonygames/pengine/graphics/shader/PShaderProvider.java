package com.phonygames.pengine.graphics.shader;

import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.PGlNode;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.util.PMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public abstract class PShaderProvider {

  public static void init() {
  }

  public abstract PShader provide(String fragmentLayout,
                                  String layer,
                                  @NonNull PVertexAttributes vertexAttributes,
                                  PMaterial material);

  public abstract static class PMapShaderProvider extends PShaderProvider {
    private PMap<String, PMap<String, PMap<PVertexAttributes, PShader>>>
        fragmentLayoutLayerVertexAttributesMap =
        new PMap<String, PMap<String, PMap<PVertexAttributes, PShader>>>() {
          @Override
          protected Object makeNewVal(/* fragmentLayout */ String s) {
            return new PMap<String, PMap<PVertexAttributes, PShader>>() {
              @Override
              protected Object makeNewVal(/* layer */ String o) {
                return new PMap<PVertexAttributes, PShader>();
              }
            };
          }
        };
    private PMap<String, PMap<String, PMap<PVertexAttributes, PMap<String, PShader>>>>
        fragmentLayoutLayerVertexAttributesMaterialIdMap =
        new PMap<String, PMap<String, PMap<PVertexAttributes, PMap<String, PShader>>>>() {
          @Override
          protected Object makeNewVal(/* fragmentLayout */ String s) {
            return new PMap<String, PMap<PVertexAttributes, PMap<String, PShader>>>() {
              @Override
              protected Object makeNewVal(/* layer */ String o) {
                return new PMap<PVertexAttributes, PMap<String, PShader>>() {
                  @Override
                  protected Object makeNewVal(PVertexAttributes o) {
                    return new PMap<String, PShader>();
                  }
                };
              }
            };
          }
        };

    @Getter
    @Setter
    private PShader defaultShader = null;

    // First check if there is a material id specific shader.
    @Override
    public PShader provide(String fragmentLayout,
                           String layer,
                           @NonNull PVertexAttributes vertexAttributes,
                           PMaterial material) {
      String layerId = layer == null ? "" : layer;
      PShader shader =
          fragmentLayoutLayerVertexAttributesMaterialIdMap.gen(fragmentLayout).gen(layerId)
              .gen(vertexAttributes)
              .get(material.getId());

      if (shader != null) {
        return shader;
      }

      shader = fragmentLayoutLayerVertexAttributesMap.gen(fragmentLayout).gen(layerId).get(vertexAttributes);

      if (shader != null) {
        return shader;
      }

      shader = genShader(fragmentLayout, layer, vertexAttributes, material);
      if (shader != null) {
        if (markForAnyMaterialId) {
          PLog.i("PShaderProvider generating new shader any material, original: " + material.getId());
          markForAnyMaterialId = false;
          fragmentLayoutLayerVertexAttributesMap.get(fragmentLayout).gen(layerId)
              .put(vertexAttributes, shader);
        } else {
          PLog.i("PShaderProvider generating new shader for: " + material.getId());
          fragmentLayoutLayerVertexAttributesMaterialIdMap.gen(fragmentLayout).gen(layer == null ? "" : layer)
              .gen(vertexAttributes)
              .put(material.getId(), shader);
        }
        return shader;
      }

      return null;
    }

    private boolean markForAnyMaterialId;

    protected void markForAnyMaterialId() {
      this.markForAnyMaterialId = true;
    }

    public abstract PShader genShader(String fragmentLayout,
                                      String layer,
                                      PVertexAttributes vertexAttributes,
                                      PMaterial material);
  }
}
