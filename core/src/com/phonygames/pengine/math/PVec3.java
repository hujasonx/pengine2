package com.phonygames.pengine.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector3;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class PVec3 extends PVec {
  @Getter
  private final Vector3 backingVec3 = new Vector3();

  public float x() {
    return backingVec3.x;
  }

  public PVec3 x(float x) {
    backingVec3.x = x;
    return this;
  }

  public float y() {
    return backingVec3.y;
  }

  public PVec3 y(float y) {
    backingVec3.y = y;
    return this;
  }

  public float z() {
    return backingVec3.z;
  }

  public PVec3 z(float z) {
    backingVec3.z = z;
    return this;
  }

  @Override
  public int numComponents() {
    return 3;
  }

  @Override
  public float len2() {
    return backingVec3.len2();
  }

  /**
   * Adds other to caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec3 add(PVec3 other) {
    backingVec3.add(other.backingVec3);
    return this;
  }

  /**
   * Subtracts other from caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec3 sub(PVec3 other) {
    backingVec3.sub(other.backingVec3);
    return this;
  }

  /**
   * Multiplies other with caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  public PVec3 mul(PVec3 other) {
    backingVec3.x *= other.backingVec3.x;
    backingVec3.y *= other.backingVec3.y;
    backingVec3.z *= other.backingVec3.z;
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
    backingVec3.mulAdd(other.backingVec3, scale);
    return this;
  }

  /**
   * Performs a dot product.
   *
   * @param other
   * @return caller (dot) other
   */
  public float dot(PVec3 other) {
    return backingVec3.dot(other.backingVec3);
  }

  @Override
  public void reset() {
    backingVec3.setZero();
  }

  public PVec3 set(float x, float y, float z) {
    backingVec3.set(x, y, z);
    return this;
  }

  public PVec3 set(PVec3 other) {
    backingVec3.set(other.backingVec3);
    return this;
  }

  public PVec3 mul(PMat4 mat4, float w) {
    float[] l_mat = mat4.values();
    float x = backingVec3.x;
    float y = backingVec3.y;
    float z = backingVec3.z;
    return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + w * l_mat[Matrix4.M03], x
        * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + w * l_mat[Matrix4.M13], x * l_mat[Matrix4.M20] + y
        * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + w * l_mat[Matrix4.M23]);
  }

  public PVec3 cpy() {
    return new PVec3().set(this);
  }
}
