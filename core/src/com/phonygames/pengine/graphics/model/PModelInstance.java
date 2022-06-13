package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
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
  @Getter(lazy = true)
  private final PStringMap<PGlNode> glNodes = new PStringMap<>();
  @Getter(lazy = true)
  private final PStringMap<PMaterial> materials = new PStringMap<>();
  @Getter
  private final PModel model;
  @Getter(lazy = true)
  private final PStringMap<Node> nodes = new PStringMap<>();
  @Getter(lazy = true)
  private final PList<Node> rootNodes = new PList<>();
  @Getter(lazy = true)
  private final PMat4 worldTransform = PMat4.obtain();
  @Getter
  @Setter
  // You can hook swap this out whenever you like.
  private PRenderContext.DataBufferEmitter dataBufferEmitter;

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

  // Returns the new vec4 count in the bone transform buffer.
  protected int outputBoneTransformsToBuffer(PRenderContext renderContext) {
    PMat4 tempMat4 = PMat4.obtain();
    PFloat4Texture tex = renderContext.genDataBuffer("boneTransforms");
    for (val e : getGlNodes()) {
      String id = e.k();
      PGlNode glNode = e.v();
      if (glNode.getInvBoneTransforms() == null || glNode.getInvBoneTransforms().size == 0) {
        continue;
      }
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

  /**
   * Outputs the PMat4 references into the map.
   * @param map
   * @param useBindPose If set, use the bind pose instead of the actual pose.
   * @param weight
   * @return
   */
  public PStringMap<PMat4> outputNodeTransformsToMap(PStringMap<PMat4> map, boolean useBindPose, float weight) {
    for (val e : getNodes()) {
      PMat4 mat4 = map.genPooled(e.k());
      if (useBindPose) {
        mat4.set(e.v().getTemplateNode().getTransform());
      } else {
        mat4.set(e.v().getTransform());
      }
      mat4.scl(weight);
    }
    return map;
  }

  public void recalcTransforms() {
    for (PModelInstance.Node node : getRootNodes()) {
      node.recalcNodeWorldTransformsRecursive(getWorldTransform());
    }
  }

  public class Node {
    @Getter
    final PModelInstance owner;
    @Getter
    final Node parent;
    @Getter
    final PModel.Node templateNode;
    @Getter(lazy = true)
    private final PList<Node> children = new PList<>();
    @Getter(lazy = true)
    private final PList<PGlNode> glNodes = new PList<>();
    @Getter(lazy = true)
    private final PMat4 transform = PMat4.obtain();
    @Getter(lazy = true)
    private final PMat4 worldTransform = PMat4.obtain();
    @Getter(lazy = true)
    private final PMat4 worldTransformInvTra = PMat4.obtain();
    @Getter
    boolean inheritTransform = true, enabled = true;

    private Node(PModelInstance owner, PModel.Node templateNode, Node parent) {
      this.owner = owner;
      this.templateNode = templateNode;
      this.parent = parent;
      this.transform.set(templateNode.getTransform());
      for (PGlNode node : templateNode.getGlNodes()) {
        PGlNode newNode = node.deepCopy();
        getMaterials().put(newNode.getDrawCall().getMaterial().getId(), newNode.getDrawCall().getMaterial());
        this.getGlNodes().add(newNode);
      }
      if (parent != null) {
        parent.getChildren().add(this);
      }
    }

    public String getId() {
      return templateNode.getId();
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

    public void resetTransformFromTemplate() {
      getTransform().set(templateNode.getTransform());
    }

    public Node setEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Node setInheritTransform(boolean inheritTransform) {
      this.inheritTransform = inheritTransform;
      return this;
    }
  }
}
