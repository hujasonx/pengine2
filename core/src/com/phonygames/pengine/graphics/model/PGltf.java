package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.animation.PNodeAnimation;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PSet;
import com.phonygames.pengine.util.PStringMap;

import net.mgsx.gltf.loaders.shared.animation.Interpolation;
import net.mgsx.gltf.scene3d.animation.NodeAnimationHack;
import net.mgsx.gltf.scene3d.model.NodePlus;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;

public class PGltf {
  private static final PStringMap<PGltf> loadedGltfs = new PStringMap<>();
  private static final PStringMap<PGltf> loadingGltfs = new PStringMap<>();
  public static PShaderProvider DEFAULT_SHADER_PROVIDER = new PShaderProvider.PMapShaderProvider() {
    @Override public PShader genShader(String fragmentLayout, String layer, PVertexAttributes vertexAttributes,
                                       PMaterial material) {
      if ("PBR".equals(layer)) {
        markForAnyMaterialId();
        return new PShader("", fragmentLayout, vertexAttributes,
                           Gdx.files.local("engine/shader/gltf/default.vert.glsl"),
                           Gdx.files.local("engine/shader/gltf/default.frag.glsl"));
      } else {
        PLog.w("PShaderProvider Unsupported layer: " + layer);
        return null;
      }
    }
  };
}
