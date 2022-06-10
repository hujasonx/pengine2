package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.model.gen.PModelGen;
import com.phonygames.pengine.util.PCollectionUtils;
import com.phonygames.pengine.util.PList;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

public class PMesh {
  @Getter
  private final PVertexAttributes vertexAttributes;
  public static PMesh FULLSCREEN_QUAD_MESH;

  @Getter
  private boolean autobind = false;

  public static void init() {
    new PModelGen() {
      PModelGen.Part basePart;

      @Override
      protected void modelIntro() {
        basePart = addPart("base", PVertexAttributes.getPOSITION());
      }

      @Override
      protected void modelMiddle() {
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, 1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, 1, 0).emitVertex();
        basePart.quad(false);
      }

      @Override
      protected void modelEnd() {
        FULLSCREEN_QUAD_MESH = basePart.getMesh();
      }
    }.buildSynchronous();
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

  @Getter
  private final Mesh backingMesh;

  public PMesh(Mesh mesh, PVertexAttributes vertexAttributes) {
    backingMesh = mesh;
    this.vertexAttributes = vertexAttributes;
  }

  /**
   * Generates a new PMesh, doing any necessary renaming.
   *
   * @param mesh
   */
  public PMesh(Mesh mesh) {
    backingMesh = mesh;
    for (val attr : mesh.getVertexAttributes()) {
      switch (attr.usage) {
        case VertexAttributes.Usage.Position:
          attr.alias = PVertexAttributes.Attribute.Keys.pos;
          break;
        case VertexAttributes.Usage.Normal:
          attr.alias = PVertexAttributes.Attribute.Keys.nor;
          break;
        case VertexAttributes.Usage.TextureCoordinates:
          attr.alias = PVertexAttributes.Attribute.Keys.uv[attr.unit];
          break;
        case VertexAttributes.Usage.ColorPacked:
        case VertexAttributes.Usage.ColorUnpacked:
          attr.alias = PVertexAttributes.Attribute.Keys.col[attr.unit];
          break;
      }
    }

    this.vertexAttributes = new PVertexAttributes(backingMesh.getVertexAttributes());
  }

  public PMesh(boolean isStatic, int maxVertices, int maxIndices, PVertexAttributes vertexAttributes) {
    backingMesh = new Mesh(isStatic, maxVertices, maxIndices, vertexAttributes.getBackingVertexAttributes());
    this.vertexAttributes = vertexAttributes;
  }

  public PMesh(boolean isStatic, PList<Float> vertexData, PList<Short> indexData, PVertexAttributes vertexAttributes) {
    this(isStatic, PCollectionUtils.toFloatArray(vertexData), PCollectionUtils.toShortArray(indexData), vertexAttributes);
  }

  public PMesh(boolean isStatic, float[] vertexData, short[] indexData, PVertexAttributes vertexAttributes) {
    this(isStatic, vertexData.length, indexData.length, vertexAttributes);
    backingMesh.setVertices(vertexData);
    backingMesh.setIndices(indexData);
  }

  public void setAutobind(boolean autobind) {
    backingMesh.setAutoBind(autobind);
    this.autobind = autobind;
  }
}
