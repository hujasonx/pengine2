package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.pengine.graphics.material.PMaterial;

import lombok.Getter;

public class PMesh {
  private final Mesh backingMesh;

  public PMesh(boolean isStatic, int maxVertices, int maxIndices, VertexAttribute... attributes) {
    backingMesh = new Mesh(isStatic, maxVertices, maxIndices, attributes);
  }

  public void render(PMaterial material) {
  }
}
