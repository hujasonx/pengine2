package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.gen.PModelGen;
import com.phonygames.pengine.graphics.shader.PShader;

import lombok.Getter;

public class PMesh {
  public static PMesh fullscreenQuadMesh;

  public static void init() {

  }

  public enum ShapeType {
    Point(GL20.GL_POINTS), Line(GL20.GL_LINES), Filled(GL20.GL_TRIANGLES);

    private final int glType;

    ShapeType(int glType) {
      this.glType = glType;
    }

    public int getGlType() {
      return glType;
    }
  }

  private final Mesh backingMesh;

  public PMesh(boolean isStatic, int maxVertices, int maxIndices, PVertexAttributes vertexAttributes) {
    backingMesh = new Mesh(isStatic, maxVertices, maxIndices, vertexAttributes.vertexAttributes);
  }

  public void render(PMaterial material, PShader shader) {
    material.applyUniforms(shader);
    shader.render(backingMesh);
  }
}
