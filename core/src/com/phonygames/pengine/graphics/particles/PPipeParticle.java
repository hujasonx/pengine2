package com.phonygames.pengine.graphics.particles;

import com.phonygames.pengine.PEngine;
import com.phonygames.pengine.exception.PAssert;
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
      staticMeshAndTopologyMap = new PMap<>() {
    @Override public PMap<Integer, Tuple3<PMeshTopology, PFloatList, PShortList>> newUnpooled(Integer i) {
      return null;
    }
  };
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private static final PPool<PPipeParticle> staticPool = new PPool<PPipeParticle>() {
    @Override protected PPipeParticle newObject() {
      return new PPipeParticle();
    }
  };
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
  /** The center positions. */
  private final PList<PVec3> intermediatePositionData = new PList<>(PVec3.getStaticPool());
  /** An array used to store canonical topology vertex positions. */
  private final PList<PVec3> canonicalTopologyVertices = new PList<>(PVec3.getStaticPool());
  private final PShortList rawIndexData = PShortList.obtain();
  private final PFloatList rawVertexData = PFloatList.obtain();
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
  private int numRings;
  /** The number of faces that make up a ring. */
  private int ringSteps;
  /** The topology object for this particle; should not modify them - so we should get them from a static context. */
  private PMeshTopology topology;

  private PPipeParticle() {
    reset();
  }

  @Override public void frameUpdateShared() {
    super.frameUpdateShared();
    if (lastMeshDataUpdateFrame != PEngine.frameCount) {
      lastMeshDataUpdateFrame = PEngine.frameCount;
      updateRawMeshData();
    }
  }

  /** Updates the rawVertexData and rawIndexData lists with values based on the head, intermediate, and tail data. */
  protected void updateRawMeshData() {
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
    numRings = -1;
    ringSteps = -1;
    rawVertexData.clear();
    rawIndexData.clear();
    canonicalTopologyVertices.clearAndFreePooled();
    topology = null;
  }

  public int indicesShortCount() {
    return rawIndexData.size();
  }

  public PPipeParticle setColFrom0() {
    col1.set(col0);
    col2.set(col0);
    col3.set(col0);
    return this;
  }

  public boolean setVertexAndIndexData(float[] vertices, int vertexOffset, short[] indices,
                                       int indexOffset) {
    // Buffer is full!
    if (vertexOffset + rawVertexData.size() >= vertices.length || indexOffset + rawIndexData.size() >= indices.length) {
      return false;
    }
    PAssert.isNotNull(topology, "Call updateTopology first!");
    topology.apply(canonicalTopologyVertices,rawVertexData, PVertexAttributes.getGLTF_UNSKINNED());
    rawVertexData.emitTo(vertices, vertexOffset);
    rawIndexData.emitTo(indices, indexOffset);
    return true;
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
      // TODO: add the topology data here.
      PMeshTopology.Builder topologyBuilder = new PMeshTopology.Builder();
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
  }

  public int verticesFloatCount() {
    return rawVertexData.size();
  }
}
