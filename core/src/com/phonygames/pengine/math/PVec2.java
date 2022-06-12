package com.phonygames.pengine.math;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PPool;

import lombok.Getter;

public class PVec2 extends PVec {
  public static class BufferList extends PPool.BufferListTemplate<PVec2> {
    private BufferList() {
      super(getStaticListPool(), getStaticPool());
    }

    public PVec2 obtain(PVec2 copyOf) {
      return PVec2.obtain(copyOf);
    }

    public static PVec2 obtain(float x, float y) {
      return PVec2.obtain().set(x, y);
    }
  }

  @Getter(lazy = true)
  private static final PPool<PPool.BufferListTemplate<PVec2>> staticListPool =
      new PPool<PPool.BufferListTemplate<PVec2>>() {
        @Override
        public BufferListTemplate<PVec2> newObject() {
          return new BufferList();
        }
      };

  private boolean staticPoolHasFree = false;
  @Getter(lazy = true)
  private static final PPool<PVec2> staticPool = new PPool<PVec2>() {
    @Override
    public PVec2 newObject() {
      return new PVec2();
    }
  };

  public static BufferList obtainList() {
    return (BufferList) getStaticListPool().obtain();
  }

  public static PVec2 obtain() {
    PVec2 v = getStaticPool().obtain();
    v.staticPoolHasFree = false;
    return v;
  }

  public static PVec2 obtain(PVec2 copyOf) {
    return obtain().set(copyOf);
  }

  public static PVec2 obtain(float x, float y) {
    return obtain().set(x, y);
  }

  public void free() {
    PAssert.isFalse(staticPoolHasFree, "Free() called but the vec3 was already free");
    getStaticPool().free(this);
    staticPoolHasFree = true;
  }

  private PVec2() { }

  public static final PVec2 IDT = new PVec2();

  // End static.

  private float x, y;

  public float x() {
    return x;
  }

  public PVec2 x(float x) {
    this.x = x;
    return this;
  }

  public float y() {
    return y;
  }

  public PVec2 y(float y) {
    this.y = y;
    return this;
  }

  public float u() {
    return x;
  }

  public PVec2 u(float u) {
    this.x = u;
    return this;
  }

  public float v() {
    return y;
  }

  public PVec2 v(float v) {
    this.y = v;
    return this;
  }

  @Override
  public int numComponents() {
    return 2;
  }

  @Override
  public float len2() {
    return x * x + y * y;
  }

  /**
   * Adds other to caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec2 add(PVec2 other) {
    x += other.x;
    y += other.y;
    return this;
  }

  /**
   * Subtracts other from caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec2 sub(PVec2 other) {
    x -= other.x;
    y -= other.y;
    return this;
  }

  /**
   * Multiplies other with caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec2 mul(PVec2 other) {
    x *= other.x;
    y *= other.y;
    return this;
  }

  /**
   * Adds scale * other to caller into caller.
   *
   * @param other
   * @param scale
   * @return caller for chaining
   */
  public PVec2 add(PVec2 other, float scale) {
    x += scale * other.x;
    y += scale * other.y;
    return this;
  }

  /**
   * Performs a dot product.
   *
   * @param other
   * @return caller (dot) other
   */
  public float dot(PVec2 other) {
    return x * other.x + y * other.y;
  }

  @Override
  public void reset() {
    this.x = 0;
    this.y = 0;
  }

  public PVec2 set(float x, float y) {
    this.x = x;
    this.y = y;
    return this;
  }

  public PVec2 set(PVec2 other) {
    this.x = other.x;
    this.y = other.y;
    return this;
  }

  public PVec2 cpy() {
    return new PVec2().set(this);
  }
}
