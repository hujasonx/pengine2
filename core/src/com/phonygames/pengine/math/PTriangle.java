package com.phonygames.pengine.math;

import com.phonygames.pengine.util.PBasic;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.collection.PList;

import lombok.AccessLevel;
import lombok.Getter;

/** A triangle in 3D space. */
public class PTriangle extends PBasic<PTriangle> {
  @Getter(value = AccessLevel.PUBLIC)
  private static final PPool<PTriangle> staticPool = new PPool<PTriangle>() {
    @Override protected PTriangle newObject() {
      return new PTriangle();
    }
  };
  /** Temp vector. */
  private final PVec3 __tempV3 = PVec3.obtain();
  /** The vertex positions. */
  private final PVec3 pos[] = new PVec3[]{PVec3.obtain(), PVec3.obtain(), PVec3.obtain()};

  public static PTriangle obtain() {
    return getStaticPool().obtain();
  }

  /** Sets the out vector to the cartesian coordinates given the input barycentric coordinates. */
  public PVec3 baryToCart(PVec3 out, PVec3 barycentric) {
    out.setZero().add(pos[0], barycentric.x()).add(pos[1], barycentric.y()).add(pos[2], barycentric.z());
    return out;
  }

  /**
   * Sets the out vector to the barycentric coordinates given the input cartesian coordinates.
   * https://people.cs.clemson.edu/~dhouse/courses/404/notes/barycentric.pdf
   */
  public PVec3 cartToBary(PVec3 out, PVec3 cartesian) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      // These variable names are directly taken from the above pdf.
      PVec3 vN = normal(pool.vec3(), false);
      float A = vN.len();
      PVec3 n = pool.vec3(vN).nor();
      PVec3 norForU = pool.vec3(pos[2]).sub(pos[1]).crs(__tempV3.set(cartesian).sub(pos[1]));
      PVec3 norForV = pool.vec3(pos[0]).sub(pos[2]).crs(__tempV3.set(cartesian).sub(pos[2]));
      __tempV3.set(n).scl(1 / A);
      out.x(norForU.dot(__tempV3));
      out.y(norForV.dot(__tempV3));
      out.z(1 - out.x() - out.y());
    }
    return out;
  }

  /** Calculates the normal of this triangle. */
  public PVec3 normal(PVec3 out, boolean normalized) {
    __tempV3.set(pos[2]).sub(pos[1]);
    out.set(pos[1]).sub(pos[0]);
    out.crs(__tempV3);
    if (normalized) {
      out.nor();
    }
    return out;
  }

  /** Clips this triangle with the given plane, adding the resulting triangle(s) to the given list. */
  public PList<PTriangle> clipWithPlane(PPlane plane, PList<PTriangle> tris) {
    // Figure out which side each of the points of this triangle is on w.r.t. the plane.
    float dst0 = plane.signedDist(pos[0]);
    float dst1 = plane.signedDist(pos[1]);
    float dst2 = plane.signedDist(pos[2]);
    // The signed distance should be nonnegative in order to keep it.
    boolean keep0 = dst0 >= 0;
    boolean keep1 = dst1 >= 0;
    boolean keep2 = dst2 >= 0;
    if (keep0 && keep1 && keep2) {
      // Keep the entire triangle.
      tris.genPooledAndAdd().set(pos[0], pos[1], pos[2]);
    } else if (!keep0 && !keep1 && !keep2) {
      // Don't add any triangles.
    } else {
      try (PPool.PoolBuffer pool = PPool.getBuffer()) {
        PVec3 corner02 = (keep0 ^ keep2) ? plane.lineIntersection(pool.vec3(), pos[0], pos[2]) : null;
        PVec3 corner12 = (keep1 ^ keep2) ? plane.lineIntersection(pool.vec3(), pos[1], pos[2]) : null;
        PVec3 corner01 = (keep0 ^ keep1) ? plane.lineIntersection(pool.vec3(), pos[0], pos[1]) : null;
        if (keep0) {
          if (keep1) {
            // Don't keep 2.
            tris.genPooledAndAdd().set(pos[0], pos[1], corner02);
            tris.genPooledAndAdd().set(corner02, pos[1], corner12);
          } else if (keep2) {
            // Don't keep 1.
            tris.genPooledAndAdd().set(pos[0], corner01, corner12);
            tris.genPooledAndAdd().set(pos[0], corner12, pos[2]);
          } else {
            // Don't keep either.
            tris.genPooledAndAdd().set(pos[0], corner01, corner02);
          }
        } else if (keep1) {
          // Keep 1, but don't keep 0.
          if (keep2) {
            tris.genPooledAndAdd().set(pos[1], pos[2], corner02);
            tris.genPooledAndAdd().set(pos[1], corner02, corner01);
          } else {
            // Only keep 1.
            tris.genPooledAndAdd().set(pos[1], corner12, corner01);
          }
        } else {
          // Keep 2, but not 0 or 1.
          tris.genPooledAndAdd().set(pos[2], corner02, corner12);
        }
      }
    }
    return tris;
  }

  /** Sets the triangle given the corner vertices. */
  public PTriangle set(PVec3 p0, PVec3 p1, PVec3 p2) {
    pos[0].set(p0);
    pos[1].set(p1);
    pos[2].set(p2);
    return this;
  }

  @Override public int compareTo(PTriangle pTri) {
    int ct0 = pos[0].compareTo(pTri.pos[0]);
    if (ct0 != 0) {
      return ct0;
    }
    int ct1 = pos[1].compareTo(pTri.pos[1]);
    if (ct1 != 0) {
      return ct1;
    }
    int ct2 = pos[2].compareTo(pTri.pos[2]);
    return ct2;
  }

  @Override public boolean equalsT(PTriangle pTri) {
    return pTri.pos[0].equalsT(pos[0]) && pTri.pos[1].equalsT(pos[1]) && pTri.pos[2].equalsT(pos[2]);
  }

  /** Sets the plane to contain this triangle. */
  public PPlane getPlane(PPlane out) {
    normal(out.normal(), true);
    out.origin().set(pos[0]);
    return out;
  }

  /** Returns true if the triangle is on the given plane. */
  public boolean onPlane(PPlane plane) {
    return plane.onPlane(pos[0]) && plane.onPlane(pos[1]) && plane.onPlane(pos[2]);
  }

  /** Returns the position at the given index [0, 1, 2]. */
  public PVec3 pos(int index) {
    return pos[index];
  }

  @Override public void reset() {
    pos[0].setZero();
    pos[1].setZero();
    pos[2].setZero();
  }

  @Override protected PPool<PTriangle> staticPool() {
    return getStaticPool();
  }

  @Override public PTriangle set(PTriangle other) {
    pos[0].set(other.pos[0]);
    pos[1].set(other.pos[1]);
    pos[2].set(other.pos[2]);
    return this;
  }
}
