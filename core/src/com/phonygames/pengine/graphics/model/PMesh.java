package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.graphics.model.gen.PModelGen;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.util.PCollectionUtils;
import com.phonygames.pengine.util.PList;

import lombok.Getter;
import lombok.val;

public class PMesh {
  @Getter
  private final PVertexAttributes vertexAttributes;
  public static PMesh FULLSCREEN_QUAD_MESH;

  public static void init() {
    PModelGen.getPostableTaskQueue().enqueue(new PModelGen() {
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
    });
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

  public PMesh(Mesh mesh) {
    backingMesh = mesh;
    this.vertexAttributes = getVertexAttributesForMesh(mesh);
  }

  private PVertexAttributes getVertexAttributesForMesh(Mesh mesh) {
    VertexAttributes vertexAttributes = mesh.getVertexAttributes();
    VertexAttribute[] attributes = new VertexAttribute[vertexAttributes.size()];
    for (int a = 0; a < attributes.length; a++) {
      val va = vertexAttributes.get(a);
      switch (va.usage) {
        case VertexAttributes.Usage.Position:
          attributes[a] = PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.pos);
          break;
        case VertexAttributes.Usage.Normal:
          attributes[a] = PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.nor);
          break;
        case VertexAttributes.Usage.TextureCoordinates:
          attributes[a] = PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.uv[va.unit]);
          break;
        case VertexAttributes.Usage.ColorPacked:
          PAssert.warnNotImplemented();
//          attributes[a] = PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.colPacked[va.unit]);
          break;
        case VertexAttributes.Usage.ColorUnpacked:
          attributes[a] = PVertexAttributes.Attribute.get(PVertexAttributes.Attribute.Keys.col[va.unit]);
          break;
      }
    }
    return new PVertexAttributes(attributes);
  }

  public PMesh(boolean isStatic, int maxVertices, int maxIndices, PVertexAttributes vertexAttributes) {
    backingMesh = new Mesh(isStatic, maxVertices, maxIndices, vertexAttributes.vertexAttributes);
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
}
