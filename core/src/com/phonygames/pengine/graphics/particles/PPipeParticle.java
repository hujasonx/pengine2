package com.phonygames.pengine.graphics.particles;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.graphics.PDebugRenderer;
import com.phonygames.pengine.graphics.color.PColor;
import com.phonygames.pengine.graphics.model.PMeshTopology;
import com.phonygames.pengine.graphics.model.PVertexAttributes;
import com.phonygames.pengine.graphics.texture.PTexture;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PFloatList;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PMap;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PShortList;
import com.phonygames.pengine.util.PVecTracker;
import com.phonygames.pengine.util.Tuple3;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A particle that is composed of a capped ellipse cylinder with varying shape.
 */
public class PPipeParticle extends PParticle {
  /**
   * A map of the form: [numRings][ringSteps]. The tuples include the mesh topology, the real vertices, and the real
   * indices.
   */
  private static final PMap<Integer, PMap<Integer, Tuple3<PMeshTopology, PFloatList, PShortList>>>
      staticMeshAndTopologyMap = new PMap<Integer, PMap<Integer, Tuple3<PMeshTopology, PFloatList, PShortList>>>() {
    @Override public PMap<Integer, Tuple3<PMeshTopology, PFloatList, PShortList>> newUnpooled(Integer i) {
      return new PMap<>();
    }
  };
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private static final PPool<PPipeParticle> staticPool = new PPool<PPipeParticle>() {
    @Override protected PPipeParticle newObject() {
      return new PPipeParticle();
    }
  };
  /** An array used to store canonical topology vertex positions. */
  private final PList<PVec3> canonicalTopologyVertices = new PList<>(PVec3.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec4 col0 = PVec4.obtain(), col1 = PVec4.obtain(), col2 = PVec4.obtain(), col3 = PVec4.obtain();
  /** The color of the head. */
  private final PVec4 headCol = PVec4.obtain();
  /** The intermediate color data. */
  private final PList<PVec4> intermediateColorData = new PList<>(PVec4.getStaticPool());
  /** The center normals for theta 0. */
  private final PList<PVec3> intermediateNormal0Data = new PList<>(PVec3.getStaticPool());
  /** The center normals for theta PI / 2. */
  private final PList<PVec3> intermediateNormal1Data = new PList<>(PVec3.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The times, between 0 and 1, where intermediate rings should be placed. */ private final PFloatList
      intermediatePointNormalizedTimes = PFloatList.obtain();
  /** The center positions. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PList<PVec3> intermediatePositionData = new PList<>(PVec3.getStaticPool());
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The ring radii. */ private final PFloatList intermediateRingRadii = PFloatList.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** Helper to track the position of the particle. */ private final PVecTracker<PVec3> previousPositionTracker =
      new PVecTracker(PVec3.getStaticPool());
  private final PShortList rawIndexData = PShortList.obtain();
  private final PFloatList rawVertexData = PFloatList.obtain();
  /** A stable vector used to calculate the normals. */
  private final PVec3 startNormal = PVec3.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  /** The last vertex. */ private final PVec3 tail = PVec3.obtain();
  /** The color of the tail. */
  private final PVec4 tailCol = PVec4.obtain();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PTexture texture = new PTexture();
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final float[] userData = new float[16];
  private int lastMeshDataUpdateFrame = -1;
  /** The number of intermediate values. */
  private int numRings = -1;
  /** The number of faces that make up a ring. */
  private int ringSteps = -1;
  /** The topology object for this particle; should not modify them - so we should get them from a static context. */
  private PMeshTopology topology;

  private PPipeParticle() {
    reset();
    previousPositionTracker.trackedVec(pos);
  }

  public PPipeParticle beginTracking(float previousPositionsTrackingDuration, int previousPositionsToKeep) {
    previousPositionTracker.beginTracking(pos, previousPositionsTrackingDuration, previousPositionsToKeep);
    return this;
  }

  @Override public void frameUpdateShared() {
    super.frameUpdateShared();
    previousPositionTracker.frameUpdate();
    if (lastMeshDataUpdateFrame != PEngine.frameCount) {
      lastMeshDataUpdateFrame = PEngine.frameCount;
      updateRawMeshData();
    }
  }

  /** Updates the rawVertexData and rawIndexData lists with values based on the head, intermediate, and tail data. */
  protected void updateRawMeshData() {
    PAssert.isFalse(numRings == -1 || ringSteps == -1, "Ring settings not applied!");
    // The first two canonical vertices are the head and tail. Then, vertices wrap around per ring, then to the next.
    // The edge where the ring connects to the beginning shares canonical vertices (at theta 0 and 2 pi.)
    canonicalTopologyVertices.get(0).set(pos);
    previousPositionTracker.getPreviousPositionNormalizedTime(tail, 1);
    canonicalTopologyVertices.get(1).set(tail);
    if (intermediatePointNormalizedTimes.size() != numRings || intermediateRingRadii.size() != numRings) {
      PAssert.warn(
          "Intermediate point normalized time or radii buffers were not initialized with the correct size (numRings): " +
          numRings);
      return;
    }
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      final float thetaPerStep = MathUtils.PI2 / ringSteps;
      PVec3 normalTheta0 = pool.vec3(startNormal);
      PVec3 centerDelta = pool.vec3();
      PVec3 nor0 = pool.vec3();
      PVec3 nor1 = pool.vec3();
      PDebugRenderer.line(pos, 0, 0, pos, 0, 10, PColor.GREEN, PColor.GREEN, 10, 1);
      int canonicalIndexIndex = 2;
      for (int ringNo = 0; ringNo < numRings; ringNo++) {
        PVec3 ringCenter = previousPositionTracker.getPreviousPositionNormalizedTime(pool.vec3(),
                                                                                     intermediatePointNormalizedTimes().get(
                                                                                         ringNo));
        centerDelta.set(ringCenter);
        if (ringNo == 0) {
          centerDelta.sub(pos);
        } else {
          centerDelta.sub(previousPositionTracker.getPreviousPositionNormalizedTime(pool.vec3(),
                                                                                    intermediatePointNormalizedTimes().get(
                                                                                        ringNo - 1)));
        }
//        PDebugRenderer.line(ringCenter, pool.vec3(ringCenter).add(centerDelta), PColor.RED, PColor.CYAN, 2, 2);
        PDebugRenderer.line(ringCenter, 0, 0, ringCenter, 0, 10, PColor.RED, PColor.CYAN, 2 + 8 * ringNo, 2 + 8 * ringNo);
        for (float theta = 0; theta < MathUtils.PI2 - thetaPerStep * .5f; theta += thetaPerStep) {
          nor0.set(normalTheta0).crs(centerDelta).crs(centerDelta).nor().scl(intermediateRingRadii().get(ringNo));
          nor1.set(nor0).rotate(centerDelta, MathUtils.HALF_PI).nor().scl(intermediateRingRadii().get(ringNo));
          canonicalTopologyVertices.get(canonicalIndexIndex).set(ringCenter)
                                   .add(nor0, MathUtils.cos(theta))
                                   .add(nor1, MathUtils.sin(theta));
          canonicalIndexIndex++;
        }
      }
      PAssert.isTrue(canonicalIndexIndex == 2 + numRings * ringSteps);
    }
    PAssert.isNotNull(topology, "Call updateTopology first!");
    topology.apply(canonicalTopologyVertices, rawVertexData, PVertexAttributes.getGLTF_UNSKINNED());
  }

  @Override public void reset() {
    super.reset();
    lastMeshDataUpdateFrame = -1;
    col0.set(PVec4.ONE);
    col1.set(PVec4.ONE);
    col2.set(PVec4.ONE);
    col3.set(PVec4.ONE);
    Arrays.fill(userData, 0f);
    texture.reset();
    headCol.set(PVec4.ONE);
    tailCol.set(PVec4.ONE);
    tail.setZero();
    intermediateColorData.clearAndFreePooled();
    intermediateNormal0Data.clearAndFreePooled();
    intermediateNormal1Data.clearAndFreePooled();
    intermediatePositionData.clearAndFreePooled();
    startNormal.set(PVec3.Y);
    intermediateRingRadii.clear();
    intermediatePointNormalizedTimes.clear();
    numRings = -1;
    ringSteps = -1;
    rawVertexData.clear();
    rawIndexData.clear();
    canonicalTopologyVertices.clearAndFreePooled();
    previousPositionTracker.reset();
    topology = null;
  }

  protected int indicesShortCount() {
    return rawIndexData.size();
  }

  protected boolean outputVertexAndIndexData(float[] vertices, int vertexOffset, short[] indices, int indexOffset) {
    // Buffer is full!
    if (vertexOffset + rawVertexData.size() >= vertices.length || indexOffset + rawIndexData.size() >= indices.length) {
      return false;
    }
    rawVertexData.emitTo(vertices, vertexOffset);
    rawIndexData.emitTo(indices, indexOffset);
    return true;
  }

  public PPipeParticle setColFrom0() {
    col1.set(col0);
    col2.set(col0);
    col3.set(col0);
    return this;
  }

  public void updateTopology(int numRings, int ringSteps) {
    if (numRings == this.numRings && ringSteps == this.ringSteps && this.topology != null) {
      // Return early if it the number of rings or ring steps has not changed.
      return;
    }
    this.numRings = numRings;
    this.ringSteps = ringSteps;
    lastMeshDataUpdateFrame = -1; // Force an update to the mesh.
    Tuple3<PMeshTopology, PFloatList, PShortList> meshAndTopologyData =
        staticMeshAndTopologyMap.genUnpooled(numRings).get(ringSteps);
    if (meshAndTopologyData == null) {
      PFloatList newVertexData = PFloatList.obtain();
      PShortList newIndexData = PShortList.obtain();
      PMeshTopology.Builder topologyBuilder = new PMeshTopology.Builder();
      // Add the vertices and indices.
      // We include an extra edge here so that u can equal 1.
      int verticesPerRing = ringSteps + 1;
      float dU = MathUtils.PI2 / ringSteps;
      float dV = 1f / (numRings + 1);
      int vertexIndex = 0;
      // The head and tail are actually rings themselves, but with 0 radius.
      for (float v = 0, vIndex = 0; vIndex < numRings + 2; vIndex++, v += dV) {
        boolean isHead = vIndex == 0;
        boolean isTail = vIndex == numRings + 1;
        for (float u = 0, uIndex = 0; uIndex < verticesPerRing; uIndex++, u += dU) {
          // Pos, nor, uv, color. The only things that need to be set is the uv.
          newVertexData.add(0); // Pos x.
          newVertexData.add(0); // Pos y.
          newVertexData.add(0); // Pos z.
          newVertexData.add(0); // Nor x.
          newVertexData.add(0); // Nor y.
          newVertexData.add(0); // Nor z.
          newVertexData.add(u); // U.
          newVertexData.add(v); // V.
          newVertexData.add(1); // Col r.
          newVertexData.add(1); // Col g.
          newVertexData.add(1); // Col b.
          newVertexData.add(1); // Col a.
          // Add the indices for the quad that has this vertex as its last corner.
          if (!isHead && uIndex != 0) {
            newIndexData.add((short) (vertexIndex));
            newIndexData.add((short) (vertexIndex - 1));
            newIndexData.add((short) (vertexIndex - 1 - verticesPerRing));
            newIndexData.add((short) (vertexIndex));
            newIndexData.add((short) (vertexIndex - 1 - verticesPerRing));
            newIndexData.add((short) (vertexIndex - verticesPerRing));
          }
          vertexIndex++;
        }
      }
      // Add the shared index data for the head.
      topologyBuilder.addCanonicalIndex((short) 0);
      for (int a = 1; a <= ringSteps; a++) {
        topologyBuilder.addSharedVertexIndex(0, (short) a);
      }
      // Add the shared index data for the tail.
      topologyBuilder.addCanonicalIndex((short) ((numRings + 1) * verticesPerRing));
      for (int a = 1; a <= ringSteps; a++) {
        topologyBuilder.addSharedVertexIndex(1, (short) ((numRings + 1) * verticesPerRing + a));
      }
      // Add the shared index data for the intermediate rings.
      int canonicalIndexIndex = 2;
      for (int ringNo = 0; ringNo < numRings; ringNo++) {
        for (int ringStep = 0; ringStep < ringSteps; ringStep++) {
          // The first vertex of each intermediate ring is canonical for the last.
          topologyBuilder.addCanonicalIndex((short) ((ringNo + 1) * verticesPerRing + ringStep));
          if (ringStep == 0) {
            topologyBuilder.addSharedVertexIndex(canonicalIndexIndex, (short) ((ringNo + 2) * verticesPerRing - 1));
          }
          canonicalIndexIndex++;
        }
      }
      PAssert.isTrue(canonicalIndexIndex == 2 + numRings * ringSteps);
      // Create the mesh and topology data if necessary.
      meshAndTopologyData =
          new Tuple3<>(topologyBuilder.buildWithOriginalTriangles(newIndexData.emit()), newVertexData, newIndexData);
      staticMeshAndTopologyMap.get(numRings).put(ringSteps, meshAndTopologyData);
    }
    // Set the raw vertex and index data.
    topology = meshAndTopologyData.a();
    rawVertexData.clear();
    rawVertexData.addAll(meshAndTopologyData.b());
    rawIndexData.clear();
    rawIndexData.addAll(meshAndTopologyData.c());
    canonicalTopologyVertices.clearAndFreePooled();
    canonicalTopologyVertices.fillToCapacityWithPooledValues(2 + numRings * ringSteps);
  }

  public int verticesFloatCount() {
    return rawVertexData.size();
  }
}
