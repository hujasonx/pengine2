package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.physics.PPhysicsEngine;
import com.phonygames.pengine.physics.PStaticBody;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PModelInstance {
  private static final PList<PModelInstance> tempModelInstanceList = new PList<>();
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
  private final PStringMap<PStaticBody> staticBodies = new PStringMap<>();
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

  public PModelInstance copySameNameBoneTransformsToWithRoot(PModelInstance modelInstance, String rootName) {
    Node node = nodes().get(rootName);
    Node otherNode = modelInstance.nodes().get(rootName);
    if (node == null || otherNode == null) {return this;}
    node.copySameNameBoneTransformsToRecursive(modelInstance, true);
    return this;
  }

  public PModelInstance createAndAddStaticBodiesFromModelWithCurrentWorldTransform() {
    try (val it = model.staticCollisionShapes().obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        if (staticBodies().has(e.k())) {continue;}
        PStaticBody staticBody = PStaticBody.obtain(e.v(), PPhysicsEngine.STATIC_FLAG, PPhysicsEngine.ALL_FLAG);
        staticBody.setWorldTransform(worldTransform());
        staticBodies().put(e.k(), staticBody);
        staticBody.addToDynamicsWorld();
      }
    }
    return this;
  }

  public void enqueue(PRenderContext renderContext, PShaderProvider shaderProvider) {
    tempModelInstanceList.clear();
    tempModelInstanceList.add(this);
    model().enqueue(renderContext, shaderProvider, tempModelInstanceList, true);
  }

  public Node getNode(@NonNull String id) {
    return nodes().get(id);
  }

  public @Nullable PMaterial material(@NonNull String name) {
    return this.materials().get(name);
  }

  protected boolean outputBoneTransformsToBuffer(PRenderContext renderContext, String glNodeID) {
    PMat4 tempMat4 = PMat4.obtain();
    PGlNode glNode = glNodes().get(glNodeID);
    if (glNode.invBoneTransforms() == null || glNode.invBoneTransforms().size == 0) {
      // If there are no inverse bone transforms, simply output the raw transform.
      PAssert.isNotNull(glNode.ownerModelInstanceNode(), "glNode had no PModelInstance.Node associated with it");
      tempMat4.set(glNode.ownerModelInstanceNode().worldTransform());
      renderContext.boneTransformsBuffer().addData(tempMat4);
      tempMat4.free();
      return false;
    }
    for (val e2 : glNode.invBoneTransforms()) {
      String boneId = e2.key;
      PMat4 invBindTransform = e2.value;
      tempMat4.set(nodes().get(boneId).worldTransform());
      tempMat4.mul(invBindTransform);
      renderContext.boneTransformsBuffer().addData(tempMat4);
    }
    tempMat4.free();
    return true;
  }

  /**
   * Outputs the node transform values into the map.
   * @param map
   * @param useBindPose If set, use the bind pose instead of the actual pose.
   * @param alpha
   * @return
   */
  public PStringMap<PMat4> outputNodeTransformsToMap(final PStringMap<PMat4> map, boolean useBindPose, float alpha) {
    try (val it = nodes().obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        if (map.has(e.k())) {
          // There was already a maxtrix in the map for this node.
          PMat4 mat4 = map.get(e.k());
          mat4.lerp(useBindPose ? e.v().templateNode().transform() : e.v().transform(), alpha);
        } else {
          // Put in a new matrix into the map for this node, ignoring alpha.
          PMat4 mat4 = map.genPooled(e.k());
          mat4.set(useBindPose ? e.v().templateNode().transform() : e.v().transform());
        }
      }
    }
    return map;
  }

  public void recalcTransforms() {
    for (PModelInstance.Node node : rootNodes()) {
      node.recalcNodeWorldTransformsRecursive(worldTransform(), false);
    }
  }

  public void resetTransformsFromTemplates() {
    try (val it = nodes().obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        e.v().resetTransformFromTemplate();
      }
    }
  }

  /**
   * Sets the node transform values from the values in the map.
   * @param map
   * @param alpha how much to affect the transforms by.
   * @return
   */
  public PStringMap<PMat4> setNodeTransformsFromMap(PStringMap<PMat4> map, float alpha) {
    try (val it = map.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        if (nodes().has(e.k())) {
          if (alpha == 1) {
            nodes().get(e.k()).transform().set(e.v());
          } else {
            nodes().get(e.k()).transform().scl(1 - alpha).mulAdd(e.v(), alpha);
          }
        }
      }
    }
    return map;
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
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    // If Set, this node will not recurse on its children when recalculating world transforms.
    boolean stopWorldTransformRecursionAt = false;

    private Node(PModelInstance owner, PModel.Node templateNode, Node parent) {
      this.owner = owner;
      this.templateNode = templateNode;
      this.parent = parent;
      transform().set(templateNode.transform());
      for (PGlNode node : templateNode.glNodes()) {
        PGlNode newNode = node.deepCopy();
        newNode.ownerModelInstanceNode(this);
        materials().put(newNode.drawCall().material().id(), newNode.drawCall().material());
        this.glNodes().add(newNode);
      }
      if (parent != null) {
        parent.children().add(this);
      }
    }

    /**
     * @param other
     * @param isRoot If set, this will calculate the transform of the other node necessary to reach the desired
     *               transform.
     */
    public void copySameNameBoneTransformsToRecursive(PModelInstance other, boolean isRoot) {
      if (other.nodes().has(templateNode().id())) {
        Node otherNode = other.nodes().get(templateNode().id());
        otherNode.worldTransform().set(worldTransform());
        other.nodes().get(templateNode().id()).worldTransformInvTra().set(worldTransformInvTra());
        if (isRoot) {
          System.out.println("Root Copying: " + templateNode().id());
        } else {
          System.out.println("\tCopying: " + templateNode().id());
        }
        if (!otherNode.inheritTransform) {
          // If the other node does not inherit transforms, set the transform directly from the world transform.
          otherNode.transform().set(worldTransform());
        } else if (isRoot && otherNode.inheritTransform) {
          // If this is a root node in the model, then recalc the transform given the other model's parent.
          PMat4 invOtherNodeParentTransform = PMat4.obtain();
          if (otherNode.parent() != null) {
            invOtherNodeParentTransform.set(otherNode.parent().worldTransform());
          } else {
            invOtherNodeParentTransform.set(other.worldTransform());
          }
          otherNode.transform().set(worldTransform()).mulLeft(invOtherNodeParentTransform.inv());
        } else {
          otherNode.transform().set(transform());
        }
        // Recurse.
        for (int a = 0; a < children().size; a++) {
          children().get(a).copySameNameBoneTransformsToRecursive(other, false);
        }
      }
    }

    public String id() {
      return templateNode.id();
    }

    public void recalcNodeWorldTransformsRecursive(PMat4 parentWorldTransform, boolean forceRecursionIfFIrst) {
      if (inheritTransform) {
        worldTransform().set(parentWorldTransform).mul(transform());
      } else {
        worldTransform().set(transform());
      }
      worldTransformInvTra().set(worldTransform()).invTra();
      for (PGlNode node : glNodes()) {
        node.setWorldTransform(worldTransform(), worldTransformInvTra());
      }
      if (stopWorldTransformRecursionAt && !forceRecursionIfFIrst) {return;}
      for (Node child : children()) {
        child.recalcNodeWorldTransformsRecursive(worldTransform(), false);
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
