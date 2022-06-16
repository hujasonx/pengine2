package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.utils.Array;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.animation.PNodeAnimation;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.shared.animation.Interpolation;
import net.mgsx.gltf.scene3d.animation.NodeAnimationHack;
import net.mgsx.gltf.scene3d.model.NodePlus;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import lombok.val;

public class PGLBAssetLoader extends AsynchronousAssetLoader<PModel, PGLBAssetLoader.LoaderParameters> {
  public PGLBAssetLoader() {
    this(new InternalFileHandleResolver());
  }

  public PGLBAssetLoader(FileHandleResolver resolver) {
    super(resolver);
  }

  @Override
  public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, LoaderParameters parameter) {
    return null;
  }

  @Override public void loadAsync(AssetManager manager, String fileName, FileHandle file, LoaderParameters parameter) {
  }

  @Override public PModel loadSync(AssetManager manager, String fileName, FileHandle file, LoaderParameters parameter) {
    SceneAsset sceneAsset = new GLBLoader().load(file, true);
    PModel model;
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
          node.drawCall().setMesh(mesh);
          node.drawCall().setMaterial(genMaterial(gdxNodePart.material));
          node.drawCall().setLayer("PBR");
          if (gdxNodePart.invBoneBindTransforms != null) {
            for (val invBoneBT : gdxNodePart.invBoneBindTransforms) {
              node.invBoneTransforms().put(invBoneBT.key.id, PMat4.obtain(invBoneBT.value.val));
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
      node.inheritTransform(nodePlus.inheritTransform);
      for (val child : nodePlus.getChildren()) {
        nodesToProcess.add((NodePlus) child);
        childToParentNodeMap.put((NodePlus) child, node);
      }
    }
    model = modelBuilder.build();
    loadAnimationsFromAsset(sceneAsset, model);
    return model;
  }

  private PMaterial genMaterial(Material material) {
    val ret = new PMaterial(material.id, null);
    for (Attribute attribute : material) {
      ret.set(attribute);
    }
    return ret;
  }

  private static void loadAnimationsFromAsset(SceneAsset asset, PModel model) {
    for (Animation baseAnim : asset.animations) {
      PAnimation.Builder builder = new PAnimation.Builder(baseAnim.id);
      for (val e : baseAnim.nodeAnimations) {
        val nodeAnimHack = (NodeAnimationHack) e;
        PNodeAnimation.Builder nodeBuilder = new PNodeAnimation.Builder(nodeAnimHack.node.id);
        nodeBuilder.setDefaultTRS(e.node.translation, e.node.rotation, e.node.scale);
        if (e.translation != null) {
          nodeBuilder.setInterpolationTranslation(convertInterpolation(nodeAnimHack.translationMode));
          for (val t : e.translation) {
            nodeBuilder.addTranslationKeyframe(t.keytime, PVec3.obtain().set(t.value));
          }
        }
        if (e.rotation != null) {
          nodeBuilder.setInterpolationRotation(convertInterpolation(nodeAnimHack.rotationMode));
          for (val t : e.rotation) {
            nodeBuilder.addRotationKeyframe(t.keytime, PVec4.obtain().set(t.value));
          }
        }
        if (e.scaling != null) {
          nodeBuilder.setInterpolationScale(convertInterpolation(nodeAnimHack.scalingMode));
          for (val t : e.scaling) {
            nodeBuilder.addScaleKeyframe(t.keytime, PVec3.obtain().set(t.value));
          }
        }
        builder.addNodeAnimation(nodeBuilder.build());
      }
      model.animations().put(baseAnim.id, builder.build());
    }
  }

  private static PNodeAnimation.Interpolation convertInterpolation(Interpolation i) {
    if (i == null) {
      return PNodeAnimation.Interpolation.STEP;
    }
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

  private void loadFromSceneAsset(SceneAsset sceneAsset) {
    PAssert.isNotNull(sceneAsset);
  }

  public static class LoaderParameters extends AssetLoaderParameters<PModel> {}
}
