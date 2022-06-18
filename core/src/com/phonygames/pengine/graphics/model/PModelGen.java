package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.badlogic.gdx.utils.ArrayMap;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.material.PMaterial;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.physics.PPhysicsCollisionShape;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PPostableTask;
import com.phonygames.pengine.util.PPostableTaskQueue;
import com.phonygames.pengine.util.PStringMap;
import com.phonygames.pengine.util.PWindowedBuffer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PModelGen implements PPostableTask {
  protected static final PList<PGlNode> tempGlNodesBuffer = new PList<>();
  @Getter
  private static final PPostableTaskQueue postableTaskQueue = new PPostableTaskQueue();
  private final PStringMap<Part> parts = new PStringMap<>();
  private final PStringMap<StaticPhysicsPart> staticPhysicsParts = new PStringMap<>();
  private boolean finished = false;

  public Part addPbrPart(String name) {
    return addPart(name, PVertexAttributes.getGLTF_UNSKINNED());
  }

  public Part addPart(String name, PVertexAttributes vertexAttributes) {
    Part p = new Part(name, vertexAttributes);
    parts.put(name, p);
    return p;
  }

  public StaticPhysicsPart addStaticPhysicsPart(String name) {
    StaticPhysicsPart p = new StaticPhysicsPart(name);
    staticPhysicsParts.put(name, p);
    return p;
  }

  public void buildSynchronous() {
    intro();
    middle();
    end();
  }

  @Override public void end() {
    modelEnd();
  }

  @Override public void intro() {
    modelIntro();
  }

  @Override public void middle() {
    modelMiddle();
  }

  protected void modelIntro() {
  }

  protected void modelMiddle() {
  }

  protected void modelEnd() {
  }

  protected PModelGen chainGlNode(@Nullable PList<PGlNode> list, @NonNull Part part, @NonNull PMaterial defaultMaterial,
                                  @Nullable ArrayMap<String, PMat4> boneInvBindTransforms, @NonNull String layer) {
    if (list == null) {
      list = new PList<>();
    }
    val glNode = new PGlNode(part.name);
    glNode.drawCall().setMesh(part.getMesh());
    glNode.drawCall().setMaterial(defaultMaterial);
    glNode.drawCall().setLayer(layer);
    if (boneInvBindTransforms != null) {
      glNode.invBoneTransforms().putAll(boneInvBindTransforms);
    }
    list.add(glNode);
    return this;
  }

  public void emitStaticPhysicsPartIntoModelBuilder(PModel.Builder builder) {
    for (val e : staticPhysicsParts) {
      if (e.v().indices().isEmpty()) {continue;}
      btBvhTriangleMeshShape triangleMeshShape = e.v().getTriangleMeshShape();
      PPhysicsCollisionShape<btBvhTriangleMeshShape> collisionShape =
          new PPhysicsCollisionShape<btBvhTriangleMeshShape>(triangleMeshShape) {};
      builder.model.staticCollisionShapes().put(e.k(), collisionShape);
    }
  }

  public static class Part {
    @Getter(value = AccessLevel.PROTECTED)
    @Accessors(fluent = true)
    private final float[] currentVertexValues;
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PList<Short> indices = new PList<>();
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PWindowedBuffer latestIndices = new PWindowedBuffer(4);
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final String name;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVertexAttributes vertexAttributes;
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PList<Float> vertices = new PList<>();
    @Getter
    private int numVertices = 0;

    private Part(@NonNull String name, @NonNull PVertexAttributes vertexAttributes) {
      this.name = name;
      this.vertexAttributes = vertexAttributes;
      currentVertexValues = new float[vertexAttributes.getNumFloatsPerVertex()];
    }

    public Part clear() {
      vertices().clear();
      indices().clear();
      numVertices = 0;
      return this;
    }

    public Part emit(@NonNull PMesh mesh, @Nullable StaticPhysicsPart staticPhysicsPart, VertexProcessor processor,
                     @NonNull PVertexAttributes attributesToCopy) {
      @NonNull final float[] meshVerts = mesh.getBackingMeshFloats();
      @NonNull final short[] meshShorts = mesh.getBackingMeshShorts();
      int meshFloatsPerVert = mesh.vertexAttributes().getNumFloatsPerVertex();
      for (int meshVIndex = 0; meshVIndex < meshShorts.length; meshVIndex++) { // Loop through all vertices in the mesh.
        for (VertexAttribute vertexAttribute : mesh.vertexAttributes().getBackingVertexAttributes()) {
          if (attributesToCopy.hasAttributeWithName(vertexAttribute.alias) &&
              this.vertexAttributes.hasAttributeWithName(
                  vertexAttribute.alias)) { // Loop through all the vertex attributes and ensure they are valid.
            PAssert.isFalse(vertexAttribute.usage == VertexAttribute.ColorUnpacked().usage);
            int lookupIndexStart = meshShorts[meshVIndex] * meshFloatsPerVert +
                                   mesh.vertexAttributes().indexForVertexAttribute(vertexAttribute);
            switch (vertexAttribute.numComponents) {
              case 1:
                set(vertexAttribute.alias, meshVerts[lookupIndexStart]);
                break;
              case 2:
                set(vertexAttribute.alias, meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1]);
                break;
              // TODO: transform.
              case 3:
                PVec3 tempResult = PVec3.obtain().set(meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1],
                                                      meshVerts[lookupIndexStart + 2]);
                processor.process(vertexAttribute, tempResult);
                set(vertexAttribute.alias, tempResult);
                tempResult.free();
                break;
              case 4:
                set(vertexAttribute.alias, meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1],
                    meshVerts[lookupIndexStart + 2], meshVerts[lookupIndexStart + 3]);
                break;
              default:
                PAssert.fail(
                    "Invalid vertexAttribute size: " + vertexAttribute.numComponents + " for " + vertexAttribute.alias);
            }
          }
        }
        emitVertex();
        if (meshVIndex % 3 == 2) { // Emit the triangle.
          tri(false, staticPhysicsPart);
        }
      }
      return this;
    }

    public Part set(String alias, float x) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 1);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = x;
      return this;
    }

    public Part set(String alias, float x, float y) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = x;
      currentVertexValues()[ind + 1] = y;
      return this;
    }

    public Part set(String alias, PVec3 vec) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = vec.x();
      currentVertexValues()[ind + 1] = vec.y();
      currentVertexValues()[ind + 2] = vec.z();
      return this;
    }

    public Part set(String alias, float x, float y, float z, float w) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = x;
      currentVertexValues()[ind + 1] = y;
      currentVertexValues()[ind + 2] = z;
      currentVertexValues()[ind + 3] = w;
      return this;
    }

    public Part emitVertex() {
      return emitVertex(100);
    }

    public Part tri(boolean flip, @Nullable StaticPhysicsPart copyTo) {
      indices().add((short) latestIndices().get(2));
      indices().add((short) latestIndices().get(flip ? 0 : 1));
      indices().add((short) latestIndices().get(flip ? 1 : 0));
      if (copyTo != null) {
        copyTo.copyLastTri(this);
      }
      return this;
    }

    public Part emitVertex(int maxLookbackAmount) {
      short index = (short) numVertices;
      for (int lookbackAmount = 1; lookbackAmount < maxLookbackAmount; lookbackAmount++) {
        int lookbackIndex = index - lookbackAmount;
        if (lookbackIndex < 0) {
          break;
        }
        int verticesIndex = lookbackIndex * vertexAttributes().getNumFloatsPerVertex();
        boolean isEqual = true;
        for (int a = 0; a < vertexAttributes().getNumFloatsPerVertex(); a++) {
          if (!PNumberUtils.epsilonEquals(vertices().get(verticesIndex + a), currentVertexValues()[a])) {
            isEqual = false;
            break;
          }
        }
        if (isEqual) {
          index = (short) lookbackIndex;
          break;
        }
      }
      // Need to add a new vertex.
      if (index == numVertices) {
        numVertices++;
        for (int a = 0; a < vertexAttributes().getNumFloatsPerVertex(); a++) {
          vertices().add(currentVertexValues()[a]);
        }
      }
      latestIndices().addInt(index);
      return this;
    }

    public PVec2 get(PVec2 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1);
    }

    public PVec3 get(PVec3 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1).z(ind + 2);
    }

    public PVec4 get(PVec4 out, String alias) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      return out.x(ind + 0).y(ind + 1).z(ind + 2).w(ind + 3);
    }

    public PMesh getMesh() {
      PMesh ret = new PMesh(true, vertices(), indices(), vertexAttributes());
      return ret;
    }

    public Part quad(boolean flip) {
      return quad(flip, null);
    }

    public Part quad(boolean flip, @Nullable StaticPhysicsPart copyTo) {
      indices().add((short) latestIndices().get(3));
      indices().add((short) latestIndices().get(flip ? 1 : 2));
      indices().add((short) latestIndices().get(flip ? 2 : 1));
      if (copyTo != null) {
        copyTo.copyLastTri(this);
      }
      indices().add((short) latestIndices().get(3));
      indices().add((short) latestIndices().get(flip ? 0 : 1));
      indices().add((short) latestIndices().get(flip ? 1 : 0));
      if (copyTo != null) {
        copyTo.copyLastTri(this);
      }
      return this;
    }

    public Part set(String alias, PVec2 vec) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = vec.x();
      currentVertexValues()[ind + 1] = vec.y();
      return this;
    }

    public Part set(String alias, PVec4 vec) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
      int ind = vertexAttributes.indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = vec.x();
      currentVertexValues()[ind + 1] = vec.y();
      currentVertexValues()[ind + 2] = vec.z();
      currentVertexValues()[ind + 3] = vec.w();
      return this;
    }

    public Part set(String alias, float x, float y, float z) {
      PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
      int ind = vertexAttributes().indexForVertexAttribute(alias);
      currentVertexValues()[ind + 0] = x;
      currentVertexValues()[ind + 1] = y;
      currentVertexValues()[ind + 2] = z;
      return this;
    }

    public Part tri(boolean flip) {
      return tri(flip, null);
    }

    public static class VertexProcessor implements PPool.Poolable {
      @Getter(value = AccessLevel.PUBLIC, lazy = true)
      @Accessors(fluent = true)
      private static final PPool<VertexProcessor> staticPool = new PPool<VertexProcessor>() {
        @Override protected VertexProcessor newObject() {
          return new VertexProcessor();
        }
      };
      @Getter
      @Setter
      private PPool ownerPool;
      @Getter(value = AccessLevel.PUBLIC)
      @Accessors(fluent = true)
      private PMat4 transform;

      private final PVec3 process(VertexAttribute attribute, PVec3 out) {
        if (transform != null) {
          return PVertexAttributes.transformVecWithMatrix(attribute, out, transform);
        }
        PAssert.fail("No valid vertex processing strategy");
        return out;
      }

      @Override public void reset() {
        this.transform = null;
      }

      public VertexProcessor transform(PMat4 mat4) {
        this.transform = mat4;
        return this;
      }
    }
  }

  public static class StaticPhysicsPart {
    private static final int MAX_SEARCH_DEPTH = 100;
    @Getter(value = AccessLevel.PUBLIC, lazy = true)
    @Accessors(fluent = true)
    private static final PVertexAttributes vertexAttributes = PVertexAttributes.getPHYSICS();
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PList<Short> indices = new PList<>();
    @Getter(value = AccessLevel.PRIVATE)
    @Accessors(fluent = true)
    private final String name;
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    @Accessors(fluent = true)
    private final PList<Float> vertices = new PList<>();

    private StaticPhysicsPart(@NonNull String name) {
      this.name = name;
    }

    private StaticPhysicsPart copyLastTri(Part part) {
      int indexOfPosAttrInPartVertexFloatArray =
          part.vertexAttributes().indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
      int partFloatsPerVertex = part.vertexAttributes().getNumFloatsPerVertex();
      float x, y, z;
      x = part.vertices().get(partFloatsPerVertex * part.indices().get(-3) + indexOfPosAttrInPartVertexFloatArray + 0);
      y = part.vertices().get(partFloatsPerVertex * part.indices().get(-3) + indexOfPosAttrInPartVertexFloatArray + 1);
      z = part.vertices().get(partFloatsPerVertex * part.indices().get(-3) + indexOfPosAttrInPartVertexFloatArray + 2);
      indices().add(getIndexOrAdd(x, y, z, MAX_SEARCH_DEPTH));
      x = part.vertices().get(partFloatsPerVertex * part.indices().get(-2) + indexOfPosAttrInPartVertexFloatArray + 0);
      y = part.vertices().get(partFloatsPerVertex * part.indices().get(-2) + indexOfPosAttrInPartVertexFloatArray + 1);
      z = part.vertices().get(partFloatsPerVertex * part.indices().get(-2) + indexOfPosAttrInPartVertexFloatArray + 2);
      indices().add(getIndexOrAdd(x, y, z, MAX_SEARCH_DEPTH));
      x = part.vertices().get(partFloatsPerVertex * part.indices().get(-1) + indexOfPosAttrInPartVertexFloatArray + 0);
      y = part.vertices().get(partFloatsPerVertex * part.indices().get(-1) + indexOfPosAttrInPartVertexFloatArray + 1);
      z = part.vertices().get(partFloatsPerVertex * part.indices().get(-1) + indexOfPosAttrInPartVertexFloatArray + 2);
      indices().add(getIndexOrAdd(x, y, z, MAX_SEARCH_DEPTH));
      return this;
    }

    private short getIndexOrAdd(final float x, final float y, final float z, final int maxSearchDepth) {
      if (!vertices().isEmpty()) {
        for (int a = 0; a < maxSearchDepth; a++) {
          int lookupIndex = vertices().size - 3 * (1 + a);
          if (!PNumberUtils.epsilonEquals(vertices().get(lookupIndex + 0), x)) {
            continue;
          }
          if (!PNumberUtils.epsilonEquals(vertices().get(lookupIndex + 1), y)) {
            continue;
          }
          if (!PNumberUtils.epsilonEquals(vertices().get(lookupIndex + 2), z)) {
            continue;
          }
          return (short) (lookupIndex / 3);
        }
      }
      // We reached the max search depth (or bottom) without finding a match for the position, so add new position.
      short ret = (short) (vertices().size / 3);
      vertices().add(x);
      vertices().add(y);
      vertices().add(z);
      return ret;
    }

    public btBvhTriangleMeshShape getTriangleMeshShape() {
      PMesh pMesh = new PMesh(true, vertices(), indices(), vertexAttributes());
      Mesh mesh = pMesh.getBackingMesh();
      ModelBuilder modelBuilder = new ModelBuilder();
      modelBuilder.begin();
      modelBuilder.part(name, mesh, GL20.GL_TRIANGLES, new Material());
      Model model = modelBuilder.end();
      btBvhTriangleMeshShape ret = new btBvhTriangleMeshShape(model.meshParts);
      return ret;
    }
  }
}
