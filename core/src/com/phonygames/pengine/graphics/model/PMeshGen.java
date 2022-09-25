package com.phonygames.pengine.graphics.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.physics.bullet.collision.btBvhTriangleMeshShape;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.math.PPlane;
import com.phonygames.pengine.math.PTriangle;
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
  /** Whether or not this mesh should be alpha blended. */
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private boolean alphaBlend;
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
  /** Temp buffer that stores 3 index shorts for a triangle. */
  private final short[] __tmpTriIndexShorts = new short[3];
  /** A list of triangles in model space that are attempting to be emitted. */
  private final PList<PTriangle> __triangleBuffer = new PList<>(PTriangle.getStaticPool());
  /** A second triangle buffer for ping ponging. */
  private final PList<PTriangle> __triangleBuffer2 = new PList<>(PTriangle.getStaticPool());
  /** The clipping planes. They will all be ANDed together to clip emitted triangles in model space. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<PPlane> clippingPlanes = new PList<PPlane>(PPlane.getStaticPool());
  /** The current float values for queueing. */
  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  private final float[] currentVertexValues;
  /** The indices that have been committed. */
  @Getter(value = AccessLevel.PUBLIC)
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
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PFloatList vertices = PFloatList.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private @Nullable
  FinalPassVertexProcessor finalPassVertexProcessor;
  /** The number of vertices to look back for reuse. */
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int maxVertexLookbackAmount = 1000;
  /** The number of vertices. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int numVertices = 0;
  /** The PMeshGenVertexProcessor to use. Processes queued vertices. */
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private @Nullable
  PMeshGenVertexProcessor vertexProcessor;

  protected PMeshGen(@NonNull String name, @NonNull PVertexAttributes vertexAttributes) {
    this.name = name;
    this.vertexAttributes = vertexAttributes;
    currentVertexValues = new float[vertexAttributes.sizeInFloats()];
    for (int a = 0; a < __tmpProcessingVertexValues.length; a++) {
      __tmpProcessingVertexValues[a] = new float[vertexAttributes.sizeInFloats()];
    }
    reset();
  }

  /** Fully resets the meshGen. Resets all variables, unlike clear(). */
  public PMeshGen reset() {
    clear();
    clearClippingPlanes();
    __triangleBuffer.clearAndFreePooled();
    queuedVertices.clear();
    Arrays.fill(currentVertexValues, 0);
    maxVertexLookbackAmount = 10000;
    vertexProcessor = null;
    finalPassVertexProcessor = null;
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

  /** Adds a copy of the given mesh. */
  public PMeshGen addMeshCopy(PMesh mesh) {
    float[] meshFloats = mesh.getBackingMeshFloats();
    short[] meshShorts = mesh.getBackingMeshShorts();
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec4 temp4 = pool.vec4();
      // Loop through all triangles and emit them.
      for (int a = 0; a < meshShorts.length; a += 3) {
        // Loop through the three vertices of the triangle.
        for (int triI = 0; triI < 3; triI++) {
          short indexOfVertexInMeshToCopy = meshShorts[a + triI];
          int vertexOffsetInFloatsArrayForMeshToCopy =
              indexOfVertexInMeshToCopy * mesh.vertexAttributes().sizeInFloats();
          // Emit the vertex in self by copying data per vertex attribute.
          for (int b = 0; b < vertexAttributes.backingVertexAttributes().size(); b++) {
            VertexAttribute selfVA = vertexAttributes.backingVertexAttributes().get(b);
            PVertexAttribute meshVA = mesh.vertexAttributes().attributes().get(selfVA.alias);
            if (meshVA == null) {continue;}
            int originalIForAttr = mesh.vertexAttributes().floatIndexForVertexAttribute(selfVA);
            /** The base index for the vertex attribute data for this vertex in the original mesh floats. */
            int copyI = vertexOffsetInFloatsArrayForMeshToCopy + originalIForAttr;
            int vertexSizeForAttribute = selfVA.getSizeInBytes() / 4;
            switch (vertexSizeForAttribute) {
              case 1:
                set(selfVA.alias, meshFloats[copyI]);
                break;
              case 2:
                // TODO: support unsigned byte and unsigned short here as well.
                set(selfVA.alias, meshFloats[copyI + 0], meshFloats[copyI + 1]);
                break;
              case 3:
                set(selfVA.alias, meshFloats[copyI + 0], meshFloats[copyI + 1], meshFloats[copyI + 2]);
                break;
              case 4:
                if (meshVA.definition().type == PVertexAttribute.Type.UNSIGNED_BYTE) {
                  PVertexAttribute.vec4FromUnsignedByteColor(temp4, meshFloats[copyI + 0]);
                } else if (meshVA.definition().type == PVertexAttribute.Type.UNSIGNED_SHORT) {
                  PVertexAttribute.vec4FromUnsignedShortColor(temp4, meshFloats[copyI + 0], meshFloats[copyI + 1]);
                } else {
                  temp4.set(meshFloats[copyI + 0], meshFloats[copyI + 1], meshFloats[copyI + 2], meshFloats[copyI + 3]);
                }
                set(selfVA.alias, temp4);
                break;
              default:
                PAssert.fail("Should not reach!");
                break;
            }
          }
          // Now emit the vertex.
          emitVertex();
        }
        // Now emit the triangle.
        tri(false);
      }
    }
    return this;
  }

  public PMeshGen set(String alias, float x) {
    int ind = vertexAttributes().floatIndexForVertexAttribute(alias);
    currentVertexValues[ind + 0] = x;
    return this;
  }

  public PMeshGen set(String alias, float x, float y) {
    int ind = vertexAttributes().floatIndexForVertexAttribute(alias);
    currentVertexValues[ind + 0] = x;
    currentVertexValues[ind + 1] = y;
    return this;
  }

  public PMeshGen set(String alias, float x, float y, float z) {
    int ind = vertexAttributes().floatIndexForVertexAttribute(alias);
    currentVertexValues[ind + 0] = x;
    currentVertexValues[ind + 1] = y;
    currentVertexValues[ind + 2] = z;
    return this;
  }

  public PMeshGen set(String alias, float x, float y, float z, float w) {
    int ind = vertexAttributes().floatIndexForVertexAttribute(alias);
    currentVertexValues[ind + 0] = x;
    currentVertexValues[ind + 1] = y;
    currentVertexValues[ind + 2] = z;
    currentVertexValues[ind + 3] = w;
    return this;
  }

  /** Queues a vertex with the current vertex values. */
  public PMeshGen emitVertex() {
    queuedVertices.addAll(currentVertexValues);
    // Shrink the queue if it is too large.
    while (queuedVertices.size() > vertexAttributes.sizeInFloats() * QUEUED_VERTICES_TO_KEEP) {
      queuedVertices.delFirstN(vertexAttributes.sizeInFloats());
    }
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
    int fPerV = vertexAttributes.sizeInFloats();
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      // First, process the vertices using the vertex processor.
      // Process the first vertex.
      float[] buffer0 = __tmpProcessingVertexValues[0];
      queuedVertices.emitTo(buffer0, 0, queuedVertices.size() - (iOffsetFromLast0 + 1) * fPerV, buffer0.length);
      PVec3 processedPos0 = pool.vec3();
      __processVertexWithVertexProcessor(buffer0, processedPos0);
      // Process the second vertex.
      float[] buffer1 = __tmpProcessingVertexValues[1];
      queuedVertices.emitTo(buffer1, 0, queuedVertices.size() - (iOffsetFromLast1 + 1) * fPerV, buffer1.length);
      PVec3 processedPos1 = pool.vec3();
      __processVertexWithVertexProcessor(buffer1, processedPos1);
      // Process the third vertex.
      float[] buffer2 = __tmpProcessingVertexValues[2];
      queuedVertices.emitTo(buffer2, 0, queuedVertices.size() - (iOffsetFromLast2 + 1) * fPerV, buffer2.length);
      PVec3 processedPos2 = pool.vec3();
      __processVertexWithVertexProcessor(buffer2, processedPos2);
      // Next, we need to slice the triangle using the clipping planes.
      PTriangle originalProcessedTri = PTriangle.obtain().set(processedPos0, processedPos1, processedPos2);
      __triangleBuffer.genPooledAndAdd().set(originalProcessedTri);
      for (int a = 0; a < clippingPlanes.size(); a++) {
        __clipTriangles(clippingPlanes.get(a), __triangleBuffer);
      }
      // Finally, emit the triangles.
      float[] outBuffer0 = __tmpProcessingVertexValues[3];
      float[] outBuffer1 = __tmpProcessingVertexValues[4];
      float[] outBuffer2 = __tmpProcessingVertexValues[5];
      for (int a = 0; a < __triangleBuffer.size(); a++) {
        PTriangle tri = __triangleBuffer.get(a);
        PVec3 bary0 = originalProcessedTri.cartToBary(pool.vec3(), tri.pos(0));
        PVec3 bary1 = originalProcessedTri.cartToBary(pool.vec3(), tri.pos(1));
        PVec3 bary2 = originalProcessedTri.cartToBary(pool.vec3(), tri.pos(2));
        __setVertexDataFromBarycentric(outBuffer0, bary0, buffer0, buffer1, buffer2);
        __setVertexDataFromBarycentric(outBuffer1, bary1, buffer0, buffer1, buffer2);
        __setVertexDataFromBarycentric(outBuffer2, bary2, buffer0, buffer1, buffer2);
        short i0 = __finalizeVertex(outBuffer0, maxVertexLookbackAmount);
        short i1 = __finalizeVertex(outBuffer1, maxVertexLookbackAmount);
        short i2 = __finalizeVertex(outBuffer2, maxVertexLookbackAmount);
        indices.add(i0);
        indices.add(i1);
        indices.add(i2);
      }
      __triangleBuffer.clearAndFreePooled();
      originalProcessedTri.free();
    }
    return this;
  }

  /**
   * Processes the vertex with the vertex processor, if one is set.
   *
   * @param floats The unprocessed float data for the vertex to add. Processed in place.
   * @param outPos A vector that will be set to the output position.
   */
  private float[] __processVertexWithVertexProcessor(float[] floats, @Nullable PVec3 outPos) {
    /** The index in the float array of the position. */
    int posI = vertexAttributes.floatIndexForVertexAttribute(PVertexAttribute.Definitions.pos.alias);
    if (vertexProcessor == null) {
      if (outPos != null) {
        outPos.set(floats[posI + 0], floats[posI + 1], floats[posI + 2]);
      }
      return floats;
    }
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 rawPos = pool.vec3(floats[posI + 0], floats[posI + 1], floats[posI + 2]);
      // Loop through the vertex attributes and process each one.
      for (int a = 0; a < vertexAttributes.count(); a++) {
        VertexAttribute va = vertexAttributes.backingVertexAttributes().get(a);
        int iForAttr = vertexAttributes.floatIndexForVertexAttribute(va);
        switch (va.usage) {
          case VertexAttributes.Usage.Position: // Handle position.
            PVec3 pos = pool.vec3(rawPos);
            vertexProcessor.processPos(pos);
            if (outPos != null) {outPos.set(pos);}
            pos.emit(floats, iForAttr);
            break;
          case VertexAttributes.Usage.Normal: // Handle normal.
            PVec3 nor = pool.vec3(floats[iForAttr + 0], floats[iForAttr + 1], floats[iForAttr + 2]);
            vertexProcessor.processNor(rawPos, nor);
            nor.emit(floats, iForAttr);
            break;
          default: // Handle everything else.
            int vertexSizeForAttribute = va.getSizeInBytes() / 4;
            switch (vertexSizeForAttribute) {
              case 1:
                PVec1 v1 = pool.vec1(floats[iForAttr]);
                vertexProcessor.processOther(v1, vertexAttributes.pva(a));
                floats[iForAttr] = v1.x();
                break;
              case 2:
                PVec2 v2 = pool.vec2(floats[iForAttr + 0], floats[iForAttr + 1]);
                vertexProcessor.processOther(v2, vertexAttributes.pva(a));
                v2.emit(floats, iForAttr);
                break;
              case 3:
                PVec3 v3 = pool.vec3(floats[iForAttr + 0], floats[iForAttr + 1], floats[iForAttr + 2]);
                vertexProcessor.processOther(v3, vertexAttributes.pva(a));
                v3.emit(floats, iForAttr);
                break;
              case 4:
                PVec4 v4 =
                    pool.vec4(floats[iForAttr + 0], floats[iForAttr + 1], floats[iForAttr + 2], floats[iForAttr + 3]);
                vertexProcessor.processOther(v4, vertexAttributes.pva(a));
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

  /** Clips the triangles with the given plane, modifying the triangles list in place. */
  private PMeshGen __clipTriangles(PPlane plane, PList<PTriangle> triangles) {
    // Transfer all the triangles to triangleBuffer2, which will be responsible for freeing pooled vecs.
    __triangleBuffer2.clear();
    __triangleBuffer2.addAll(triangles);
    triangles.clear();
    for (int a = 0; a < __triangleBuffer2.size(); a++) {
      PTriangle tri = __triangleBuffer2.get(a);
      tri.clipWithPlane(plane, triangles);
    }
    __triangleBuffer2.clearAndFreePooled();
    return this;
  }

  /**
   * Sets the output buffer with the float data corresponding to the given barycentric coordinates and positions.
   *
   * @param outBuffer  The buffer to output to.
   * @param bary       The barycentric coordinates of the original triangle.
   * @param triBuffer0 The buffer of the first vertex of the original triangle.
   * @param triBuffer1 The buffer of the second vertex of the original triangle.
   * @param triBuffer2 The buffer of the third vertex of the original triangle.
   * @return
   */
  private static void __setVertexDataFromBarycentric(float[] outBuffer, PVec3 bary, float[] triBuffer0,
                                                     float[] triBuffer1, float[] triBuffer2) {
    for (int a = 0; a < outBuffer.length; a++) {
      outBuffer[a] = triBuffer0[a] * bary.x() + triBuffer1[a] * bary.y() + triBuffer2[a] * bary.z();
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
    if (finalPassVertexProcessor != null) {
      finalPassVertexProcessor.process(processedVertexValues);
    }
    short index = (short) numVertices;
    for (int lookbackAmount = 1; lookbackAmount < maxLookbackAmount; lookbackAmount++) {
      int lookbackIndex = index - lookbackAmount;
      if (lookbackIndex < 0) {
        break;
      }
      int verticesIndex = lookbackIndex * vertexAttributes().sizeInFloats();
      boolean isEqual = true;
      for (int a = 0; a < vertexAttributes().sizeInFloats(); a++) {
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
      for (int a = 0; a < vertexAttributes().sizeInFloats(); a++) {
        vertices().add(processedVertexValues[a]);
      }
    }
    return index;
  }

  public PVec1 get(PVec1 out, String alias) {
    int ind = vertexAttributes().floatIndexForVertexAttribute(alias);
    return out.x(currentVertexValues[ind + 0]);
  }

  public PVec2 get(PVec2 out, String alias) {
    int ind = vertexAttributes().floatIndexForVertexAttribute(alias);
    return out.x(currentVertexValues[ind + 0]).y(currentVertexValues[ind + 1]);
  }

  public PVec3 get(PVec3 out, String alias) {
    int ind = vertexAttributes().floatIndexForVertexAttribute(alias);
    return out.x(currentVertexValues[ind + 0]).y(currentVertexValues[ind + 1]).z(currentVertexValues[ind + 2]);
  }

  public PVec4 get(PVec4 out, String alias) {
    int ind = vertexAttributes().floatIndexForVertexAttribute(alias);
    return out.x(currentVertexValues[ind + 0]).y(currentVertexValues[ind + 1]).z(currentVertexValues[ind + 2])
              .w(currentVertexValues[ind + 3]);
  }

  public PMesh getMesh() {
    PMesh ret = new PMesh(true, vertices(), indices(), vertexAttributes());
    return ret;
  }

  /** Returns a bullet triangle mesh shape. */
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

  /** Returns true if this meshGen has no vertices. */
  public boolean isEmpty() {
    return numVertices == 0;
  }

  public PMeshGen quad(boolean flip) {
    __emitQueuedTriangle(3, flip ? 1 : 2, flip ? 2 : 1);
    __emitQueuedTriangle(3, flip ? 0 : 1, flip ? 1 : 0);
    return this;
  }

  public PMeshGen set(String alias, PVec2 vec) {
    return set(alias, vec.x(), vec.y());
  }

  public PMeshGen set(String alias, PVec3 vec) {
    return set(alias, vec.x(), vec.y(), vec.z());
  }

  public PMeshGen set(String alias, PVec4 vec) {
    return set(alias, vec.x(), vec.y(), vec.z(), vec.w());
  }

  /** Class that modifies vertexProcessed vertex data per vertex. */
  public abstract static class FinalPassVertexProcessor {
    /** Processes the vertex. This happens immediate before they are emitted in model space. */
    public abstract void process(float[] vertexFloats);
  }
}
