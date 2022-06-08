package com.phonygames.pengine.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public class PVec4 extends PVec {
  private float x, y, z, w;

  public float x() {
    return x;
  }

  public PVec4 x(float x) {
    this.x = x;
    return this;
  }

  public float y() {
    return y;
  }

  public PVec4 y(float y) {
    this.y = y;
    return this;
  }

  public float z() {
    return z;
  }

  public PVec4 z(float z) {
    this.z = z;
    return this;
  }

  public float w() {
    return w;
  }

  public PVec4 w(float w) {
    this.w = w;
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
  public PVec4 add(PVec4 other) {
    x += other.x;
    y += other.y;
    z += other.z;
    w += other.w;
    return this;
  }

  /**
   * Subtracts other from caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec4 sub(PVec4 other) {
    x -= other.x;
    y -= other.y;
    z -= other.z;
    w -= other.w;
    return this;
  }

  /**
   * Multiplies other with caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec4 mul(PVec4 other) {
    x *= other.x;
    y *= other.y;
    z *= other.z;
    w *= other.w;
    return this;
  }

  /**
   * Adds scale * other to caller into caller.
   *
   * @param other
   * @param scale
   * @return caller for chaining
   */
  public PVec4 add(PVec4 other, float scale) {
    x += scale * other.x;
    y += scale * other.y;
    z += scale * other.z;
    w += scale * other.w;
    return this;
  }

  /**
   * Performs a dot product.
   *
   * @param other
   * @return caller (dot) other
   */
  public float dot(PVec4 other) {
    return x * other.x + y * other.y + z * other.z + w * other.w;
  }

  @Override
  public void reset() {
    this.x = 0;
    this.y = 0;
    this.z = 0;
    this.w = 0;
  }

  public PVec4 set(float x, float y, float z, float w) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
    return this;
  }

  public PVec4 set(PVec4 other) {
    this.x = other.x;
    this.y = other.y;
    this.z = other.z;
    this.w = other.w;
    return this;
  }

  public PVec4 mul(PMat4 mat4) {
    float[] l_mat = mat4.values();
    return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + w * l_mat[Matrix4.M03], x
        * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + w * l_mat[Matrix4.M13], x * l_mat[Matrix4.M20] + y
        * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + w * l_mat[Matrix4.M23], x * l_mat[Matrix4.M30] + y
        * l_mat[Matrix4.M31] + z * l_mat[Matrix4.M32] + w * l_mat[Matrix4.M33]);
  }

  public PVec4 cpy() {
    return new PVec4().set(this);
  }
}
