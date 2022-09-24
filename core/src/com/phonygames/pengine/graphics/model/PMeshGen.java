package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PPlane;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.collection.PFloatList;
import com.phonygames.pengine.util.collection.PList;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Helper class to generate a mesh.
 * <p>
 * Usage:
 */
public class PMeshGen {
  private static final int QUEUED_VERTICES_TO_KEEP = 6;
  /** Temp vectors. */
  private final PVec1 __tmp1 = PVec1.obtain();
  /** Temp vectors. */
  private final PVec2 __tmp2 = PVec2.obtain();
  /** Temp vectors. */
  private final PVec3 __tmp3 = PVec3.obtain();
  /** Temp vectors. */
  private final PVec4 __tmp4 = PVec4.obtain();
  /** A list of float buffer used internally for processing queued vertices. */
  private final float[][] __tmpProcessingVertexValues = new float[6][];
  /** The clipping planes. They will all be ANDed together to clip emitted triangles in model space. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<PPlane> clippingPlanes = new PList<PPlane>(PPlane.getStaticPool());
  /** The current float values for queueing. */
  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  private final float[] currentVertexValues;
  /** The indices that have been committed. */
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  private final PList<Short> indices = new PList<>();
  /** The name of this mesh. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final String name;
  /** The vertex values that have been queued. */
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  private final PFloatList queuedVertices = PFloatList.obtain();
  /** The vertex attributes. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVertexAttributes vertexAttributes;
  /** The vertex values that have been committed. */
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  private final PFloatList vertices = PFloatList.obtain();
  /** The number of vertices. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int numVertices = 0;
  /** The PMeshGenVertexProcessor to use. */
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private @Nullable
  PMeshGenVertexProcessor vertexProcessor;

  protected PMeshGen(@NonNull String name, @NonNull PVertexAttributes vertexAttributes) {
    this.name = name;
    this.vertexAttributes = vertexAttributes;
    currentVertexValues = new float[vertexAttributes.getNumFloatsPerVertex()];
    for (int a = 0; a < __tmpProcessingVertexValues.length; a++) {
      __tmpProcessingVertexValues[a] = new float[vertexAttributes.getNumFloatsPerVertex()];
    }
  }

  /**
   * Takes processed vertex data and adds it to the output mesh. Caller is responsible for adding the returned index to
   * the indices buffer.
   *
   * @param processedVertexValues The processed vertex values that should be directly emitted.
   * @param maxLookbackAmount     The maximum number of vertices back to search for reusable indices.
   * @return the index.
   */
  private short __finalizeVertex(float[] processedVertexValues, int maxLookbackAmount) {
    short index = (short) numVertices;
    for (int lookbackAmount = 1; lookbackAmount < maxLookbackAmount; lookbackAmount++) {
      int lookbackIndex = index - lookbackAmount;
      if (lookbackIndex < 0) {
        break;
      }
      int verticesIndex = lookbackIndex * vertexAttributes().getNumFloatsPerVertex();
      boolean isEqual = true;
      for (int a = 0; a < vertexAttributes().getNumFloatsPerVertex(); a++) {
        if (!PNumberUtils.epsilonEquals(vertices().get(verticesIndex + a), processedVertexValues[a])) {
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
        vertices().add(processedVertexValues[a]);
      }
    }
    return index;
  }

  /**
   * Processes the vertex with the vertex processor, if one is set.
   *
   * @param floats The unprocessed float data for the vertex to add. Processed in place.
   * @param outPos A vector that will be set to the output position.
   */
  private float[] __processVertexWithVertexProcessor(float[] floats, @Nullable PVec3 outPos) {
    /** The index in the float array of the position. */
    int posI = vertexAttributes.indexForVertexAttribute(PVertexAttributes.Attribute.Keys.pos);
    if (vertexProcessor == null) {
      if (outPos != null) {
        outPos.set(floats[posI + 0], floats[posI + 1], floats[posI + 2]);
      }
      return floats;
    }
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 rawPos = pool.vec3(floats[posI + 0], floats[posI + 1], floats[posI + 2]);
      // Loop through the vertex attributes and process each one.
      for (int a = 0; a < vertexAttributes.getBackingVertexAttributes().size(); a++) {
        VertexAttribute va = vertexAttributes.getBackingVertexAttributes().get(a);
        int iForAttr = vertexAttributes.indexForVertexAttribute(va);
        switch (va.alias) {
          case PVertexAttributes.Attribute.Keys.pos: // Handle position.
            PVec3 pos = pool.vec3(rawPos);
            vertexProcessor.processPos(pos);
            if (outPos != null) {outPos.set(pos);}
            pos.emit(floats, iForAttr);
            break;
          case PVertexAttributes.Attribute.Keys.nor: // Handle normal.
            PVec3 nor = pool.vec3(floats[iForAttr + 0], floats[iForAttr + 1], floats[iForAttr + 2]);
            vertexProcessor.processNor(rawPos, nor);
            nor.emit(floats, iForAttr);
            break;
          default: // Handle everything else.
            int vertexSizeForAttribute = va.getSizeInBytes() / 4;
            switch (vertexSizeForAttribute) {
              case 1:
                PVec1 v1 = pool.vec1(floats[iForAttr]);
                vertexProcessor.processOther(v1, va);
                floats[iForAttr] = v1.x();
                break;
              case 2:
                PVec2 v2 = pool.vec2(floats[iForAttr + 0], floats[iForAttr + 1]);
                vertexProcessor.processOther(v2, va);
                v2.emit(floats, iForAttr);
                break;
              case 3:
                PVec3 v3 = pool.vec3(floats[iForAttr + 0], floats[iForAttr + 1], floats[iForAttr + 2]);
                vertexProcessor.processOther(v3, va);
                v3.emit(floats, iForAttr);
                break;
              case 4:
                PVec4 v4 =
                    pool.vec4(floats[iForAttr + 0], floats[iForAttr + 1], floats[iForAttr + 2], floats[iForAttr + 3]);
                vertexProcessor.processOther(v4, va);
                v4.emit(floats, iForAttr);
                break;
              default:
                PAssert.fail("Should not reach!");
                break;
            }
            break;
        }
      }
    }
    return floats;
  }

  /** Queues a vertex with the current vertex values. */
  public PMeshGen emitVertex() {
    queuedVertices.addAll(currentVertexValues);
    // Shrink the queue if it is too large.
    while (queuedVertices.size() > vertexAttributes.getNumFloatsPerVertex() * QUEUED_VERTICES_TO_KEEP) {
      queuedVertices.delFirstN(vertexAttributes.getNumFloatsPerVertex());
    }
    return this;
  }

  public PVec1 get(PVec1 out, String alias) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 1);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    return out.x(currentVertexValues[ind + 0]);
  }

  public PVec2 get(PVec2 out, String alias) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    return out.x(currentVertexValues[ind + 0]).y(currentVertexValues[ind + 1]);
  }

  public PVec3 get(PVec3 out, String alias) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    return out.x(currentVertexValues[ind + 0]).y(currentVertexValues[ind + 1]).z(currentVertexValues[ind + 2]);
  }

  public PVec4 get(PVec4 out, String alias) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    return out.x(currentVertexValues[ind + 0]).y(currentVertexValues[ind + 1]).z(currentVertexValues[ind + 2])
              .w(currentVertexValues[ind + 3]);
  }

  public PMesh getMesh() {
    PMesh ret = new PMesh(true, vertices(), indices(), vertexAttributes());
    return ret;
  }

  public PMeshGen quad(boolean flip) {
    __emitQueuedTriangle(3, flip ? 1 : 2, flip ? 2 : 1);
    __emitQueuedTriangle(3, flip ? 0 : 1, flip ? 1 : 0);
    return this;
  }

  /** Fully resets the meshGen. Resets all variables, unlike clear(). */
  public PMeshGen reset() {
    clear();
    clearClippingPlanes();
    queuedVertices.clear();
    Arrays.fill(currentVertexValues, 0);
    return this;
  }

  /** Clears the meshGen to have no commited vertices or indices. */
  public PMeshGen clear() {
    vertices().clear();
    indices().clear();
    numVertices = 0;
    return this;
  }

  /** Resets all clipping planes. */
  public PMeshGen clearClippingPlanes() {
    clippingPlanes.clearAndFreePooled();
    return this;
  }

  public PMeshGen set(String alias, float x) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 1);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues[ind + 0] = x;
    return this;
  }

  public PMeshGen set(String alias, PVec2 vec) {
    return set(alias, vec.x(), vec.y());
  }

  public PMeshGen set(String alias, float x, float y) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 2);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues[ind + 0] = x;
    currentVertexValues[ind + 1] = y;
    return this;
  }

  public PMeshGen set(String alias, PVec3 vec) {
    return set(alias, vec.x(), vec.y(), vec.z());
  }

  public PMeshGen set(String alias, float x, float y, float z) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 3);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues[ind + 0] = x;
    currentVertexValues[ind + 1] = y;
    currentVertexValues[ind + 2] = z;
    return this;
  }

  public PMeshGen set(String alias, PVec4 vec) {
    return set(alias, vec.x(), vec.y(), vec.z(), vec.w());
  }

  public PMeshGen set(String alias, float x, float y, float z, float w) {
    PAssert.equals(PVertexAttributes.Attribute.get(alias).numComponents, 4);
    int ind = vertexAttributes().indexForVertexAttribute(alias);
    currentVertexValues[ind + 0] = x;
    currentVertexValues[ind + 1] = y;
    currentVertexValues[ind + 2] = z;
    currentVertexValues[ind + 3] = w;
    return this;
  }

  public PMeshGen tri(boolean flip) {
    return __emitQueuedTriangle(2, flip ? 0 : 1, flip ? 1 : 0);
    //    indices().add((short) latestIndices().get(2));
    //    indices().add((short) latestIndices().get(flip ? 0 : 1));
    //    indices().add((short) latestIndices().get(flip ? 1 : 0));
  }

  /**
   * Processes and emits a triangle from the queued vertex buffer. The triangle will be clipped and thus can result in
   * 0, 1, or many actual emitted triangles.
   *
   * @param iOffsetFromLast0 The index offset from the end of of the queue for the first vertex of the triangle.
   * @param iOffsetFromLast1 The index offset from the end of of the queue for the second vertex of the triangle.
   * @param iOffsetFromLast2 The index offset from the end of of the queue for the third vertex of the triangle.
   */
  private PMeshGen __emitQueuedTriangle(int iOffsetFromLast0, int iOffsetFromLast1, int iOffsetFromLast2) {
    int fPerV = vertexAttributes.getNumFloatsPerVertex();
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      // First, process the vertices using the vertex processor.
      // Process the first vertex.
      float[] buffer0 = __tmpProcessingVertexValues[0];
      queuedVertices.emitTo(buffer0, queuedVertices.size() - (iOffsetFromLast0 + 1) * fPerV);
      PVec3 processedPos0 = pool.vec3();
      __processVertexWithVertexProcessor(buffer0, processedPos0);
      // Process the second vertex.
      float[] buffer1 = __tmpProcessingVertexValues[1];
      queuedVertices.emitTo(buffer1, queuedVertices.size() - (iOffsetFromLast1 + 1) * fPerV);
      PVec3 processedPos1 = pool.vec3();
      __processVertexWithVertexProcessor(buffer1, processedPos1);
      // Process the third vertex.
      float[] buffer2 = __tmpProcessingVertexValues[2];
      queuedVertices.emitTo(buffer2, queuedVertices.size() - (iOffsetFromLast2 + 1) * fPerV);
      PVec3 processedPos2 = pool.vec3();
      __processVertexWithVertexProcessor(buffer2, processedPos2);
      // Next, we need to slice the triangle using the clipping planes.
    }
    return this;
  }

  private PMeshGen __clipTriangles(PPlane plane, PList<>)
}
