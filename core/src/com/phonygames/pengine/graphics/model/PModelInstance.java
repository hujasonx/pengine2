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

  private final PStringMap<Node> nodes = new PStringMap<>();
  private final PStringMap<PGlNode> glNodes = new PStringMap<>();
  private final PList<Node> rootNodes = new PList<>();

  @Getter
  private final PStringMap<PMaterial> materials = new PStringMap<>();

  @Getter
  private final PMat4 worldTransform = new PMat4();

  public PModelInstance(PModel model) {
    this.model = model;

    PMap<PModel.Node, Node> childToParentNodeMap = new PMap<>();
    PList<PModel.Node> modelNodesToProcess = new PList<>();
    for (val rootName : model.rootNodeIds) {
      modelNodesToProcess.add(model.nodes.get(rootName));
    }

    while (modelNodesToProcess.size > 0) {
      PModel.Node modelNode = modelNodesToProcess.removeLast();

      Node parent = childToParentNodeMap.get(modelNode);
      Node node = new Node(this, modelNode, parent);
      for (val glNode : node.glNodes) {
        glNodes.put(glNode.getId(), glNode);
      }

      nodes.put(modelNode.id, node);
      if (parent == null) {
        rootNodes.add(node);
      }

      for (val child : modelNode.getChildren()) {
        modelNodesToProcess.add(child);
        childToParentNodeMap.put(child, node);
      }
    }
  }

  public Node getNode(@NonNull String id) {
    return nodes.get(id);
  }

  public void enqueue(PRenderContext renderContext, PShaderProvider shaderProvider, PList<PModelInstance> instances) {
    for (val node : rootNodes) {
      node.enqueueRecursiveInstanced(renderContext, shaderProvider, instances);
    }
  }

  public void recalcTransforms() {
    for (val node : rootNodes) {
      node.recalcNodeWorldTransformsRecursive(worldTransform);
    }

  }

  public class Node {
    @Getter
    final PModel.Node templateNode;
    @Getter
    final PMat4 transform = new PMat4();
    @Getter
    final Node parent;
    @Getter
    final PList<Node> children = new PList<>();
    final PList<PGlNode> glNodes = new PList<>();
    @Getter
    boolean inheritTransform = true, enabled = true;

    @Getter
    final PMat4 worldTransform = new PMat4();
    @Getter
    final PMat4 worldTransformInvTra = new PMat4();

    @Getter
    final PModelInstance owner;

    private Node(PModelInstance owner, PModel.Node templateNode, Node parent) {
      this.owner = owner;
      this.templateNode = templateNode;
      this.parent = parent;
      this.transform.set(templateNode.transform);
      for (PGlNode node : templateNode.glNodes) {
        PGlNode newNode = new PGlNode(node);
        materials.put(newNode.getDrawCall().getMaterial().getId(), newNode.getDrawCall().getMaterial());
        this.glNodes.add(newNode);
      }
      if (parent != null) {
        parent.children.add(this);
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
      return templateNode.id;
    }

    // Renders recursively instanced, using the material of the caller.
    public void enqueueRecursiveInstanced(PRenderContext renderContext,
                                          PShaderProvider shaderProvider,
                                          PList<PModelInstance> modelInstances) {
      for (val node : glNodes) {
        for (PModelInstance modelInstance : modelInstances) {
          PAssert.isTrue(model == modelInstance.model, "Incompatible model type in instances list");
          val instanceNode = modelInstance.glNodes.get(node.getId());
          if (instanceNode.getDataBufferEmitter() != null) {
            instanceNode.getDataBufferEmitter().emitDataBuffersInto(renderContext);
          }

          outputBonesToBuffers(renderContext);
        }

        renderContext
            .enqueue(shaderProvider, PGlDrawCall.getTemp(node.getDrawCall()).setNumInstances(modelInstances.size));
      }

      for (Node child : children) {
        child.enqueueRecursiveInstanced(renderContext, shaderProvider, modelInstances);
      }
    }

    public void recalcNodeWorldTransformsRecursive(PMat4 parentWorldTransform) {
      if (inheritTransform) {
        worldTransform.set(parentWorldTransform).mul(transform);
      } else {
        worldTransform.set(transform);
      }

      worldTransformInvTra.set(worldTransform).invTra();

      for (PGlNode node : glNodes) {
        node.setWorldTransform(worldTransform, worldTransformInvTra);
      }

      for (Node child : children) {
        child.recalcNodeWorldTransformsRecursive(worldTransform);
      }
    }
  }

  // Returns the new vec4 count in the bone transform buffer.
  private static final PMat4 tempMat4 = new PMat4();

  private int outputBonesToBuffers(PRenderContext renderContext) {
    PFloat4Texture tex = renderContext.genDataBuffer("boneTransforms");
    for (val e : glNodes) {
      String id = e.k();
      PGlNode glNode = e.v();
      if (glNode.getInvBoneTransforms() == null || glNode.getInvBoneTransforms().size == 0) {
        continue;
      }

      renderContext.setVecsPerInstanceForDataBuffer("boneTransforms", glNode.getInvBoneTransforms().size * 4);
      for (val e2 : glNode.getInvBoneTransforms()) {
        String boneId = e2.key;
        PMat4 invBindTransform = e2.value;
        tempMat4.set(nodes.get(boneId).worldTransform);
        tempMat4.mul(invBindTransform);
        tex.addData(tempMat4);
      }
    }

    return tex.vecsWritten();
  }
}
