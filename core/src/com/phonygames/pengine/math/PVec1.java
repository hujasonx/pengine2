package com.phonygames.pengine.math;

import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;

public class PVec1 extends PVec<PVec1> {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PVec1> staticPool = new PPool<PVec1>() {
    @Override protected PVec1 newObject() {
      return new PVec1();
    }
  };
  private float x;

  private PVec1() {}

  public static PVec1 obtain() {
    return getStaticPool().obtain();
  }

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

  @Override public float len2() {
    return x * x;
  }

  @Override public boolean isOnLine(PVec1 other) {
    return true;
  }

  /**
   * Performs a dot product.
   * @param other
   * @return caller (dot) other
   */
  @Override public float dot(PVec1 other) {
    return x * other.x;
  }

  @Override public boolean isZero(float margin) {
    if (Math.abs(x()) > margin) {
      return false;
    }
    return true;
  }

  @Override public float len() {
    return Math.abs(x);
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
  // End static.

  @Override public PVec1 nor() {
    if (x > 0) {x = 1;} else {x = -1;}
    return this;
  }

  @Override public PVec1 setZero() {
    x = 0;
    return this;
  }

  @Override public PVec1 roundComponents(float factor) {
    this.x = (Math.round(this.x * factor) / factor);
    return this;
  }

  @Override public PVec1 scl(float scl) {
    x *= scl;
    return this;
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

  @Override public boolean equalsT(PVec1 pVec1) {
    return x == pVec1.x;
  }

  @Override public PVec1 lerp(PVec1 other, float mix) {
    x += (other.x - x) * mix;
    return this;
  }

  public PVec1 set(float x) {
    this.x = x;
    return this;
  }

  @Override protected PPool staticPool() {
    return getStaticPool();
  }

  @Override public PVec1 set(PVec1 other) {
    this.x = other.x;
    return this;
  }

  public PVec1 x(float x) {
    this.x = x;
    return this;
  }
}
