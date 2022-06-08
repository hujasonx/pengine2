package com.phonygames.pengine.math;

public class PVec2 extends PVec {
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

  public PVec2 set(PVec2 other) {
    this.x = other.x;
    this.y = other.y;
    return this;
  }

  public PVec2 cpy() {
    return new PVec2().set(this);
  }
}
