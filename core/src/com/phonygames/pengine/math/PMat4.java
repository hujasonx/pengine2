package com.phonygames.pengine.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Pool;

public class PMat4 implements Pool.Poolable {
  private final Matrix4 backingMatrix4;

  public PMat4() {
    this(new Matrix4());
  }

  public PMat4(Matrix4 backingMatrix4) {
    this.backingMatrix4 = backingMatrix4;
  }

  public void mul(PVec4 vec4) {
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

  public float[] values() {
    return backingMatrix4.val;
  }

  public PMat4 cpy() {
    return new PMat4().set(this);
  }
}
