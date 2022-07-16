package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PCollectionUtils;
import com.phonygames.pengine.util.PList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.val;

public class PMesh {
  public static PMesh FULLSCREEN_QUAD_MESH;
  @Getter
  private final Mesh backingMesh;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVertexAttributes vertexAttributes;
  @Getter
  private boolean autobind = false;
  private float[] backingMeshFloats;
  private short[] backingMeshShorts;
  private BoundingBox boundingBox;
  private PVec3 center;

  public PMesh(Mesh mesh, PVertexAttributes vertexAttributes) {
    backingMesh = mesh;
    this.vertexAttributes = vertexAttributes;
  }

  /**
   * Generates a new PMesh, doing any necessary renaming.
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
        case VertexAttributes.Usage.BoneWeight:
          attr.alias = PVertexAttributes.Attribute.Keys.bon[attr.unit];
          break;
        default:
          // PAssert.warnNotImplemented("Vertex attribute " + attr.alias);
          break;
      }
    }
    this.vertexAttributes = new PVertexAttributes(backingMesh.getVertexAttributes());
  }

  public PMesh(boolean isStatic, PList<Float> vertexData, PList<Short> indexData, PVertexAttributes vertexAttributes) {
    this(isStatic, PCollectionUtils.toFloatArray(vertexData), PCollectionUtils.toShortArray(indexData),
         vertexAttributes);
  }

  public PMesh(boolean isStatic, float[] vertexData, short[] indexData, PVertexAttributes vertexAttributes) {
    this(isStatic, vertexData.length, indexData.length, vertexAttributes);
    backingMesh.setVertices(vertexData);
    backingMesh.setIndices(indexData);
  }

  public PMesh(boolean isStatic, int maxVertices, int maxIndices, PVertexAttributes vertexAttributes) {
    backingMesh = new Mesh(isStatic, maxVertices, maxIndices, vertexAttributes.getBackingVertexAttributes());
    this.vertexAttributes = vertexAttributes;
  }

  public static void init() {
    new PModelGen() {
      PModelGen.Part basePart;

      @Override protected void modelIntro() {
        basePart = addPart("base", PVertexAttributes.getPOSITION());
      }

      @Override protected void modelMiddle() {
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, -1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, 1, 1, 0).emitVertex();
        basePart.set(PVertexAttributes.Attribute.Keys.pos, -1, 1, 0).emitVertex();
        basePart.quad(false);
      }

      @Override protected void modelEnd() {
        FULLSCREEN_QUAD_MESH = basePart.getMesh();
      }
    }.buildSynchronous();
  }

  public static PVec4 vColForIndex(PVec4 out, int index) {
    int ratio = 16;
    float indexRemaining = index;
    out.x((indexRemaining % ratio) / ratio);
    indexRemaining = ((indexRemaining - (indexRemaining % ratio)) / ratio);
    out.y((indexRemaining % ratio) / ratio);
    indexRemaining = ((indexRemaining - (indexRemaining % ratio)) / ratio);
    out.z((indexRemaining % ratio) / ratio);
    indexRemaining = ((indexRemaining - (indexRemaining % ratio)) / ratio);
    out.w(1);
    return out;
  }

  public PVec3 center() {
    if (center != null) {
      return center;
    }
    if (backingMesh.getNumVertices() == 0) {
      return center = PVec3.obtain().setZero();
    }
    if (boundingBox == null) {
      boundingBox = backingMesh.calculateBoundingBox();
    }
    return (center = PVec3.obtain()).set(boundingBox.getCenterX(), boundingBox.getCenterY(), boundingBox.getCenterZ());
  }

  public float[] getBackingMeshFloats() {
    if (backingMeshFloats == null) {
      backingMesh.getVertices(
          backingMeshFloats = new float[backingMesh.getNumVertices() * vertexAttributes().getNumFloatsPerVertex()]);
    }
    return backingMeshFloats;
  }

  public short[] getBackingMeshShorts() {
    if (backingMeshShorts == null) {
      backingMesh.getIndices(backingMeshShorts = new short[backingMesh.getNumIndices()]);
    }
    return backingMeshShorts;
  }

  public void glRenderInstanced(PShader shader, int numInstances) {
    if (shader.checkValid()) {
      backingMesh.bind(shader.getShaderProgram());
      if (numInstances > 0) {
        Gdx.gl30.glDrawElementsInstanced(GL20.GL_TRIANGLES, backingMesh.getNumIndices(), GL20.GL_UNSIGNED_SHORT, 0,
                                         numInstances);
      } else if (numInstances == 0) {
        Gdx.gl30.glDrawElements(GL20.GL_TRIANGLES, backingMesh.getNumIndices(), GL20.GL_UNSIGNED_SHORT, 0);
      }
      backingMesh.unbind(shader.getShaderProgram());
    }
  }

  public void setAutobind(boolean autobind) {
    backingMesh.setAutoBind(autobind);
    this.autobind = autobind;
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
}
