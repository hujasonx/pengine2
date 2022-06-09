package com.phonygames.pengine.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;

import lombok.Getter;

public class PMat4 implements Pool.Poolable {
  public static final PMat4 IDT = new PMat4();

  @Getter
  private final Matrix4 backingMatrix4;

  public PMat4() {
    this(new Matrix4());
  }

  public PMat4(Matrix4 backingMatrix4) {
    this.backingMatrix4 = backingMatrix4;
  }

  public PMat4 set(PVec3 t, PVec4 r, PVec3 s) {
    return set(t.x(), t.y(), t.z(), r.x(), r.y(), r.z(), r.w(), s.x(), s.y(), s.z());
  }

  public PMat4 set(Vector3 t, Quaternion r, Vector3 s) {
    return set(t.x, t.y, t.z, r.x, r.y, r.z, r.w, s.x, s.y, s.z);
  }

  public PMat4 set(float tx, float ty, float tz, float rx, float ry, float rz, float rw, float sx, float sy, float sz) {
    this.backingMatrix4.set(tx, ty, tz, rx, ry, rz, rw, sx, sy, sz);
    return this;
  }

  public PVec3 mul(PVec3 vec3, float a) {
    vec3.mul(this, a);
    return vec3;
  }

  public PVec4 mul(PVec4 vec4) {
    vec4.mul(this);
    return vec4;
  }

  @Override
  public void reset() {
    backingMatrix4.idt();
  }

  public PMat4 set(PMat4 other) {
    this.backingMatrix4.set(other.backingMatrix4);
    return this;
  }

  public PMat4 mul(PMat4 other) {
    this.backingMatrix4.mul(other.backingMatrix4);
    return this;
  }

  public PMat4 mulLeft(PMat4 other) {
    this.backingMatrix4.mulLeft(other.backingMatrix4);
    return this;
  }

  public PMat4 inv() {
    this.backingMatrix4.inv();
    return this;
  }

  public PMat4 tra() {
    this.backingMatrix4.tra();
    return this;
  }

  public PMat4 set(float[] values) {
    this.backingMatrix4.set(values);
    return this;
  }

  public PMat4 set(PVec3 xAxis, PVec3 yAxis, PVec3 zAxis, PVec3 pos) {
    this.backingMatrix4.set(xAxis.getBackingVec3(), yAxis.getBackingVec3(), zAxis.getBackingVec3(), pos.getBackingVec3());
    return this;
  }

  public PMat4 invTra() {
    this.backingMatrix4.inv().tra();
    return this;
  }

  public float[] values() {
    return backingMatrix4.val;
  }

  public PMat4 cpy() {
    return new PMat4().set(this);
  }
}
