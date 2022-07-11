package com.phonygames.pengine.math;

public class PVectorUtils {
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
