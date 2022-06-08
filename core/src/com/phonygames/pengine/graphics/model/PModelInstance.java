package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.graphics.shader.PShaderProvider;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import net.mgsx.gltf.scene3d.model.NodePlus;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PModelInstance {
  @Getter
  private final PModel model;

  @Getter
  private final PMap<String, Node> nodes = new PMap<>();
  private final PList<Node> rootNodes = new PList<>();

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
    boolean inheritTransform, enabled;

    @Getter
    final PMat4 worldTransform = new PMat4();
    @Getter
    final PMat4 worldTransformI = new PMat4();

    private Node(PModel.Node templateNode, Node parent, PShaderProvider defaultShaderProvider) {
      this.templateNode = templateNode;
      this.parent = parent;
      for (PGlNode node : templateNode.glNodes) {
        PGlNode newNode = node.tryDeepCopy();
        newNode.setDefaultShader(defaultShaderProvider.provide(node));

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
        node.tryRenderDefault(renderContext);
      }

      for (Node child : children) {
        child.renderDefault(renderContext);
      }
    }
  }
}
