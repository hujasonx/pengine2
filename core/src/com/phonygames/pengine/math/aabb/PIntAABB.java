package com.phonygames.pengine.math.aabb;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PRay;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PIntAABB implements PPool.Poolable {
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int x0, y0, z0, x1, y1, z1;

  @Getter(value = AccessLevel.PUBLIC)
  private static final PPool<PIntAABB> staticPool = new PPool<PIntAABB>() {
    @Override protected PIntAABB newObject() {
      return new PIntAABB();
    }
  };

  public boolean intersects(PIntAABB other) {
    return (x0 <= other.x1 && x1 >= other.x0) && (y0 <= other.y1 && y1 >= other.y0) &&
           (z0 <= other.z1 && z1 >= other.z0);
  }

  public boolean intersects(PRay ray) {
    float intersectLength = ray.intersectLength(this);
    return intersectLength >= 0;
  }

  @Override public void reset() {
    x0 = 0;
    x1 = 0;
    y0 = 0;
    y1 = 0;
    z0 = 0;
    z1 = 0;
  }

  /**
   * Returns true if this aabb contains the other. Aligned edges are considered contained.
   */
  public boolean fullyContains(PIntAABB other) {
    if (!contains(other.x0,other.y0, other.z0)) { return false; }
    if (!contains(other.x1,other.y1, other.z1)) { return false; }
    return true;
  }

  public PIntAABB set(int x0, int y0, int z0, int x1, int y1, int z1) {
    this.x0 = Math.min(x0, x1);
    this.x1 = Math.max(x0, x1);
    this.y0 = Math.min(y0, y1);
    this.y1 = Math.max(y0, y1);
    this.z0 = Math.min(z0, z1);
    this.z1 = Math.max(z0, z1);
    return this;
  }

  public boolean contains(int x, int y, int z) {
    return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
  }
}
