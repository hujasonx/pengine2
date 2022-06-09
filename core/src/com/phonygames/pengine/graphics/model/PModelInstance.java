package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PModelInstance {
  @Getter
  private final PModel model;

  @Getter
  private final PMap<String, Node> nodes = new PMap<>();
  @Getter
  private final PMap<String, PGlNode> glNodes = new PMap<>();
  private final PList<Node> rootNodes = new PList<>();

  @Getter
  private final PMap<String, PMaterial> materials = new PMap<>();

  @Getter
  private final PMat4 worldTransform = new PMat4();

  public PModelInstance(PModel model) {
    this(model, null);
  }

  public PModelInstance(PModel model, PShaderProvider defaultShaderProvider) {
    this.model = model;

    PMap<PModel.Node, Node> childToParentNodeMap = new PMap<>();
    PList<PModel.Node> modelNodesToProcess = new PList<>();
    for (val rootName : model.rootNodeIds) {
      modelNodesToProcess.add(model.nodes.get(rootName));

    }

    while (modelNodesToProcess.size() > 0) {
      PModel.Node modelNode = modelNodesToProcess.removeLast();

      Node parent = childToParentNodeMap.get(modelNode);
      Node node = new Node(modelNode, parent, defaultShaderProvider);
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

  public void renderDefault(PRenderContext renderContext) {
    for (val node : rootNodes) {
      node.recalcNodeWorldTransformsRecursive(worldTransform);
      node.renderDefault(renderContext);
    }
  }

  protected class Node {
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
    @Setter
    boolean inheritTransform = true, enabled = true;

    @Getter
    final PMat4 worldTransform = new PMat4();
    @Getter
    final PMat4 worldTransformInvTra = new PMat4();

    @Getter
    @Setter
    PShaderProvider defaultShaderProvider;

    private Node(PModel.Node templateNode, Node parent, PShaderProvider defaultShaderProvider) {
      this.templateNode = templateNode;
      this.parent = parent;
      this.transform.set(templateNode.transform);
      for (PGlNode node : templateNode.glNodes) {
        PGlNode newNode = node.tryDeepCopy();
        this.defaultShaderProvider = defaultShaderProvider;
        materials.put(newNode.material.getId(), newNode.material);

        this.glNodes.add(newNode);
      }
      if (parent != null) {
        parent.children.add(this);
      }
    }

    public String getId() {
      return templateNode.id;
    }

    public void renderDefault(PRenderContext renderContext) {
      for (val node : glNodes) {
        if (node.getDefaultShader() == null && defaultShaderProvider != null) {
          node.setDefaultShader(defaultShaderProvider.provide(renderContext.getCurrentRenderBuffer().getFragmentLayout(), node));
        }

        node.tryRenderDefault(renderContext);
      }

      for (Node child : children) {
        child.renderDefault(renderContext);
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
}
