package com.phonygames.pengine.math;

import com.phonygames.pengine.math.aabb.PIntAABB;
import com.phonygames.pengine.util.PBasic;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PRay extends PBasic<PRay> {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PRay> staticPool = new PPool<PRay>() {
    @Override protected PRay newObject() {
      return new PRay();
    }
  };
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  @Accessors(fluent = true)
  private final PVec3 origin = PVec3.obtain(), dir = PVec3.obtain();

  public static PRay obtain() {
    return getStaticPool().obtain();
  }

  @Override public int compareTo(PRay pRay) {
    int ct0 = origin().compareTo(pRay.origin());
    if (ct0 != 0) {
      return ct0;
    }
    return dir().compareTo(pRay.dir());
  }

  @Override public boolean equalsT(PRay pRay) {
    return pRay.origin().equalsT(origin()) && pRay.dir().equalsT(dir());
  }

  /**
   * @param aabb
   * @return -1 if no intersection.
   */
  public float intersectLength(PIntAABB aabb) {
    PVec3 dirFrac = PVec3.obtain();
    // r.dir is unit direction vector of ray
    dirFrac.x(1.0f / dir().x());
    dirFrac.y(1.0f / dir().y());
    dirFrac.z(1.0f / dir().z());
    // lb is the corner of AABB with minimal coordinates - left bottom, rt is maximal corner
    // r.org is origin of ray
    float t1 = (aabb.x0() - origin().x()) * dirFrac.x();
    float t2 = (aabb.x1() - origin().x()) * dirFrac.x();
    float t3 = (aabb.y0() - origin().y()) * dirFrac.y();
    float t4 = (aabb.y1() - origin().y()) * dirFrac.y();
    float t5 = (aabb.z0() - origin().z()) * dirFrac.z();
    float t6 = (aabb.z1() - origin().z()) * dirFrac.z();
    dirFrac.free();
    float tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
    float tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
    // if tmax < 0, ray (line) is intersecting AABB, but the whole AABB is behind us
    if (tmax < 0) {
      return -1;
    }
    // if tmin > tmax, ray doesn't intersect AABB
    if (tmin > tmax) {
      return -1;
    }
    return tmin;
  }

  @Override public void reset() {
    origin().setZero();
    dir().setZero();
  }

  @Override protected PPool<PRay> staticPool() {
    return getStaticPool();
  }

  @Override public PRay set(PRay other) {
    origin().set(other.origin());
    dir().set(other.dir());
    return this;
  }
}
