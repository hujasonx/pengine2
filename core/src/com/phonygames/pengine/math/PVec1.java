package com.phonygames.pengine.math;

import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;

public class PVec1 extends PVec<PVec1> {
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  private static final PPool<PVec1> staticPool = new PPool<PVec1>() {
    @Override protected PVec1 newObject() {
      return new PVec1();
    }
  };
  private float x;

  private PVec1() { }

  @Override public boolean isOnLine(PVec1 other) {
    return true;
  }

  public static PVec1 obtain() {
    return getStaticPool().obtain();
  }
  // End static.

  /**
   * Adds other to caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec1 add(PVec1 other) {
    x += other.x;
    return this;
  }

  /**
   * Adds scale * other to caller into caller.
   * @param other
   * @param scale
   * @return caller for chaining
   */
  @Override public PVec1 add(PVec1 other, float scale) {
    x += scale * other.x;
    return this;
  }

  /**
   * Performs a dot product.
   * @param other
   * @return caller (dot) other
   */
  @Override public float dot(PVec1 other) {
    return x * other.x;
  }

  @Override public float len() {
    return Math.abs(x);
  }

  @Override public float len2() {
    return x * x;
  }

  /**
   * Multiplies other with caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec1 mul(PVec1 other) {
    x *= other.x;
    return this;
  }

  public PVec1 set(float x) {
    this.x = x;
    return this;
  }

  @Override public PVec1 set(PVec1 other) {
    this.x = other.x;
    return this;
  }

  @Override protected PPool staticPool() {
    return getStaticPool();
  }

  /**
   * Subtracts other from caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec1 sub(PVec1 other) {
    x -= other.x;
    return this;
  }

  public float x() {
    return x;
  }

  public PVec1 x(float x) {
    this.x = x;
    return this;
  }
}
