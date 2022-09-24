package com.phonygames.pengine.math;

import android.support.annotation.Nullable;

import com.phonygames.pengine.util.PBasic;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/** A plane with a normal and origin. */
public class PPlane extends PBasic<PPlane> {
  @Getter(value = AccessLevel.PUBLIC)
  private static final PPool<PPlane> staticPool = new PPool<PPlane>() {
    @Override protected PPlane newObject() {
      return new PPlane();
    }
  };
  /** Temp vector. */
  private final PVec3 __tempV3 = PVec3.obtain();
  /** The normal of the plane. Users are responsible for ensuring it is normalized!!! */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 normal = PVec3.obtain();
  /** The origin of the plane. */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final PVec3 origin = PVec3.obtain();

  public static PPlane obtain() {
    return getStaticPool().obtain();
  }

  @Override public int compareTo(PPlane pPlane) {
    int ct0 = origin.compareTo(pPlane.origin);
    if (ct0 != 0) {
      return ct0;
    }
    return normal.compareTo(pPlane.normal);
  }

  @Override public boolean equalsT(PPlane pPlane) {
    return pPlane.origin.equalsT(origin) && pPlane.normal.equalsT(normal);
  }

  /**
   * Returns the point on this plane that intersects the line formed by the input points. If there is no intersection,
   * returns null.
   */
  public @Nullable PVec3 lineIntersection(PVec3 out, PVec3 lineA, PVec3 lineB) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 lineDir = pool.vec3(lineB).sub(lineA).nor();
      if (PNumberUtils.epsilonEquals(lineDir.dot(normal), 0)) {
        return null;
      }
      float t = (normal.dot(origin) - normal.dot(lineA)) / normal.dot(lineDir);
      out.set(lineA).add(lineDir, t);
    }
    return out;
  }

  /** Returns true if the input point is on the plane. */
  public boolean onPlane(PVec3 p) {
    return PNumberUtils.epsilonEquals(signedDist(p), 0);
  }

  /**
   * Returns the distance from the point to the surface of the plane. If the plane's normal points towards the point,
   * the returned value is positive, else it is negative. The plane normal must be normalized for this to return the
   * correct value.
   *
   * @param p
   * @return
   */
  public float signedDist(PVec3 p) {
    __tempV3.set(p).sub(origin);
    return __tempV3.dot(normal);
  }

  /** Returns true if the input point is on the positive side of the plane w.r.t. the plane normal and origin. */
  public boolean positiveNormal(PVec3 p) {
    return signedDist(p) > 0;
  }
  //  public boolean

  @Override public void reset() {
    origin.setZero();
    normal.setZero();
  }

  /** Sets the normal of the plane. */
  public PPlane setNormal(float x, float y, float z) {
    this.normal.set(x, y, z);
    return this;
  }

  /** Sets the origin of the plane. */
  public PPlane setOrigin(float x, float y, float z) {
    this.origin.set(x, y, z);
    return this;
  }

  @Override protected PPool<PPlane> staticPool() {
    return getStaticPool();
  }

  @Override public PPlane set(PPlane other) {
    origin.set(other.origin);
    normal.set(other.normal);
    return this;
  }
}
