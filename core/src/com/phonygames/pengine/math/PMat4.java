package com.phonygames.pengine.math;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PBasic;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringMap;
import com.phonygames.pengine.util.PStringUtils;
import com.phonygames.pengine.util.PWriteLockable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class PMat4 extends PBasic<PMat4> implements PPool.Poolable, PLerpable<PMat4>, PWriteLockable<PMat4> {
  // #pragma mark - PWriteLockable
  @Getter
  @Setter
  private boolean lockWriting = false;
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end
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
      return new PStringMap<PMat4>(PMat4.getStaticPool());
    }
  };
  @Getter
  private final Matrix4 backingMatrix4 = new Matrix4();

  private PMat4() {
    backingMatrix4.idt();
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
    this.forWriting().backingMatrix4.set(values);
    return this;
  }

  @Override public int compareTo(PMat4 mat4) {
    return 0;
  }

  @Override public boolean equalsT(PMat4 mat4) {
    PAssert.failNotImplemented("equalsT");
    return false;
  }

  public PVec3 getXAxis(PVec3 out) {
    return getTransformedDirection(out.set(1, 0, 0));
  }

  public PVec3 getTransformedDirection(PVec3 inout) {
    inout.mul(this, 1);
    float endX = inout.x();
    float endY = inout.y();
    float endZ = inout.z();
    return inout.setZero().mul(this, 1).sub(endX, endY, endZ).scl(-1).nor();
  }

  public PVec3 getYAxis(PVec3 out) {
    return getTransformedDirection(out.set(0, 1, 0));
  }

  public PVec3 getZAxis(PVec3 out) {
    return getTransformedDirection(out.set(0, 0, 1));
  }

  public PMat4 inv() {
    this.forWriting().backingMatrix4.inv();
    return this;
  }

  public PMat4 invTra() {
    this.forWriting().backingMatrix4.inv().tra();
    return this;
  }

  @Override public PMat4 lerp(PMat4 other, float mix) {
    this.forWriting().backingMatrix4.lerp(other.backingMatrix4, mix);
    return this;
  }

  @Override public void lockWriting() {this.lockWriting = true;}

  public PMat4 mul(PMat4 mat4, float a) {
    mat4.mul(this, a);
    return mat4;
  }

  public PVec4 mul(PVec4 vec4) {
    vec4.mul(this);
    return vec4;
  }

  public PMat4 mul(PMat4 other) {
    this.forWriting().backingMatrix4.mul(other.backingMatrix4);
    return this;
  }

  public PMat4 mulAdd(PMat4 other, float weight) {
    for (int a = 0; a < 16; a++) {
      backingMatrix4.val[a] += other.backingMatrix4.val[a] * weight;
    }
    return this;
  }

  public PMat4 mulLeft(PMat4 other) {
    this.forWriting().backingMatrix4.mulLeft(other.backingMatrix4);
    return this;
  }

  @Override public void reset() {
    backingMatrix4.idt();
  }

  public PMat4 rotate(PVec4 rotation) {
    backingMatrix4.rotate(rotation.backingQuaterion());
    return this;
  }

  public PMat4 rotate(PVec3 axis, float rad) {
    return this.rotate(axis.x(), axis.y(), axis.z(), rad);
  }

  public PMat4 rotate(float axisX, float axisY, float axisZ, float rad) {
    synchronized (IDT) {
      backingMatrix4.rotateRad(axisX, axisY, axisZ, rad);
    }
    return this;
  }

  public PMat4 scl(float scl) {
    for (int a = 0; a < 16; a++) {
      backingMatrix4.val[a] *= scl;
    }
    return this;
  }

  public PMat4 scl(float x, float y, float z) {
    backingMatrix4.scale(x, y, z);
    return this;
  }

  public PMat4 set(Vector3 t, Quaternion r, Vector3 s) {
    return set(t.x, t.y, t.z, r.x, r.y, r.z, r.w, s.x, s.y, s.z);
  }

  public PMat4 set(float tx, float ty, float tz, float rx, float ry, float rz, float rw, float sx, float sy, float sz) {
    this.forWriting().backingMatrix4.set(tx, ty, tz, rx, ry, rz, rw, sx, sy, sz);
    return this;
  }

  public PMat4 set(PVec3 xAxis, PVec3 yAxis, PVec3 zAxis, PVec3 pos) {
    float[] val = backingMatrix4.val;
    val[Matrix4.M00] = xAxis.x();
    val[Matrix4.M01] = yAxis.x();
    val[Matrix4.M02] = zAxis.x();
    val[Matrix4.M03] = pos.x();
    val[Matrix4.M10] = xAxis.y();
    val[Matrix4.M11] = yAxis.y();
    val[Matrix4.M12] = zAxis.y();
    val[Matrix4.M13] = pos.y();
    val[Matrix4.M20] = xAxis.z();
    val[Matrix4.M21] = yAxis.z();
    val[Matrix4.M22] = zAxis.z();
    val[Matrix4.M23] = pos.z();
    val[Matrix4.M30] = 0f;
    val[Matrix4.M31] = 0f;
    val[Matrix4.M32] = 0f;
    val[Matrix4.M33] = 1f;
    return this;
  }

  public PMat4 setToRotation(float axisX, float axisY, float axisZ, float angleRad) {
    this.forWriting().backingMatrix4.setToRotationRad(axisX, axisY, axisZ, angleRad);
    return this;
  }

  public PMat4 setToScl(float x, float y, float z) {
    this.forWriting().backingMatrix4.setToScaling(x, y, z);
    return this;
  }

  public PMat4 setToScl(float scale) {
    this.forWriting().backingMatrix4.setToScaling(scale, scale, scale);
    return this;
  }

  public PMat4 setToTranslation(PVec3 vec3) {
    return idt().translate(vec3);
  }

  public PMat4 translate(PVec3 vec3) {
    backingMatrix4.translate(vec3.backingVec3());
    return this;
  }

  public PMat4 idt() {
    set(IDT);
    return this;
  }

  public PMat4 setToTranslation(float x, float y, float z) {
    return idt().translate(x, y, z);
  }

  public PMat4 translate(float x, float y, float z) {
    backingMatrix4.translate(x, y, z);
    return this;
  }

  public PMat4 setZero() {
    return set(ZERO);
  }

  @Override public PPool<PMat4> staticPool() {
    return getStaticPool();
  }

  @Override public PMat4 set(PMat4 other) {
    this.forWriting().backingMatrix4.set(other.backingMatrix4);
    return this;
  }

  @Override public String toString() {
    return "[PMat4]\n\t[" +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[0]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[4]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[8]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[12]), 7) +
           "]\n\t[" +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[1]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[5]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[9]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[13]), 7) +
           "]\n\t[" +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[2]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[6]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[10]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[14]), 7) +
           "]\n\t[" +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[3]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[7]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[11]), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingMatrix4.val[15]), 7) + "]";
  }

  public PMat4 tra() {
    this.forWriting().backingMatrix4.tra();
    return this;
  }

  /**
   * @param rotation - Must be normalized.
   * @return
   */
  public PMat4 updateRotation(PVec4 rotation) {
    PVec3 tempTra = getTranslation(PVec3.obtain());
    PVec3 tempScl = getScale(PVec3.obtain());
    set(tempTra, rotation, tempScl);
    tempScl.free();
    tempTra.free();
    return this;
  }

  public PVec3 getTranslation(PVec3 out) {
    backingMatrix4.getTranslation(out.backingVec3());
    return out;
  }

  public PVec3 getScale(PVec3 out) {
    backingMatrix4.getScale(out.backingVec3());
    return out;
  }

  public PMat4 set(PVec3 t, PVec4 r, PVec3 s) {
    return set(t.x(), t.y(), t.z(), r.x(), r.y(), r.z(), r.w(), s.x(), s.y(), s.z());
  }

  public PMat4 updateTranslation(PVec3 translation) {
    PVec3 tempScl = getScale(PVec3.obtain());
    PVec4 tempRot = getRotation(PVec4.obtain()).nor();
    set(translation, tempRot, tempScl);
    tempRot.free();
    tempScl.free();
    return this;
  }

  public PVec4 getRotation(PVec4 out) {
    backingMatrix4.getRotation(out.backingQuaterion());
    return out;
  }

  public float[] values() {
    return backingMatrix4.val;
  }
}
