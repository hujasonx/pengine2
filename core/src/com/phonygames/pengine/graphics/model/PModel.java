package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PGlDrawCall;
import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.math.PMat4;
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
  private final PStringMap<Node> nodes = new PStringMap<>();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PList<String> rootNodeIds = new PList<>();

  private PModel() {
  }

  // Will use the material of the first instance. So make sure all buffers point to the same source!
  public void render(PList<PModelInstance> instances) {
    for (PModelInstance instance : instances) {
      if (instance.model() != this) {
        continue;
      }
    }
    for (String rootNodeId : rootNodeIds()) {
      PModel.Node node = nodes().get(rootNodeId);
    }
  }

  public void enqueue(@NonNull PRenderContext renderContext, @NonNull PShaderProvider shaderProvider, @NonNull PList<PModelInstance> instances) {
    PModelInstance firstInstance = null;
    // First, fill up the data buffers.
    for (PModelInstance modelInstance : instances) {
      if (firstInstance == null) {
        firstInstance = modelInstance;
      }
      if (modelInstance.getDataBufferEmitter() != null) {
        modelInstance.getDataBufferEmitter().emitDataBuffersInto(renderContext);
      }
    }

    // Next, enqueue the model instance nodes.
    if (firstInstance != null) {
      for (PModelInstance.Node node : firstInstance.rootNodes()) {
        enqueueModelInstanceNodeRecursiveInstanced(node, renderContext, shaderProvider, instances);
      }
    }
  }

  // Renders recursively instanced, using the material of the caller.
  public void enqueueModelInstanceNodeRecursiveInstanced(@NonNull PModelInstance.Node modelInstanceNode, @NonNull PRenderContext renderContext, @NonNull PShaderProvider shaderProvider,
                                        @NonNull PList<PModelInstance> modelInstances) {
    for (PGlNode node : modelInstanceNode.glNodes()) {
      // Emit all the model instance bone transforms for the node.
      int currentBoneTransformsOffset = renderContext.boneTransformsBuffer().vecsWritten();
      for (PModelInstance modelInstance : modelInstances) {
        PAssert.isTrue(this == modelInstance.model(), "Incompatible model type in instances list");
        modelInstance.outputBoneTransformsToBuffer(renderContext);
      }

      // Now that all the bone and data buffers have been set, enqueue a draw call. Since snapshotBufferOffsets was
      // called, we will need to re-output all buffer data
      renderContext.enqueue(shaderProvider,
                            PGlDrawCall.getTemp(node.drawCall()).setNumInstances(modelInstances.size), currentBoneTransformsOffset);
    }

    for (PModelInstance.Node child : modelInstanceNode.children()) {
      enqueueModelInstanceNodeRecursiveInstanced(child, renderContext, shaderProvider, modelInstances);
    }
  }

  public static class Builder extends PBuilder {
    private PModel model = new PModel();

    public Node addNode(String id, Node parent, PList<PGlNode> glNodes, PMat4 transform) {
      checkLock();
      Node node = new Node(id, parent, glNodes);
      node.transform().set(transform);
      if (parent == null) {
        model.rootNodeIds().add(id);
      }
      model.nodes().put(id, node);
      return node;
    }

    public PModel build() {
      lockBuilder();
      return model;
    }
  }

  protected static class Node {
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
    private final PMat4 transform = PMat4.obtain();
    @Getter(value = AccessLevel.PUBLIC)
    @Setter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    boolean inheritTransform;

    private Node(String id, Node parent, PList<PGlNode> glNodes) {
      this.id = id;
      this.parent = parent;
      this.glNodes().addAll(glNodes);
      if (parent != null) {
        parent.children().add(this);
      }
    }

    private boolean hasParent() {
      return parent() != null;
    }
  }
}
