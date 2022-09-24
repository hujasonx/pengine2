package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.utils.Null;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PWindowedIntBuffer;
import com.phonygames.pengine.util.collection.PList;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Helper class to generate a mesh. */
public class PMeshGen {
  /** The current float values. */
  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  private final float[] currentVertexValues;
  /** The indices that have been committed. */
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  private final PList<Short> indices = new PList<>();
  /** The last several vertex indices, used to make tris and quads. */
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  private final PWindowedIntBuffer latestIndices = new PWindowedIntBuffer(4);
  /** The bounds of the mesh so far. */
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  private final PVec3 minPos = PVec3.obtain(), maxPos = PVec3.obtain();
  /** The name of this mesh. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final String name;
  /** The vertex attributes. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVertexAttributes vertexAttributes;
  /** The vertex values that have been committed. */
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  private final PList<Float> vertices = new PList<>();
  /** The number of vertices. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int numVertices = 0;
  /** The PMeshGenVertexProcessor to use. */
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private @Nullable PMeshGenVertexProcessor vertexProcessor;

  private PMeshGen(@NonNull String name, @NonNull PVertexAttributes vertexAttributes) {
    this.name = name;
    this.vertexAttributes = vertexAttributes;
    currentVertexValues = new float[vertexAttributes.getNumFloatsPerVertex()];
  }

  /** Clears the meshGen to have no commited vertices or indices. */
  public PMeshGen clear() {
    vertices().clear();
    indices().clear();
    numVertices = 0;
    return this;
  }

  /** Fully resets the meshGen. Resets all variables, unlike clear(). */
  public PMeshGen reset() {
    clear();
    latestIndices.clear();
    Arrays.fill(currentVertexValues, 0);
    return this;
  }

//  public PMeshGen emit(@NonNull PMesh mesh, @Nullable PModelGen.StaticPhysicsPart staticPhysicsPart, PMeshGenVertexProcessor processor,
//                             @NonNull PVertexAttributes attributesToCopy) {
//    @NonNull final float[] meshVerts = mesh.getBackingMeshFloats();
//    @NonNull final short[] meshShorts = mesh.getBackingMeshShorts();
//    int meshFloatsPerVert = mesh.vertexAttributes().getNumFloatsPerVertex();
//    for (int meshVIndex = 0; meshVIndex < meshShorts.length; meshVIndex++) { // Loop through all vertices in the mesh.
//      // Get the raw position.
//      int posLookupI = meshShorts[meshVIndex] * meshFloatsPerVert +
//                       mesh.vertexAttributes().indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
//      float rawPosX = meshVerts[posLookupI + 0];
//      float rawPosY = meshVerts[posLookupI + 1];
//      float rawPosZ = meshVerts[posLookupI + 2];
//      // Loop through all the vertex attributes and ensure they are valid.
//      for (VertexAttribute vertexAttribute : mesh.vertexAttributes().getBackingVertexAttributes()) {
//        if (attributesToCopy.hasAttributeWithName(vertexAttribute.alias) &&
//            this.vertexAttributes.hasAttributeWithName(vertexAttribute.alias)) {
//          PAssert.isFalse(vertexAttribute.usage == VertexAttribute.ColorUnpacked().usage);
//          // Process the attribute data.
//          int lookupIndexStart = meshShorts[meshVIndex] * meshFloatsPerVert +
//                                 mesh.vertexAttributes().indexForVertexAttribute(vertexAttribute);
//          switch (vertexAttribute.numComponents) {
//            case 1:
//              set(vertexAttribute.alias, meshVerts[lookupIndexStart]);
//              break;
//            case 2:
//              set(vertexAttribute.alias, meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1]);
//              break;
//            // TODO: transform.
//            case 3:
//              PVec3 tempResult = PVec3.obtain().set(meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1],
//                                                    meshVerts[lookupIndexStart + 2]);
//              processor.process(rawPosX, rawPosY, rawPosZ, vertexAttribute, tempResult);
//              set(vertexAttribute.alias, tempResult);
//              tempResult.free();
//              break;
//            case 4:
//              set(vertexAttribute.alias, meshVerts[lookupIndexStart + 0], meshVerts[lookupIndexStart + 1],
//                  meshVerts[lookupIndexStart + 2], meshVerts[lookupIndexStart + 3]);
//              break;
//            default:
//              PAssert.fail(
//                  "Invalid vertexAttribute size: " + vertexAttribute.numComponents + " for " + vertexAttribute.alias);
//          }
//        }
//      }
//      emitVertex();
//      if (meshVIndex % 3 == 2) { // Emit the triangle.
//        tri(false, staticPhysicsPart);
//      }
//    }
//    return this;
//  }

  public PMeshGen set(String alias, float x) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 1);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues()[ind + 0] = x;
    return this;
  }

  public PMeshGen set(String alias, float x, float y) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues()[ind + 0] = x;
    currentVertexValues()[ind + 1] = y;
    return this;
  }

  public PMeshGen set(String alias, PVec3 vec) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
    if (alias.equals(PVertexAttributes.Attribute.Keys.pos)) {
      minPos().set(Math.min(minPos().x(), vec.x()), Math.min(minPos().y(), vec.y()), Math.min(minPos().z(), vec.z()));
      maxPos().set(Math.max(maxPos().x(), vec.x()), Math.max(maxPos().y(), vec.y()), Math.max(maxPos().z(), vec.z()));
    }
    int ind = vertexAttributes.indexForVertexAttribute(alias);
    currentVertexValues()[ind + 0] = vec.x();
    currentVertexValues()[ind + 1] = vec.y();
    currentVertexValues()[ind + 2] = vec.z();
    return this;
  }

  public PMeshGen set(String alias, float x, float y, float z, float w) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues()[ind + 0] = x;
    currentVertexValues()[ind + 1] = y;
    currentVertexValues()[ind + 2] = z;
    currentVertexValues()[ind + 3] = w;
    return this;
  }

  public PMeshGen emitVertex() {
    return emitVertex(Integer.MAX_VALUE);
  }

  public PMeshGen tri(boolean flip) {
    indices().add((short) latestIndices().get(2));
    indices().add((short) latestIndices().get(flip ? 0 : 1));
    indices().add((short) latestIndices().get(flip ? 1 : 0));
    return this;
  }

  public PMeshGen emitVertex(int maxLookbackAmount) {
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

  public PVec3 getMiddle(PVec3 out) {
    out.set(minPos()).lerp(maxPos(), 0.5f);
    return out;
  }

  public PMeshGen quad(boolean flip) {
    indices().add((short) latestIndices().get(3));
    indices().add((short) latestIndices().get(flip ? 1 : 2));
    indices().add((short) latestIndices().get(flip ? 2 : 1));
    indices().add((short) latestIndices().get(3));
    indices().add((short) latestIndices().get(flip ? 0 : 1));
    indices().add((short) latestIndices().get(flip ? 1 : 0));
    return this;
  }

  public PMeshGen set(String alias, float x, float y, float z) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
    if (alias.equals(PVertexAttributes.Attribute.Keys.pos)) {
      minPos().set(Math.min(minPos().x(), x), Math.min(minPos().y(), y), Math.min(minPos().z(), z));
      maxPos().set(Math.max(maxPos().x(), x), Math.max(maxPos().y(), y), Math.max(maxPos().z(), z));
    }
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues()[ind + 0] = x;
    currentVertexValues()[ind + 1] = y;
    currentVertexValues()[ind + 2] = z;
    return this;
  }

  public PMeshGen set(String alias, PVec2 vec) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues()[ind + 0] = vec.x();
    currentVertexValues()[ind + 1] = vec.y();
    return this;
  }

  public PMeshGen set(String alias, PVec4 vec) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
    int ind = vertexAttributes.indexForVertexAttribute(alias);
    currentVertexValues()[ind + 0] = vec.x();
    currentVertexValues()[ind + 1] = vec.y();
    currentVertexValues()[ind + 2] = vec.z();
    currentVertexValues()[ind + 3] = vec.w();
    return this;
  }

}
