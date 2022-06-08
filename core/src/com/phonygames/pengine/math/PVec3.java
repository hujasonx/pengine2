package com.phonygames.pengine.math;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class PVec3 extends PVec {
  private float x, y, z;

  public float x() {
    return x;
  }

  public PVec3 x(float x) {
    this.x = x;
    return this;
  }

  public float y() {
    return y;
  }

  public PVec3 y(float y) {
    this.y = y;
    return this;
  }

  public float z() {
    return z;
  }

  public PVec3 z(float z) {
    this.z = z;
    return this;
  }

  @Override
  public int numComponents() {
    return 3;
  }

  @Override
  public float len2() {
    return x * x + y * y + z * z;
  }

  /**
   * Adds other to caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec3 add(PVec3 other) {
    x += other.x;
    y += other.y;
    z += other.z;
    return this;
  }

  /**
   * Subtracts other from caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec3 sub(PVec3 other) {
    x -= other.x;
    y -= other.y;
    z -= other.z;
    return this;
  }

  /**
   * Multiplies other with caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec3 mul(PVec3 other) {
    x *= other.x;
    y *= other.y;
    z *= other.z;
    return this;
  }

  /**
   * Adds scale * other to caller into caller.
   *
   * @param other
   * @param scale
   * @return caller for chaining
   */
  public PVec3 add(PVec3 other, float scale) {
    x += scale * other.x;
    y += scale * other.y;
    z += scale * other.z;
    return this;
  }

  /**
   * Performs a dot product.
   *
   * @param other
   * @return caller (dot) other
   */
  public float dot(PVec3 other) {
    return x * other.x + y * other.y + z * other.z;
  }

  @Override
  public void reset() {
    this.x = 0;
    this.y = 0;
    this.z = 0;
  }
}
