package com.phonygames.pengine.graphics.model;

import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/** Override this class to be able to process vertices. */
public class PMeshGenVertexProcessor {
  /** Processes the normal vector in place. Make sure the parameter can be modified! */
  public PVec3 processNor(PVec3 rawPos, PVec3 inNor) {
    return inNor;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec1 processOther(PVec1 inVec1, PVertexAttribute vertexAttribute) {
    return inVec1;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec2 processOther(PVec2 inVec2, PVertexAttribute vertexAttribute) {
    return inVec2;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec3 processOther(PVec3 inVec3, PVertexAttribute vertexAttribute) {
    return inVec3;
  }

  /** Processes the vector in place. Make sure the parameter can be modified! */
  public PVec4 processOther(PVec4 inVec4, PVertexAttribute vertexAttribute) {
    return inVec4;
  }

  /** Processes the position vector in place. Make sure the parameter can be modified! */
  public PVec3 processPos(PVec3 inPos) {
    return inPos;
  }

  /** Vertex processor that transforms using a transformation matrix. */
  public static class Transform extends PMeshGenVertexProcessor {
    /** The transformation matrix. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PMat4 transform = PMat4.obtain();

    @Override public PVec3 processNor(PVec3 rawPos, PVec3 inNor) {
      return inNor.mul(transform, 0);
    }

    @Override public PVec3 processPos(PVec3 inPos) {
      inPos.mul(transform, 1);
      return inPos;
    }

    /** Resets the transformation matrix. */
    public Transform reset() {
      transform.idt();
      return this;
    }
  }

  /** Vertex processor that transforms using flat quad coordinates. */
  public static class FlatQuad extends PMeshGenVertexProcessor {
    /** The position of the 00 quad corner. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec3 flatQuad00 = PVec3.obtain();
    /** The position of the 01 quad corner. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec3 flatQuad01 = PVec3.obtain();
    /** The position of the 11 quad corner. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec3 flatQuad11 = PVec3.obtain();
    /** The position of the 10 quad corner. */
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private final PVec3 flatQuad10 = PVec3.obtain();

    @Override public PVec3 processNor(PVec3 rawPos, PVec3 inNor) {
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        // Calculate the x and z axes at the edges of the 1x1 quad (using the normal of the quad's edge)
        PVec3 xAtX0 = pool.vec3().set(flatQuad01.z() - flatQuad00.z(), 0, flatQuad00.x() - flatQuad01.x());
        PVec3 xAtX1 = pool.vec3().set(flatQuad11.z() - flatQuad10.z(), 0, flatQuad10.x() - flatQuad11.x());
        PVec3 zAtZ0 = pool.vec3().set(flatQuad00.z() - flatQuad10.z(), 0, flatQuad10.x() - flatQuad00.x());
        PVec3 zAtZ1 = pool.vec3().set(flatQuad01.z() - flatQuad11.z(), 0, flatQuad11.x() - flatQuad01.x());
        PVec3 yNor0 = pool.vec3().set(flatQuad11).sub(flatQuad00).crs(pool.vec3().set(flatQuad10).sub(flatQuad00));
        PVec3 yNor1 = pool.vec3().set(flatQuad01).sub(flatQuad00).crs(pool.vec3().set(flatQuad11).sub(flatQuad00));
        PVec3 xNor = pool.vec3().set(xAtX0).lerp(xAtX1, rawPos.x());
        PVec3 yNor = pool.vec3().set(yNor0).lerp(yNor1, 0.5f); // Average of the normals for triangles 012 and 023.
        PVec3 zNor = pool.vec3().set(zAtZ0).lerp(zAtZ1, rawPos.z());
        float x = xNor.x() * inNor.x() + yNor.x() * inNor.y() + zNor.x() * inNor.z();
        float y = xNor.y() * inNor.x() + yNor.y() * inNor.y() + zNor.y() * inNor.z();
        float z = xNor.z() * inNor.x() + yNor.z() * inNor.y() + zNor.z() * inNor.z();
        return inNor.set(x, y, z).nor();
      }
    }

    @Override public PVec3 processPos(PVec3 inPos) {
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        PVec3 lerpPosX0 = pool.vec3().set(flatQuad00).lerp(flatQuad01, inPos.z());
        PVec3 lerpPosX1 = pool.vec3().set(flatQuad10).lerp(flatQuad11, inPos.z());
        PVec3 lerpPosXZ = pool.vec3().set(lerpPosX0).lerp(lerpPosX1, inPos.x());
        float outY = lerpPosXZ.y() + inPos.y();
        inPos.set(lerpPosXZ.x(), outY, lerpPosXZ.z());
      }
      return inPos;
    }

    /** Resets the flat quad corners. */
    public FlatQuad reset() {
      flatQuad00.setZero();
      flatQuad01.setZero();
      flatQuad11.setZero();
      flatQuad10.setZero();
      return this;
    }
  }
}
