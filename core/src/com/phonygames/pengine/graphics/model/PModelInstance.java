package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.math.Matrix4;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PGlDrawCall;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringMap;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;

public class PModelInstance {


  @Getter
  private final PModel model;

  @Getter(lazy = true)
  private final PStringMap<Node> nodes = new PStringMap<>();
  @Getter(lazy = true)
  private final PStringMap<PGlNode> glNodes = new PStringMap<>();
  @Getter(lazy = true)
  private final PList<Node> rootNodes = new PList<>();

  @Getter(lazy = true)
  private final PStringMap<PMaterial> materials = new PStringMap<>();

  @Getter(lazy = true)
  private final PMat4 worldTransform = PMat4.obtain();

  public PModelInstance(PModel model) {
    this.model = model;

    PMap<PModel.Node, Node> childToParentNodeMap = new PMap<>();
    PList<PModel.Node> modelNodesToProcess = new PList<>();
    for (val rootName : model.getRootNodeIds()) {
      modelNodesToProcess.add(model.getNodes().get(rootName));
    }

    while (modelNodesToProcess.size > 0) {
      PModel.Node modelNode = modelNodesToProcess.removeLast();

      Node parent = childToParentNodeMap.get(modelNode);
      Node node = new Node(this, modelNode, parent);
      for (PGlNode glNode : node.getGlNodes()) {
        getGlNodes().put(glNode.getId(), glNode);
      }

      getNodes().put(modelNode.getId(), node);
      if (parent == null) {
        getRootNodes().add(node);
      }

      for (val child : modelNode.getChildren()) {
        modelNodesToProcess.add(child);
        childToParentNodeMap.put(child, node);
      }
    }
  }

  public Node getNode(@NonNull String id) {
    return getNodes().get(id);
  }

  public void enqueue(PRenderContext renderContext, PShaderProvider shaderProvider, PList<PModelInstance> instances) {
    for (PModelInstance.Node node : getRootNodes()) {
      node.enqueueRecursiveInstanced(renderContext, shaderProvider, instances);
    }
  }

  public void recalcTransforms() {
    for (PModelInstance.Node node : getRootNodes()) {
      node.recalcNodeWorldTransformsRecursive(getWorldTransform());
    }

  }

  public class Node {
    @Getter
    final PModel.Node templateNode;
    @Getter(lazy = true)
    private final PMat4 transform = PMat4.obtain();
    @Getter
    final Node parent;
    @Getter(lazy = true)
    private final PList<Node> children = new PList<>();
    @Getter(lazy = true)
    private final PList<PGlNode> glNodes = new PList<>();
    @Getter
    boolean inheritTransform = true, enabled = true;

    @Getter(lazy = true)
    private final PMat4 worldTransform = PMat4.obtain();
    @Getter(lazy = true)
    private final PMat4 worldTransformInvTra = PMat4.obtain();

    @Getter
    final PModelInstance owner;

    private Node(PModelInstance owner, PModel.Node templateNode, Node parent) {
      this.owner = owner;
      this.templateNode = templateNode;
      this.parent = parent;
      this.transform.set(templateNode.getTransform());
      for (PGlNode node : templateNode.getGlNodes()) {
        PGlNode newNode = new PGlNode(node);
        getMaterials().put(newNode.getDrawCall().getMaterial().getId(), newNode.getDrawCall().getMaterial());
        this.getGlNodes().add(newNode);
      }
      if (parent != null) {
        parent.getChildren().add(this);
      }
    }

    public Node setInheritTransform(boolean inheritTransform) {
      this.inheritTransform = inheritTransform;
      return this;
    }

    public Node setEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public String getId() {
      return templateNode.getId();
    }

    // Renders recursively instanced, using the material of the caller.
    public void enqueueRecursiveInstanced(PRenderContext renderContext,
                                          PShaderProvider shaderProvider,
                                          PList<PModelInstance> modelInstances) {
      for (PGlNode node : getGlNodes()) {
        for (PModelInstance modelInstance : modelInstances) {
          PAssert.isTrue(model == modelInstance.model, "Incompatible model type in instances list");
          PGlNode instanceNode = modelInstance.getGlNodes().get(node.getId());
          if (instanceNode.getDataBufferEmitter() != null) {
            instanceNode.getDataBufferEmitter().emitDataBuffersInto(renderContext);
          }

          outputBonesToBuffers(renderContext);
        }

        renderContext
            .enqueue(shaderProvider, PGlDrawCall.getTemp(node.getDrawCall()).setNumInstances(modelInstances.size));
      }

      for (Node child : getChildren()) {
        child.enqueueRecursiveInstanced(renderContext, shaderProvider, modelInstances);
      }
    }

    public void recalcNodeWorldTransformsRecursive(PMat4 parentWorldTransform) {
      if (inheritTransform) {
        getWorldTransform().set(parentWorldTransform).mul(getTransform());
      } else {
        getWorldTransform().set(getTransform());
      }

      getWorldTransformInvTra().set(getWorldTransform()).invTra();

      for (PGlNode node : getGlNodes()) {
        node.setWorldTransform(getWorldTransform(), getWorldTransformInvTra());
      }

      for (Node child : getChildren()) {
        child.recalcNodeWorldTransformsRecursive(getWorldTransform());
      }
    }
  }

  // Returns the new vec4 count in the bone transform buffer.
  private int outputBonesToBuffers(PRenderContext renderContext) {
    PMat4 tempMat4 = PMat4.obtain();
    PFloat4Texture tex = renderContext.genDataBuffer("boneTransforms");
    for (val e : getGlNodes()) {
      String id = e.k();
      PGlNode glNode = e.v();
      if (glNode.getInvBoneTransforms() == null || glNode.getInvBoneTransforms().size == 0) {
        continue;
      }

      renderContext.setVecsPerInstanceForDataBuffer("boneTransforms", glNode.getInvBoneTransforms().size * 4);
      for (val e2 : glNode.getInvBoneTransforms()) {
        String boneId = e2.key;
        PMat4 invBindTransform = e2.value;
        tempMat4.set(getNodes().get(boneId).getWorldTransform());
        tempMat4.mul(invBindTransform);
        tex.addData(tempMat4);
      }
    }
    tempMat4.free();

    return tex.vecsWritten();
  }
}
