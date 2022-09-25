package com.phonygames.pengine.graphics.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.graphics.color.PVColIndexBuffer;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.collection.PCollectionUtils;
import com.phonygames.pengine.util.collection.PFloatList;
import com.phonygames.pengine.util.collection.PList;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

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
  private PVec3 center, bounds;

  public PMesh(Mesh mesh, PVertexAttributes vertexAttributes) {
    backingMesh = mesh;
    this.vertexAttributes = vertexAttributes;
  }


  /**
   * Generates a new PMesh, doing any necessary renaming from GDX attributes to pengine2 attributes.
   * Additionally, converts col[0] to vColI if vColIndexDivisions != -1.
   * @param mesh
   * @param vColIndexDivisions the number of divisions used to specify vColIndices, or -1.
   */
  public PMesh(Mesh mesh, int vColIndexDivisions) {
    // If we need to convert a col0 attr to vColI after the initial attributes pass, store it here.
    VertexAttribute col0AttrToConvertToVColI = null;
    PVertexAttribute.Definition[] definitions = new PVertexAttribute.Definition[mesh.getVertexAttributes().size()];
    for (int a = 0; a < mesh.getVertexAttributes().size(); a++) {
      VertexAttribute attr= mesh.getVertexAttributes().get(a);
      switch (attr.usage) {
        case VertexAttributes.Usage.Position:
          attr.alias = PVertexAttribute.Definitions.pos.alias;
          break;
        case VertexAttributes.Usage.Normal:
          attr.alias = PVertexAttribute.Definitions.nor.alias;
          break;
        case VertexAttributes.Usage.TextureCoordinates:
          attr.alias = PVertexAttribute.Definitions.uv[attr.unit].alias;
          break;
        case VertexAttributes.Usage.ColorPacked:
          if (attr.unit == 0 && vColIndexDivisions != -1) {col0AttrToConvertToVColI = attr;}
          attr.alias = PVertexAttribute.Definitions.colPacked[attr.unit].alias;
          break;
        case VertexAttributes.Usage.ColorUnpacked:
          if (attr.unit == 0 && vColIndexDivisions != -1) {col0AttrToConvertToVColI = attr;}
          attr.alias = PVertexAttribute.Definitions.col[attr.unit].alias;
          break;
        case VertexAttributes.Usage.BoneWeight:
          attr.alias = PVertexAttribute.Definitions.bon[attr.unit].alias;
          break;
        default:
          // PAssert.warnNotImplemented("Vertex attribute " + attr.alias);
          break;
      }
      // If we need to convert this col0, create the VertexAttribute definition.
      if (attr == col0AttrToConvertToVColI) {
        definitions[a] = PVertexAttribute.Definitions.vColI;
      } else {
        definitions[a] = PVertexAttribute.Definition.fromAttribute(attr);
      }
    }
    // Create the vertex attributes.
    this.vertexAttributes = new PVertexAttributes(definitions);
    if (col0AttrToConvertToVColI != null) {
      PVec4 tempCol0 = PVec4.obtain();
      // Convert col0 to vColI.
      float[] originalMeshVertices = new float[mesh.getNumVertices() * mesh.getVertexAttributes().vertexSize / 4];
      short[] originalMeshIndices = new short[mesh.getNumIndices()];
      mesh.getVertices(originalMeshVertices);
      mesh.getIndices(originalMeshIndices);
      float[] newMeshVertices = new float[mesh.getNumVertices() * this.vertexAttributes.sizeInFloats()];
      // Create a new mesh with the new vertexAttributes.
      this.backingMesh = new Mesh(false, mesh.getNumVertices(), originalMeshIndices.length, this.vertexAttributes.backingVertexAttributes());
      backingMesh.setIndices(originalMeshIndices);
      // Loop through each vertex and emit its attributes.
      for (int b = 0; b < mesh.getNumVertices(); b++) {
        // The float offset in the original vertex buffer that the vertex can be found at.
        int oInO = mesh.getVertexSize() / 4 * b;
        // The float offset in the new vertex buffer that the vertex should be put at.
        int oInN = this.vertexAttributes.sizeInFloats() * b;
        for (int a = 0; a < this.vertexAttributes.count(); a++) {
          // The new vertex attribute.
          PVertexAttribute pva = this.vertexAttributes.pva(a);
          // The original vertex attribute.
          VertexAttribute originalAttr = mesh.getVertexAttributes().get(a);
          // The float offset in the original vertex buffer that the vertex attribute for this vertex can be found at.
          int pvaOInO = oInO + originalAttr.offset / 4;
          // The float offset in the new vertex buffer that the vertex attribute for this vertex should be put at.
          int pvaOInN = oInN + pva.offsetInOwnerBytes() / 4;
          if (originalAttr == col0AttrToConvertToVColI) {
            if (originalAttr.type == GL20.GL_UNSIGNED_BYTE) {
              PVertexAttribute.vec4FromUnsignedByteColor(tempCol0, originalMeshVertices[pvaOInO]);
            }
            if (originalAttr.type == GL20.GL_UNSIGNED_SHORT ) {
              PVertexAttribute.vec4FromUnsignedShortColor(tempCol0, originalMeshVertices[pvaOInO], originalMeshVertices[pvaOInO + 1]);
            }
            // Convert col0 to vColI.
            newMeshVertices[pvaOInN] = tempCol0.toVColI(vColIndexDivisions);
          } else {
            // Emit every other attribute normally.
            for (int c = 0; c < pva.sizeInFloats(); c++) {
              newMeshVertices[pvaOInN + c] = originalMeshVertices[pvaOInO + c];
            }
          }
        }
      }
      backingMesh.setVertices(newMeshVertices);
      tempCol0.free();
    } else {
      // Use the mesh as is.
      this.backingMesh = mesh;
    }
  }

  /**
   * Generates a new PMesh, doing any necessary renaming from GDX attributes to pengine2 attributes.
   *
   * @param mesh
   */
  public PMesh(Mesh mesh) {
    this(mesh, -1);
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
    backingMesh = new Mesh(isStatic, maxVertices, maxIndices, vertexAttributes.backingVertexAttributes());
    this.vertexAttributes = vertexAttributes;
  }

  public PMesh(boolean isStatic, PFloatList vertexData, PList<Short> indexData, PVertexAttributes vertexAttributes) {
    this(isStatic, vertexData.emitTo(new float[vertexData.size()], 0), PCollectionUtils.toShortArray(indexData),
         vertexAttributes);
  }

  public static void init() {
    new PModelGen() {
      PMeshGen baseMeshGen;

      @Override protected void modelIntro() {
        baseMeshGen = getOrAddOpaqueMesh("base", PVertexAttributes.Templates.POS);
      }

      @Override protected void modelMiddle() {
        baseMeshGen.set(PVertexAttribute.Definitions.pos.alias, -1, -1, 0).emitVertex();
        baseMeshGen.set(PVertexAttribute.Definitions.pos.alias, 1, -1, 0).emitVertex();
        baseMeshGen.set(PVertexAttribute.Definitions.pos.alias, 1, 1, 0).emitVertex();
        baseMeshGen.set(PVertexAttribute.Definitions.pos.alias, -1, 1, 0).emitVertex();
        baseMeshGen.quad(false);
      }

      @Override protected void modelEnd() {
        FULLSCREEN_QUAD_MESH = baseMeshGen.getMesh();
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

  public PVec3 bounds() {
    if (bounds != null) {
      return bounds;
    }
    if (backingMesh.getNumVertices() == 0) {
      return bounds = PVec3.obtain().setZero();
    }
    if (boundingBox == null) {
      boundingBox = backingMesh.calculateBoundingBox();
    }
    bounds = PVec3.obtain();
    boundingBox.getDimensions(bounds.backingVec3());
    return bounds;
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
    center = PVec3.obtain();
    boundingBox.getCenter(center.backingVec3());
    return center;
  }

  public float[] getBackingMeshFloats() {
    if (backingMeshFloats == null) {
      backingMesh.getVertices(
          backingMeshFloats = new float[backingMesh.getNumVertices() * vertexAttributes().sizeInFloats()]);
    }
    return backingMeshFloats;
  }

  public short[] getBackingMeshShorts() {
    if (backingMeshShorts == null) {
      backingMesh.getIndices(backingMeshShorts = new short[backingMesh.getNumIndices()]);
    }
    return backingMeshShorts;
  }

  public ShortBuffer getIndicesBuffer() {
    return backingMesh.getIndicesBuffer();
  }

  public FloatBuffer getVerticesBuffer() {
    return backingMesh.getVerticesBuffer();
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

  public PMesh setIndices(short[] indices) {
    return setIndices(indices, 0, indices.length);
  }

  public PMesh setIndices(short[] indices, int offset, int count) {
    backingMesh.setIndices(indices, offset, count);
    backingMeshShorts = null;
    if (center != null) {
      center.free();
      center = null;
    }
    return this;
  }

  public PMesh setVertices(float[] vertices) {
    return setVertices(vertices,0,vertices.length);
  }

  public PMesh setVertices(float[] vertices, int offset, int count) {
    backingMesh.setVertices(vertices, offset, count);
    backingMeshFloats = null;
    if (center != null) {
      center.free();
      center = null;
    }
    return this;
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
