package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PGlDrawCall;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.physics.PPhysicsCollisionShape;
import com.phonygames.pengine.physics.collisionshape.PPhysicsBvhTriangleMeshShape;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PModel {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PAnimation> animations = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PGlNode> glNodes = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<Node> nodes = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PList<String> rootNodeIds = new PList<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PStringMap<PPhysicsBvhTriangleMeshShape> staticCollisionShapes = new PStringMap<>();

  private PModel() {
  }

  public void enqueue(@NonNull PRenderContext renderContext, @NonNull PShaderProvider shaderProvider,
                      @NonNull PList<PModelInstance> instances, boolean useMaterialOfFirstInstance) {
    if (instances.size() == 0) {
      return;
    }
    // First, fill up the data buffers.
    for (int a = 0; a < instances.size(); a++) {
      PModelInstance modelInstance = instances.get(a);
      if (modelInstance.getDataBufferEmitter() != null) {
        modelInstance.getDataBufferEmitter().emitDataBuffersInto(renderContext);
      }
    }
    // Next, enqueue the model instance glNodes, filling up the boneTransform buffer for each drawCall/glNode.
    try (val it = nodes().obtainIterator()) {
      while (it.hasNext()) {
        val v = it.next();
        Node node = v.v();
        for (int a = 0; a < v.v().glNodes().size(); a++) {
          PGlNode glNode = v.v().glNodes().get(a);
          // Fill the bone transforms buffer.
          PModelInstance firstInstance = null;
          int currentBoneTransformsOffset = renderContext.boneTransformsBuffer().vecsWritten();
          for (int b = 0; b < instances.size(); b++) {
            PModelInstance modelInstance = instances.get(b);
            PAssert.isTrue(this == modelInstance.model(), "Incompatible model type in instances list");
            modelInstance.outputBoneTransformsToBuffer(renderContext, glNode.id());
            if (firstInstance == null) {
              firstInstance = modelInstance;
            }
          }
          PGlDrawCall drawCall = PGlDrawCall.getTemp(glNode.drawCall()).setNumInstances(instances.size());
          if (useMaterialOfFirstInstance && firstInstance != null) {
            drawCall.material(firstInstance.glNodes().get(glNode.id()).drawCall().material());
          }
          renderContext.enqueue(shaderProvider, drawCall, currentBoneTransformsOffset,
                                Math.max(1 /* If no bones, we still output the world transform. */,
                                         glNode.invBoneTransforms().size) * 4, false);
        }
      }
    }
    // Finally, snapshot the data buffers so that new drawcalls arent reusing the data emitted by this enqueueing.
    renderContext.snapshotBufferOffsets();
  }

  public @Nullable PGlNode getFirstNode() {
    for (int a = 0; a < rootNodeIds().size(); a++) {
      String nodeId = rootNodeIds().get(a);
      val node = nodes().get(nodeId);
      for (int b = 0; b < node.glNodes().size(); b++) {
        PGlNode glNode = node.glNodes().get(b);
        return glNode;
      }
    }
    return null;
  }

  public @Nullable PGlNode glNodeWithId(String id) {
    for (int a = 0; a < rootNodeIds().size(); a++) {
      String nodeId = rootNodeIds().get(a);
      val node = nodes().get(nodeId);
      for (int b = 0; b < node.glNodes().size(); b++) {
        PGlNode glNode = node.glNodes().get(b);
        if (glNode.id().equals(id)) {return glNode;}
      }
    }
    return null;
  }

  // Will use the material of the first instance. So make sure all buffers point to the same source!
  public void render(PList<PModelInstance> instances) {
    for (int a = 0; a < instances.size(); a++) {
      PModelInstance instance = instances.get(a);
      if (instance.model() != this) {
        continue;
      }
    }
    for (int a = 0; a < rootNodeIds().size(); a++) {
      String rootNodeId = rootNodeIds().get(a);
      PModel.Node node = nodes().get(rootNodeId);
    }
  }

  public static class Builder extends PBuilder {
    protected final PModel model = new PModel();

    public Node addNode(String id, Node parent, PList<PGlNode> glNodes, PMat4 transform) {
      checkLock();
      Node node = new Node(id, parent, glNodes);
      node.transform().set(transform).lockWriting();
      if (parent == null) {
        model.rootNodeIds().add(id);
      }
      model.nodes().put(id, node);
      for (int a = 0; a < glNodes.size(); a++) {
        PGlNode glNode = glNodes.get(a);
        model.glNodes().put(glNode.id(), glNode);
      }
      return node;
    }

    public PModel build() {
      lockBuilder();
      processNodes();
      return model;
    }

    private void processNodeRecursive(Node node, @Nullable PMat4 parentTransform) {
      node.modelSpaceTransform().unlockWriting();
      if (parentTransform == null) {
        node.modelSpaceTransform().set(node.transform());
      } else {
        node.modelSpaceTransform().set(parentTransform).mul(node.transform());
      }
      node.modelSpaceTransform().lockWriting();
      node.modelSpaceTransformSet = true;
      for (int a = 0; a < node.children().size(); a++) {
        processNodeRecursive(node.children().get(a), node.modelSpaceTransform());
      }
    }

    private void processNodes() {
      for (int a = 0; a < model.rootNodeIds().size(); a++) {
        Node node = model.nodes().get(model.rootNodeIds().get(a));
        processNodeRecursive(node, null);
      }
    }
  }

  public static class Node {
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PList<Node> children = new PList<>();
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PList<PGlNode> glNodes = new PList<>();
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final String id;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final Node parent;
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PMat4 transform = PMat4.obtain(), modelSpaceTransform = PMat4.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    boolean inheritTransform;
    protected @Nullable PPhysicsCollisionShape physicsCollisionShape;
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PMat4 physicsCollisionShapeOffset = PMat4.obtain();
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private final PMat4 physicsCollisionShapeOffsetInv = PMat4.obtain();
    protected float boneMass = 0;

    private boolean modelSpaceTransformSet = false;

    private Node(String id, Node parent, PList<PGlNode> glNodes) {
      this.id = id;
      this.parent = parent;
      this.glNodes().addAll(glNodes);
      if (parent != null) {
        parent.children().add(this);
      }
      // Dont let the user write to the model space transform; the model builder will set it.
      modelSpaceTransform().lockWriting();
    }



    private boolean hasParent() {
      return parent() != null;
    }
  }
}
