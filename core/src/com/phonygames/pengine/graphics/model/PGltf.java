package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.phonygames.pengine.PAssetManager;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPostableTask;
import com.phonygames.pengine.util.PSet;

import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.model.NodePlus;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import lombok.Getter;
import lombok.val;
import lombok.var;

public class PGltf {
  private final String fileName;

  @Getter
  private PModel model;
  private SceneAsset sceneAsset;

  private static final PMap<String, PGltf> loadedGltfs = new PMap<>();
  private static final PMap<String, PGltf> loadingGltfs = new PMap<>();
  private final PSet<OnloadCallback> onLoadTasks = new PSet<>();
  private final PGltf self = this;

  public static PShaderProvider DEFAULT_SHADER_PROVIDER = new PShaderProvider.PMapShaderProvider() {
    @Override
    public PShader genShader(String fragmentLayout, PVertexAttributes vertexAttributes) {
      return new PShader("#define pbrFlag\n", fragmentLayout, vertexAttributes, Gdx.files.local("engine/shader/gltf/default.vert.glsl"),
                         Gdx.files.local("engine/shader/gltf/default.frag.glsl"));
    }
  };

  @Getter
  private boolean isLoaded;

  public PGltf(String fileName) {
    this.fileName = fileName;
  }

  private final PMap<String, PMesh> meshes = new PMap<>();

  private PMaterial genMaterial(Material material) {
    val ret = new PMaterial(material.id);
    ret.setLayer("PBR");

    for (Attribute attribute : material) {
      ret.set(attribute);
    }
    return ret;
  }

  public static void triggerLoads() {
    val it = loadingGltfs.iterator();
    while (it.hasNext()) {
      val v = it.next();

      String key = v.getKey();
      final PGltf value = v.getValue();
      if (PAssetManager.isLoaded(key)) {
        value.loadFromSceneAsset(PAssetManager.sceneAsset(key));
        loadedGltfs.put(key, value);
        Gdx.app.postRunnable(new Runnable() {
          @Override
          public void run() {
            for (val task : value.onLoadTasks) {
              task.onLoad(value);
            }

            value.onLoadTasks.clear();
          }
        });
        it.remove();
      }
    }
  }

  private void loadFromSceneAsset(SceneAsset sceneAsset) {
    PAssert.isNotNull(sceneAsset);
    PModel.Builder modelBuilder = new PModel.Builder();
    PList<NodePlus> nodesToProcess = new PList<>();
    PMap<NodePlus, PModel.Node> childToParentNodeMap = new PMap<>();

    for (val node : sceneAsset.scene.model.nodes) {
      NodePlus nodePlus = (NodePlus) node;
      nodesToProcess.add(nodePlus);
    }

    PList<PGlNode> glNodes = new PList<>();

    while (nodesToProcess.size() > 0) {
      glNodes.clear();
      NodePlus nodePlus = nodesToProcess.removeLast();
      // Generate meshes if needed.
      if (!nodePlus.parts.isEmpty()) {
        for (val gdxNodePart : nodePlus.parts) {
          PMesh mesh = new PMesh(gdxNodePart.meshPart.mesh);
          PGlNode node = new PGlNode(gdxNodePart.meshPart.id);
          node.setMesh(mesh);
          node.setMaterial(genMaterial(gdxNodePart.material));
          glNodes.add(node);
        }
      }

      PModel.Node node = modelBuilder.addNode(nodePlus.id, childToParentNodeMap.get(nodePlus), glNodes, new PMat4().set(nodePlus.translation, nodePlus.rotation, nodePlus.scale));
      node.setInheritTransform(nodePlus.inheritTransform);

      for (val child : nodePlus.getChildren()) {
        nodesToProcess.add((NodePlus) child);
        childToParentNodeMap.put((NodePlus) child, node);
      }
    }

    model = modelBuilder.build();
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
