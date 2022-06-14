package com.phonygames.pengine.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.phonygames.pengine.util.PBasic;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class PMat4 extends PBasic<PMat4> implements PPool.Poolable {
  public static final PMat4 IDT = new PMat4();
  public static final PMat4 ZERO = new PMat4().set(new float[16]);
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PMat4> staticPool = new PPool<PMat4>() {
    @Override public PMat4 newObject() {
      return new PMat4();
    }
  };
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PStringMap<PMat4>> mat4StringMapsPool = new PPool<PStringMap<PMat4>>() {
    @Override protected PStringMap<PMat4> newObject() {
      return new PStringMap<>(PMat4.getStaticPool());
    }
  };
  @Getter
  private final Matrix4 backingMatrix4 = new Matrix4();
  @Getter
  @Setter
  private PPool ownerPool;

  private PMat4() {
  }

  public static PMat4 obtain(PMat4 copyOf) {
    return obtain().set(copyOf);
  }

  public static PMat4 obtain() {
    return getStaticPool().obtain();
  }

  public static PMat4 obtain(float[] val) {
    return obtain().set(val);
  }

  public PMat4 set(float[] values) {
    this.backingMatrix4.set(values);
    return this;
  }

  @Override public int compareTo(PMat4 mat4) {
    return 0;
  }

  @Override public boolean equalsT(PMat4 mat4) {
    return false;
  }

  public PVec3 getTranslation(PVec3 out) {
    backingMatrix4.getTranslation(out.getBackingVec3());
    return out;
  }

  public PMat4 inv() {
    this.backingMatrix4.inv();
    return this;
  }

  public PMat4 invTra() {
    this.backingMatrix4.inv().tra();
    return this;
  }

  public PMat4 mul(PMat4 mat4, float a) {
    mat4.mul(this, a);
    return mat4;
  }

  public PVec4 mul(PVec4 vec4) {
    vec4.mul(this);
    return vec4;
  }

  public PMat4 mul(PMat4 other) {
    this.backingMatrix4.mul(other.backingMatrix4);
    return this;
  }

  public PMat4 mulAdd(PMat4 other, float weight) {
    for (int a = 0; a < 16; a++) {
      backingMatrix4.val[a] += other.backingMatrix4.val[a] * weight;
    }
    return this;
  }

  public PMat4 mulLeft(PMat4 other) {
    this.backingMatrix4.mulLeft(other.backingMatrix4);
    return this;
  }

  @Override public void reset() {
    backingMatrix4.idt();
  }

  public PMat4 scl(float scl) {
    for (int a = 0; a < 16; a++) {
      backingMatrix4.val[a] *= scl;
    }
    return this;
  }

  public PMat4 set(PVec3 t, PVec4 r, PVec3 s) {
    return set(t.x(), t.y(), t.z(), r.x(), r.y(), r.z(), r.w(), s.x(), s.y(), s.z());
  }

  public PMat4 set(float tx, float ty, float tz, float rx, float ry, float rz, float rw, float sx, float sy, float sz) {
    this.backingMatrix4.set(tx, ty, tz, rx, ry, rz, rw, sx, sy, sz);
    return this;
  }

  public PMat4 set(Vector3 t, Quaternion r, Vector3 s) {
    return set(t.x, t.y, t.z, r.x, r.y, r.z, r.w, s.x, s.y, s.z);
  }

  public PMat4 set(PVec3 xAxis, PVec3 yAxis, PVec3 zAxis, PVec3 pos) {
    this.backingMatrix4.set(xAxis.getBackingVec3(), yAxis.getBackingVec3(), zAxis.getBackingVec3(),
                            pos.getBackingVec3());
    return this;
  }

  public PMat4 setToRotation(float axisX, float axisY, float axisZ, float rad) {
    return idt().rot(axisX, axisY, axisZ, rad);
  }

  public PMat4 rot(float axisX, float axisY, float axisZ, float rad) {
    synchronized (IDT) {
      backingMatrix4.rotateRad(axisX, axisY, axisZ, rad);
    }
    return this;
  }

  public PMat4 idt() {
    set(IDT);
    return this;
  }

  public PMat4 setToTranslation(PVec3 vec3) {
    return idt().translate(vec3);
  }

  public PMat4 translate(PVec3 vec3) {
    backingMatrix4.translate(vec3.getBackingVec3());
    return this;
  }

  public PMat4 setToTranslation(float x, float y, float z) {
    return idt().translate(x, y, z);
  }

  public PMat4 translate(float x, float y, float z) {
    backingMatrix4.translate(x, y, z);
    return this;
  }

  public PMat4 setTpScl(float x, float y, float z) {
    return idt().scl(x, y, z);
  }

  public PMat4 scl(float x, float y, float z) {
    backingMatrix4.scale(x, y, z);
    return this;
  }

  public PMat4 setZero() {
    return set(ZERO);
  }

  @Override public PPool<PMat4> staticPool() {
    return getStaticPool();
  }

  @Override public PMat4 set(PMat4 other) {
    this.backingMatrix4.set(other.backingMatrix4);
    return this;
  }

  public PMat4 tra() {
    this.backingMatrix4.tra();
    return this;
  }

  public float[] values() {
    return backingMatrix4.val;
  }
}
