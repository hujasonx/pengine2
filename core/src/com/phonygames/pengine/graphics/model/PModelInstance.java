package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.texture.PFloat4Texture;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PModelInstance {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PGlNode> glNodes = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PMaterial> materials = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PModel model;
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<Node> nodes = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PList<Node> rootNodes = new PList<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PMat4 worldTransform = PMat4.obtain();
  @Getter
  @Setter
  // You can hook swap this out whenever you like.
  private PRenderContext.DataBufferEmitter dataBufferEmitter;

  public PModelInstance(PModel model) {
    this.model = model;
    PMap<PModel.Node, Node> childToParentNodeMap = new PMap<>();
    PList<PModel.Node> modelNodesToProcess = new PList<>();
    for (val rootName : model.rootNodeIds()) {
      modelNodesToProcess.add(model.nodes().get(rootName));
    }
    while (modelNodesToProcess.size > 0) {
      PModel.Node modelNode = modelNodesToProcess.removeLast();
      Node parent = childToParentNodeMap.get(modelNode);
      Node node = new Node(this, modelNode, parent);
      for (PGlNode glNode : node.glNodes()) {
        glNodes().put(glNode.id(), glNode);
      }
      nodes().put(modelNode.id(), node);
      if (parent == null) {
        rootNodes().add(node);
      }
      for (val child : modelNode.children()) {
        modelNodesToProcess.add(child);
        childToParentNodeMap.put(child, node);
      }
    }
  }

  public Node getNode(@NonNull String id) {
    return nodes().get(id);
  }

  // Returns the new vec4 count in the bone transform buffer.
  protected int outputBoneTransformsToBuffer(PRenderContext renderContext) {
    PMat4 tempMat4 = PMat4.obtain();
    PFloat4Texture tex = renderContext.genDataBuffer("boneTransforms");
    for (val e : glNodes()) {
      String id = e.k();
      PGlNode glNode = e.v();
      if (glNode.invBoneTransforms() == null || glNode.invBoneTransforms().size == 0) {
        continue;
      }
      for (val e2 : glNode.invBoneTransforms()) {
        String boneId = e2.key;
        PMat4 invBindTransform = e2.value;
        tempMat4.set(nodes().get(boneId).worldTransform());
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
    for (val e : nodes()) {
      PMat4 mat4 = map.genPooled(e.k());
      if (useBindPose) {
        mat4.set(e.v().templateNode().transform());
      } else {
        mat4.set(e.v().transform());
      }
      mat4.scl(weight);
    }
    return map;
  }

  public void recalcTransforms() {
    for (PModelInstance.Node node : rootNodes()) {
      node.recalcNodeWorldTransformsRecursive(worldTransform());
    }
  }

  public class Node {
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    final PModelInstance owner;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    final Node parent;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    final PModel.Node templateNode;
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PList<Node> children = new PList<>();
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PList<PGlNode> glNodes = new PList<>();
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PMat4 transform = PMat4.obtain();
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PMat4 worldTransform = PMat4.obtain();
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PMat4 worldTransformInvTra = PMat4.obtain();
    @Getter
    boolean inheritTransform = true, enabled = true;

    private Node(PModelInstance owner, PModel.Node templateNode, Node parent) {
      this.owner = owner;
      this.templateNode = templateNode;
      this.parent = parent;
      this.transform.set(templateNode.transform());
      for (PGlNode node : templateNode.glNodes()) {
        PGlNode newNode = node.deepCopy();
        materials().put(newNode.drawCall().material().id(), newNode.drawCall().material());
        this.glNodes().add(newNode);
      }
      if (parent != null) {
        parent.children().add(this);
      }
    }

    public String id() {
      return templateNode.id();
    }

    public void recalcNodeWorldTransformsRecursive(PMat4 parentWorldTransform) {
      if (inheritTransform) {
        worldTransform().set(parentWorldTransform).mul(transform());
      } else {
        worldTransform().set(transform());
      }
      worldTransformInvTra().set(worldTransform()).invTra();
      for (PGlNode node : glNodes()) {
        node.setWorldTransform(worldTransform(), worldTransformInvTra());
      }
      for (Node child : children()) {
        child.recalcNodeWorldTransformsRecursive(worldTransform());
      }
    }

    public void resetTransformFromTemplate() {
      transform().set(templateNode.transform());
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
