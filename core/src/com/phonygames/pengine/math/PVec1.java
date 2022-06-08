package com.phonygames.pengine.math;

public class PVec1 extends PVec {
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

  public PVec1 set(PVec1 other) {
    this.x = other.x;
    return this;
  }

  public PVec1 cpy() {
    return new PVec1().set(this);
  }
}
