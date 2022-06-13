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
        return new PShader("", fragmentLayout, vertexAttributes,
                           Gdx.files.local("engine/shader/gltf/default.vert.glsl"),
                           Gdx.files.local("engine/shader/gltf/default.frag.glsl"));
      } else {
        PLog.w("PShaderProvider Unsupported layer: " + layer);
        return null;
      }
    }
  };
  private final String fileName;
  private final PStringMap<PMesh> meshes = new PStringMap<>();
  private final PSet<OnloadCallback> onLoadTasks = new PSet<>();
  private final PGltf self = this;
  private SceneAsset backingSceneAsset;
  @Getter(value = AccessLevel.PRIVATE)
  private boolean isLoaded;
  @Getter(value = AccessLevel.PUBLIC)
  private PModel model;

  public PGltf(String fileName) {
    this.fileName = fileName;
  }

  public static void triggerLoads() {
    PList<String> keysToRemove = null;
    for (val e : loadingGltfs) {
      String key = e.k();
      final PGltf value = e.v();
      if (PAssetManager.isLoaded(key)) {
        value.loadFromSceneAsset(PAssetManager.sceneAsset(key));
        loadedGltfs.put(key, value);
        Gdx.app.postRunnable(new Runnable() {
          @Override public void run() {
            for (val task : value.onLoadTasks) {
              task.onLoad(value);
            }
            value.onLoadTasks.clear();
          }
        });
        if (keysToRemove == null) {
          keysToRemove = new PList<>();
          keysToRemove.add(key);
        }
      }
    }
    if (keysToRemove != null) {
      loadingGltfs.delAll(keysToRemove);
    }
  }

  private PMaterial genMaterial(Material material) {
    val ret = new PMaterial(material.id, null);
    for (Attribute attribute : material) {
      ret.set(attribute);
    }
    return ret;
  }

  private void loadFromSceneAsset(SceneAsset sceneAsset) {
    PAssert.isNotNull(sceneAsset);
    backingSceneAsset = sceneAsset;
    PModel.Builder modelBuilder = new PModel.Builder();
    PList<NodePlus> nodesToProcess = new PList<>();
    PMap<NodePlus, PModel.Node> childToParentNodeMap = new PMap<>();
    for (val node : sceneAsset.scene.model.nodes) {
      NodePlus nodePlus = (NodePlus) node;
      nodesToProcess.add(nodePlus);
    }
    PList<PGlNode> glNodes = new PList<>();
    while (nodesToProcess.size > 0) {
      glNodes.clear();
      NodePlus nodePlus = nodesToProcess.removeLast();
      // Generate meshes if needed.
      if (!nodePlus.parts.isEmpty()) {
        for (val gdxNodePart : nodePlus.parts) {
          PMesh mesh = new PMesh(gdxNodePart.meshPart.mesh);
          PGlNode node = new PGlNode(gdxNodePart.meshPart.id);
          node.getDrawCall().setMesh(mesh);
          node.getDrawCall().setMaterial(genMaterial(gdxNodePart.material));
          node.getDrawCall().setLayer("PBR");
          if (gdxNodePart.invBoneBindTransforms != null) {
            for (val invBoneBT : gdxNodePart.invBoneBindTransforms) {
              node.getInvBoneTransforms().put(invBoneBT.key.id, PMat4.obtain(invBoneBT.value.val));
            }
          }
          glNodes.add(node);
        }
      }
      PModel.Node node = modelBuilder.addNode(nodePlus.id, childToParentNodeMap.get(nodePlus), glNodes, PMat4.obtain()
                                                                                                             .set(
                                                                                                                 nodePlus.translation,
                                                                                                                 nodePlus.rotation,
                                                                                                                 nodePlus.scale));
      node.setInheritTransform(nodePlus.inheritTransform);
      for (val child : nodePlus.getChildren()) {
        nodesToProcess.add((NodePlus) child);
        childToParentNodeMap.put((NodePlus) child, node);
      }
    }
    model = modelBuilder.build();
    loadAnimationsFromAsset(sceneAsset, model);
  }

  private static void loadAnimationsFromAsset(SceneAsset asset, PModel model) {
    for (Animation baseAnim : asset.animations) {
      PAnimation.Builder builder = new PAnimation.Builder(baseAnim.id);
      for (val e : baseAnim.nodeAnimations) {
        val nodeAnimHack = (NodeAnimationHack) e;
        PNodeAnimation.Builder nodeBuilder = new PNodeAnimation.Builder(nodeAnimHack.node.id);
        nodeBuilder.setInterpolationTranslation(convertInterpolation(nodeAnimHack.translationMode));
        nodeBuilder.setInterpolationScale(convertInterpolation(nodeAnimHack.scalingMode));
        nodeBuilder.setInterpolationRotation(convertInterpolation(nodeAnimHack.rotationMode));
        for (val t : e.translation) {
          nodeBuilder.addTranslationKeyframe(t.keytime, PVec3.obtain().set(t.value));
        }
        for (val r : e.rotation) {
          nodeBuilder.addRotationKeyframe(r.keytime, PVec4.obtain().set(r.value));
        }
        for (val s : e.scaling) {
          nodeBuilder.addScaleKeyframe(s.keytime, PVec3.obtain().set(s.value));
        }
        builder.addNodeAnimation(nodeBuilder.build());
      }

      model.getAnimations().put(baseAnim.id, builder.build());
    }
  }

  private static PNodeAnimation.Interpolation convertInterpolation(Interpolation i) {
    switch (i) {
      case STEP:
        return PNodeAnimation.Interpolation.STEP;
      case CUBICSPLINE:
        return PNodeAnimation.Interpolation.CUBIC;
      case LINEAR:
      default:
        return PNodeAnimation.Interpolation.LINEAR;
    }
  }

  public PGltf loadThenDo(OnloadCallback runnable) {
    if (!isLoaded()) {
      onLoadTasks.add(runnable);
      loadingGltfs.put(fileName, this);
    } else {
      runnable.onLoad(this);
    }
    return this;
  }

  public static abstract class OnloadCallback {
    public abstract void onLoad(PGltf gltf);
  }
}
