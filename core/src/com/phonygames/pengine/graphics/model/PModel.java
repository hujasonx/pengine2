package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.graphics.PRenderContext;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PStringMap;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PModel {
  @Getter(lazy = true)
  private final PStringMap<Node> nodes = new PStringMap<>();
  @Getter(lazy = true)
  private final PList<String> rootNodeIds = new PList<>();

  private PModel() {

  }

  protected static class Node {
    @Getter
    private final String id;
    @Getter(lazy = true)
    private final PMat4 transform = PMat4.obtain();
    @Getter
    private final Node parent;
    @Getter(lazy = true)
    private final PList<Node> children = new PList<>();
    @Getter(lazy = true)
    private final PList<PGlNode> glNodes = new PList<>();
    @Getter
    @Setter
    boolean inheritTransform;

    private Node(String id, Node parent, PList<PGlNode> glNodes) {
      this.id = id;
      this.parent = parent;
      this.getGlNodes().addAll(glNodes);
      if (parent != null) {
        parent.getChildren().add(this);
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
      node.getTransform().set(transform);

      if (parent == null) {
        model.getRootNodeIds().add(id);
      }

      model.getNodes().put(id, node);
      return node;
    }

    public PModel build() {
      lockBuilder();
      return model;
    }
  }

  // Will use the material of the first instance. So make sure all buffers point to the same source!
  public void render(PList<PModelInstance> instances) {
    for (PModelInstance instance : instances) {
      if (instance.getModel() != this) {
        continue;
      }
    }

    for (String rootNodeId : getRootNodeIds()) {
      PModel.Node node = getNodes().get(rootNodeId);
    }
  }
}
