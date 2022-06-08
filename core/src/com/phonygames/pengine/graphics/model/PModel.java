package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;

import lombok.Getter;
import lombok.Setter;

public class PModel {
  final PMap<String, Node> nodes = new PMap<>();
  final PList<String> rootNodeIds = new PList<>();

  private PModel() {

  }

  protected static class Node {
    final String id;
    @Getter
    final PMat4 transform = new PMat4();
    @Getter
    final Node parent;
    @Getter
    final PList<Node> children = new PList<>();
    final PList<PGlNode> glNodes = new PList<>();
    @Getter
    @Setter
    boolean inheritTransform;

    private Node(String id, Node parent, PList<PGlNode> glNodes) {
      this.id = id;
      this.parent = parent;
      this.glNodes.addAll(glNodes);
      if (parent != null) {
        parent.children.add(this);
      }
    }

    private boolean hasParent() {
      return parent != null;
    }
  }

  public static class Builder extends PBuilder {
    private PModel model = new PModel();

    public Node addNode(String id, Node parent, PList<PGlNode> glNodes, PMat4 transform) {
      checkLock();
      Node node = new Node(id, parent, glNodes);
      node.transform.set(transform);

      if (parent == null) {
        model.rootNodeIds.add(id);
      }

      model.nodes.put(id, node);
      return node;
    }

    public PModel build() {
      lockBuilder();
      return model;
    }
  }

}
