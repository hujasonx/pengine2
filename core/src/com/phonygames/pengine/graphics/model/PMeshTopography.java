package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.util.PFloatList;
import com.phonygames.pengine.util.PPool;

public class PMeshTopography {
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
  public static float[] recalcSmoothNormals(float[] vertices, short[] indices, int minVertexIndex, int vertexCount,
                                            PVertexAttributes vertexAttributes) {
    int posOffset = vertexAttributes.indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
    int norOffset = vertexAttributes.indexForVertexAttribute(PVertexAttributes.Attribute.Keys.nor);
    if (posOffset == -1 || norOffset == -1) {
      PAssert.warn("recalcSmoothNormals called on a mesh with no positions or normals");
      return vertices;
    }
    int floatsPV = vertexAttributes.getNumFloatsPerVertex();
    int numVerticesInMesh = vertices.length / floatsPV;
    /** Not inclusive. */
    int maxVertexIndex = vertexCount != -1 ? PNumberUtils.clamp(vertexCount, 0, numVerticesInMesh) : numVerticesInMesh;
    // Zero out the normal in the vertices buffer.
    PFloatList weights = PFloatList.obtain();
    for (int a = minVertexIndex; a < maxVertexIndex; a ++) {
      vertices[a * floatsPV + norOffset + 0] = 0;
      vertices[a * floatsPV + norOffset + 1] = 0;
      vertices[a * floatsPV + norOffset + 2] = 0;
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
        v0.set(vertices[i0 * floatsPV + posOffset + 0], vertices[i0 * floatsPV + posOffset + 1],
               vertices[i0 * floatsPV + posOffset + 2]);
        v1.set(vertices[i1 * floatsPV + posOffset + 0], vertices[i1 * floatsPV + posOffset + 1],
               vertices[i1 * floatsPV + posOffset + 2]);
        v2.set(vertices[i2 * floatsPV + posOffset + 0], vertices[i2 * floatsPV + posOffset + 1],
               vertices[i2 * floatsPV + posOffset + 2]);
        delta01.set(v1).sub(v0);
        delta02.set(v2).sub(v0);
        normal.set(delta01).crs(delta02);
        // Set the corresponding normal in the vertices buffer.
        if (minVertexIndex <= i0 && i0 < maxVertexIndex) {
          vertices[i0 * floatsPV + norOffset + 0] = normal.x();
          vertices[i0 * floatsPV + norOffset + 1] = normal.y();
          vertices[i0 * floatsPV + norOffset + 2] = normal.z();
        }
        if (minVertexIndex <= i1 && i1 < maxVertexIndex) {
          vertices[i1 * floatsPV + norOffset + 0] = normal.x();
          vertices[i1 * floatsPV + norOffset + 1] = normal.y();
          vertices[i1 * floatsPV + norOffset + 2] = normal.z();
        }
        if (minVertexIndex <= i2 && i2 < maxVertexIndex) {
          vertices[i2 * floatsPV + norOffset + 0] = normal.x();
          vertices[i2 * floatsPV + norOffset + 1] = normal.y();
          vertices[i2 * floatsPV + norOffset + 2] = normal.z();
        }
      }
      // Recalc the normals.
      for (int a = minVertexIndex; a < maxVertexIndex; a ++) {
        normal.set(vertices[a * floatsPV + norOffset + 0], vertices[a * floatsPV + norOffset + 1], vertices[a * floatsPV + norOffset + 2]);
        normal.scl(1f / weights.getOrDefault(a, 1f));
        normal.emit(vertices, a * floatsPV + norOffset);
      }
    }
    weights.free();
    return vertices;
  }
}
