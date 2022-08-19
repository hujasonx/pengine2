package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.animation.PAnimation;
import com.phonygames.pengine.graphics.animation.PNodeAnimation;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.Duple;
import com.phonygames.pengine.util.PBuilder;
import com.phonygames.pengine.util.PFloatList;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * A class that helps keep track of co-located vertices that may differ in other non-positional attributes.
 */
public class PMeshTopology {
  /**
   * Stores collocated vertex data. Data is stored in the following form: canonical index, shared vertex count, shared
   * vertex index 0, shared vertex index 1, etc. If the vertex has no shared vertices, it should still be stored here.
   */
  private short[] canonicalIndices;
  /** The indices of the mesh, using only index of the canonical indices. */
  private short[] triangleIndicesCanonical;

  private PMeshTopology(short[] canonicalIndices, short[] triangleIndicesCanonical) {
    this.canonicalIndices = canonicalIndices;
    this.triangleIndicesCanonical = triangleIndicesCanonical;
  }

  public static class Builder extends PBuilder {
    private final PList<Duple<Short, PList<Short>>> indicesData = new PList();
    /** The number of shorts to include in the canonical indices array. */
    private int canonicalIndicesLength = 0;

    public Builder addCanonicalIndex(short canonicalIndex) {
      checkLock();
      indicesData.add(new Duple<>(canonicalIndex,new PList<>()));
      canonicalIndicesLength+= 2; // Add 1 for the index, 1 for the number of shared vertices.
      return this;
    }

    public Builder addSharedVertexIndex(int canonicalIndexIndex, short sharedIndex) {
      checkLock();
      PAssert.isTrue(canonicalIndexIndex < indicesData.size());
      indicesData.get(canonicalIndexIndex).getValue().add(sharedIndex);
      canonicalIndicesLength++;
      return this;
    }

    /**
     * Builds the mesh topology using the original triangle mesh data.
     * @param originalTriangles
     * @return
     */
    public PMeshTopology buildWithOriginalTriangles(short[] originalTriangles) {
      lockBuilder();
      // Create the canonical indices array.
      short[] canonicalIndices = new short[canonicalIndicesLength];
      /** Shared key, canonical value */
      PMap<Short, Short> canonicalMap = new PMap<>();
      int arrayIndex = 0;
      for (int a = 0; a < indicesData.size(); a++) {
        short canonical = indicesData.get(a).getKey();
        canonicalIndices[arrayIndex ++] = canonical;
        canonicalIndices[arrayIndex ++] = (short)indicesData.get(a).getValue().size();
        canonicalMap.put(canonical, canonical);
        for (int b = 0; b < indicesData.get(a).getValue().size(); b++) {
          short shared = indicesData.get(a).getValue().get(b);
          canonicalIndices[arrayIndex ++] = shared;
          canonicalMap.put(shared, canonical);
        }
      }
      // Create the triangle indices array by replacing non-canonical indices with canonical indices.
      short[] triangleIndicesCanonical = new short[originalTriangles.length];
      for (int a = 0; a < originalTriangles.length; a++) {
        triangleIndicesCanonical[a] = canonicalMap.get(originalTriangles[a]);
      }
      return new PMeshTopology(canonicalIndices, triangleIndicesCanonical);
    }
  }

  public void apply(PList<PVec3> canonicalPositions, PFloatList verticesToModify, PVertexAttributes vertexAttributes) {
    // Set the positions of the canonical vertices using the canonicalPositions list.
    int posOffset = vertexAttributes.indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
    int norOffset = vertexAttributes.indexForVertexAttribute(PVertexAttributes.Attribute.Keys.nor);
    int floatsPV = vertexAttributes.getNumFloatsPerVertex();
    int numVerticesInMesh = verticesToModify.size() / floatsPV;
    for (int indexInCanonicalIndicesArray = 0, canonicalIndexIndex = 0; indexInCanonicalIndicesArray < canonicalIndices.length;) {
      short canonicalIndex = canonicalIndices[indexInCanonicalIndicesArray + 0];
      short numSharedVertices = canonicalIndices[indexInCanonicalIndicesArray + 1];
      canonicalPositions.get(canonicalIndexIndex).emit(verticesToModify,canonicalIndex * floatsPV + posOffset);
      for (int a = 0; a < numSharedVertices; a++) {
        short sharedIndex = canonicalIndices[indexInCanonicalIndicesArray + 2 + a];
        canonicalPositions.get(canonicalIndexIndex).emit(verticesToModify,sharedIndex * floatsPV + posOffset);
      }
      canonicalIndexIndex++;
      indexInCanonicalIndicesArray += 2 + numSharedVertices;
    }
    // Recalculate the normals based on the new shape.
    recalcSmoothNormals(verticesToModify, triangleIndicesCanonical, 0,-1, vertexAttributes);
    // Copy the position and normal data from canonical vertices to the shared vertices.
    for (int indexInCanonicalIndicesArray = 0, canonicalIndexIndex = 0; indexInCanonicalIndicesArray < canonicalIndices.length;) {
      short canonicalIndex = canonicalIndices[indexInCanonicalIndicesArray + 0];
      short numSharedVertices = canonicalIndices[indexInCanonicalIndicesArray + 1];
      float normalX = verticesToModify.get(canonicalIndex * floatsPV + norOffset + 0);
      float normalY = verticesToModify.get(canonicalIndex * floatsPV + norOffset + 1);
      float normalZ = verticesToModify.get(canonicalIndex * floatsPV + norOffset + 2);
      for (int a = 0; a < numSharedVertices; a++) {
        short sharedIndex = canonicalIndices[indexInCanonicalIndicesArray + 2 + a];
        canonicalPositions.get(canonicalIndexIndex).emit(verticesToModify,sharedIndex * floatsPV + posOffset);
        verticesToModify.set(sharedIndex * floatsPV + norOffset + 0, normalX);
        verticesToModify.set(sharedIndex * floatsPV + norOffset + 1, normalY);
        verticesToModify.set(sharedIndex * floatsPV + norOffset + 2, normalZ);
      }
      canonicalIndexIndex++;
      indexInCanonicalIndicesArray += 2 + numSharedVertices;
    }
  }

  /**
   * Recalculates the normals of the mesh for smooth normals; modifies in place.
   *
   * @param vertices
   * @param indices
   * @param minVertexIndex
   * @param vertexCount      set to -1 to use all vertices.
   * @param vertexAttributes
   * @return vertices
   */
  public static PFloatList recalcSmoothNormals(PFloatList vertices, short[] indices, int minVertexIndex, int vertexCount,
                                            PVertexAttributes vertexAttributes) {
    int posOffset = vertexAttributes.indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
    int norOffset = vertexAttributes.indexForVertexAttribute(PVertexAttributes.Attribute.Keys.nor);
    if (posOffset == -1 || norOffset == -1) {
      PAssert.warn("recalcSmoothNormals called on a mesh with no positions or normals");
      return vertices;
    }
    int floatsPV = vertexAttributes.getNumFloatsPerVertex();
    int numVerticesInMesh = vertices.size() / floatsPV;
    /** Not inclusive. */
    int maxVertexIndex =
        vertexCount != -1 ? PNumberUtils.clamp(minVertexIndex + vertexCount, 0, numVerticesInMesh) : numVerticesInMesh;
    // Zero out the normal in the vertices buffer.
    PFloatList weights = PFloatList.obtain();
    for (int a = minVertexIndex; a < maxVertexIndex; a++) {
      vertices.set(a * floatsPV + norOffset + 0, 0);
      vertices.set(a * floatsPV + norOffset + 1, 0);
      vertices.set(a * floatsPV + norOffset + 2, 0);
    }
    // Loop through the triangles and add up the normals and weights.
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 v0 = pool.vec3();
      PVec3 v1 = pool.vec3();
      PVec3 v2 = pool.vec3();
      PVec3 delta01 = pool.vec3();
      PVec3 delta02 = pool.vec3();
      PVec3 normal = pool.vec3();
      for (int a = 0; a < indices.length; a += 3) {
        // Calculate the flat normal for this triangle.
        short i0 = indices[a + 0];
        short i1 = indices[a + 1];
        short i2 = indices[a + 2];
        weights.set(i0, weights.getOrDefault(i0, 0) + 1f);
        weights.set(i1, weights.getOrDefault(i1, 0) + 1f);
        weights.set(i2, weights.getOrDefault(i2, 0) + 1f);
        v0.set(vertices.get(i0 * floatsPV + posOffset + 0), vertices.get(i0 * floatsPV + posOffset + 1),
               vertices.get(i0 * floatsPV + posOffset + 2));
        v1.set(vertices.get(i1 * floatsPV + posOffset + 0), vertices.get(i1 * floatsPV + posOffset + 1),
               vertices.get(i1 * floatsPV + posOffset + 2));
        v2.set(vertices.get(i2 * floatsPV + posOffset + 0), vertices.get(i2 * floatsPV + posOffset + 1),
               vertices.get(i2 * floatsPV + posOffset + 2));
        delta01.set(v1).sub(v0);
        delta02.set(v2).sub(v0);
        normal.set(delta01).crs(delta02);
        // Set the corresponding normal in the vertices buffer.
        if (minVertexIndex <= i0 && i0 < maxVertexIndex) {
          vertices.set(i0 * floatsPV + norOffset + 0, normal.x());
          vertices.set(i0 * floatsPV + norOffset + 1, normal.y());
          vertices.set(i0 * floatsPV + norOffset + 2, normal.z());
        }
        if (minVertexIndex <= i1 && i1 < maxVertexIndex) {
          vertices.set(i1 * floatsPV + norOffset + 0, normal.x());
          vertices.set(i1 * floatsPV + norOffset + 1, normal.y());
          vertices.set(i1 * floatsPV + norOffset + 2, normal.z());
        }
        if (minVertexIndex <= i2 && i2 < maxVertexIndex) {
          vertices.set(i2 * floatsPV + norOffset + 0, normal.x());
          vertices.set(i2 * floatsPV + norOffset + 1, normal.y());
          vertices.set(i2 * floatsPV + norOffset + 2, normal.z());
        }
      }
      // Recalc the normals.
      for (int a = minVertexIndex; a < maxVertexIndex; a++) {
        normal.set(vertices.get(a * floatsPV + norOffset + 0), vertices.get(a * floatsPV + norOffset + 1),
                   vertices.get(a * floatsPV + norOffset + 2));
        normal.scl(1f / weights.getOrDefault(a, 1f));
        normal.emit(vertices, a * floatsPV + norOffset);
      }
    }
    weights.free();
    return vertices;
  }
}
