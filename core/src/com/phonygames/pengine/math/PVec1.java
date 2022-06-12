package com.phonygames.pengine.math;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PPool;

import lombok.Getter;

public class PVec1 extends PVec {
  public static class BufferList extends PPool.BufferListTemplate<PVec1> {
    private BufferList() {
      super(getStaticListPool(), getStaticPool());
    }

    public PVec1 obtain(PVec1 copyOf) {
      return PVec1.obtain(copyOf);
    }

    public static PVec1 obtain(float x) {
      return PVec1.obtain().set(x);
    }
  }

  @Getter(lazy = true)
  private static final PPool<PPool.BufferListTemplate<PVec1>> staticListPool =
      new PPool<PPool.BufferListTemplate<PVec1>>() {
        @Override
        public BufferListTemplate<PVec1> newObject() {
          return new BufferList();
        }
      };

  private boolean staticPoolHasFree = false;
  @Getter(lazy = true)
  private static final PPool<PVec1> staticPool = new PPool<PVec1>() {
    @Override
    public PVec1 newObject() {
      return new PVec1();
    }
  };

  public static BufferList obtainList() {
    return (BufferList) getStaticListPool().obtain();
  }

  public static PVec1 obtain() {
    PVec1 v = getStaticPool().obtain();
    v.staticPoolHasFree = false;
    return v;
  }

  public static PVec1 obtain(PVec1 copyOf) {
    return obtain().set(copyOf);
  }

  public static PVec1 obtain(float x) {
    return obtain().set(x);
  }

  public void free() {
    PAssert.isFalse(staticPoolHasFree, "Free() called but the vec3 was already free");
    getStaticPool().free(this);
    staticPoolHasFree = true;
  }

  private PVec1() { }

  public static final PVec1 IDT = new PVec1();

  // End static.

  private float x;

  public float x() {
    return x;
  }

  public PVec1 x(float x) {
    this.x = x;
    return this;
  }

  public float u() {
    return x;
  }

  public PVec1 u(float u) {
    this.x = u;
    return this;
  }

  @Override
  public int numComponents() {
    return 1;
  }

  @Override
  public float len2() {
    return x * x;
  }

  /**
   * Adds other to caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec1 add(PVec1 other) {
    x += other.x;
    return this;
  }

  /**
   * Subtracts other from caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec1 sub(PVec1 other) {
    x -= other.x;
    return this;
  }

  /**
   * Multiplies other with caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec1 mul(PVec1 other) {
    x *= other.x;
    return this;
  }

  /**
   * Adds scale * other to caller into caller.
   *
   * @param other
   * @param scale
   * @return caller for chaining
   */
  public PVec1 add(PVec1 other, float scale) {
    x += scale * other.x;
    return this;
  }

  /**
   * Performs a dot product.
   *
   * @param other
   * @return caller (dot) other
   */
  public float dot(PVec1 other) {
    return x * other.x;
  }

  @Override
  public void reset() {
    this.x = 0;
  }

  public PVec1 set(float x) {
    this.x = x;
    return this;
  }

  public PVec1 set(PVec1 other) {
    this.x = other.x;
    return this;
  }

  public PVec1 cpy() {
    return new PVec1().set(this);
  }
}
