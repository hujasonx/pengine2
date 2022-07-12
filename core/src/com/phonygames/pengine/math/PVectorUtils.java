package com.phonygames.pengine.math;

import com.badlogic.gdx.math.MathUtils;
import com.phonygames.pengine.util.PPool;

public class PVectorUtils {
  public static float angleToRotateOntoPlane(PVec3 vec, PVec3 axis, PVec3 planeNormal) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      float a = vec.dot(planeNormal);
      float b = (pool.vec3().set(axis).crs(vec)).dot(planeNormal);
      float c = (vec.dot(axis) * planeNormal.dot(vec));
      float xPlus =
          (c * (c - a) + PNumberUtils.sqrt(b * b * ((a - c) * (a - c) + b * b - c * c)) / ((a - c) * (a - c) + b * b));
      float yPlus = ((c - a) * xPlus - c) / b;
      return MathUtils.atan2(yPlus, xPlus);
    }
  }

  public static PVec3 closestPointOnCircle(PVec3 out, PVec3 in, PVec3 normal, PVec3 center, float radius) {
    projectOntoPlane(out.set(in), normal, center);
    out.sub(center).nor().scl(radius);
    return out.add(center);
  }

  public static PVec3 projectOntoPlane(PVec3 inout, PVec3 normal, PVec3 planePoint) {
    return inout.add(normal,
                     -normal.dot(inout.x() - planePoint.x(), inout.y() - planePoint.y(), inout.z() - planePoint.z()));
  }

  // Returns null if no intersection.
  // Adapted from https://stackoverflow.com/questions/5666222/3d-line-plane-intersection;
  public static PVec3 lineSegmentIntersectWithPlane(PVec3 out, PVec3 p0, PVec3 p1, PVec3 planeNormal,
                                                    PVec3 planePosition) {
    out.set(p1).sub(p0); // Set out to the direction for now.
    if (planeNormal.dot(out) == 0) {
      return null;
    }
    float t = (planeNormal.dot(planePosition) - planeNormal.dot(p0)) / planeNormal.dot(out.nor());
    return out.scl(t).add(p0);
  }
}
