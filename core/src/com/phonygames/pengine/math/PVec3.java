package com.phonygames.pengine.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PVec3 extends PVec<PVec3> {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PVec3> staticPool = new PPool<PVec3>() {
    @Override protected PVec3 newObject() {
      return new PVec3();
    }
  };
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private static final PMat4 tempMat = PMat4.obtain();
  public static PVec3 X = new PVec3().set(1, 0, 0);
  public static PVec3 Y = new PVec3().set(0, 1, 0);
  public static PVec3 Z = new PVec3().set(0, 0, 1);
  public static PVec3 ZERO = new PVec3().set(0, 0, 0);
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private final Vector3 backingVec3 = new Vector3();

  private PVec3() {}

  /**
   * Adds other to caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec3 add(PVec3 other) {
    backingVec3.add(other.backingVec3);
    return this;
  }

  public float[] emit(float[] out) {
    PAssert.isTrue(out.length == 3);
    out[0] = backingVec3.x;
    out[1] = backingVec3.y;
    out[2] = backingVec3.z;
    return out;
  }

  /**
   * Adds scale * other to caller into caller.
   * @param other
   * @param scale
   * @return caller for chaining
   */
  @Override public PVec3 add(PVec3 other, float scale) {
    backingVec3.mulAdd(other.backingVec3, scale);
    return this;
  }

  @Override public float len2() {
    return backingVec3.len2();
  }

  @Override public boolean isOnLine(PVec3 other) {
    return backingVec3.isOnLine(other.backingVec3);
  }

  /**
   * Performs a dot product.
   * @param other
   * @return caller (dot) other
   */
  @Override public float dot(PVec3 other) {
    return backingVec3.dot(other.backingVec3);
  }

  @Override public boolean isZero(float margin) {
    if (Math.abs(x()) > margin) {
      return false;
    }
    if (Math.abs(y()) > margin) {
      return false;
    }
    if (Math.abs(z()) > margin) {
      return false;
    }
    return true;
  }

  /**
   * Multiplies other with caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec3 mul(PVec3 other) {
    backingVec3.x *= other.backingVec3.x;
    backingVec3.y *= other.backingVec3.y;
    backingVec3.z *= other.backingVec3.z;
    return this;
  }

  @Override public PVec3 nor() {
    backingVec3.nor();
    return this;
  }

  @Override public PVec3 setZero() {
    this.backingVec3.setZero();
    return this;
  }

  @Override public PVec3 roundComponents(float factor) {
    this.backingVec3.x = (Math.round(this.backingVec3.x * factor) / factor);
    this.backingVec3.y = (Math.round(this.backingVec3.y * factor) / factor);
    this.backingVec3.z = (Math.round(this.backingVec3.z * factor) / factor);
    return this;
  }

  /**
   * Multiplies scale with caller into caller.
   * @param scale
   * @return caller for chaining
   */
  @Override public PVec3 scl(float scale) {
    backingVec3.x *= scale;
    backingVec3.y *= scale;
    backingVec3.z *= scale;
    return this;
  }

  /**
   * Subtracts other from caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec3 sub(PVec3 other) {
    backingVec3.sub(other.backingVec3);
    return this;
  }

  public float x() {
    return backingVec3.x;
  }

  public float y() {
    return backingVec3.y;
  }

  public float z() {
    return backingVec3.z;
  }

  public PVec3 add(float x, float y, float z) {
    backingVec3.add(x, y, z);
    return this;
  }

  public PVec3 cpy() {
    return new PVec3().set(this);
  }

  public PVec3 crs(PVec3 other) {
    backingVec3.crs(other.backingVec3);
    return this;
  }

  public float dst(PVec3 other) {
    return backingVec3.dst(other.backingVec3);
  }

  public float dst2(PVec3 other) {
    return backingVec3.dst2(other.backingVec3);
  }

  @Override public boolean equalsT(PVec3 vec3) {
    return this.backingVec3.equals(vec3);
  }

  @Override public PVec3 lerp(PVec3 other, float mix) {
    backingVec3.x += (other.backingVec3.x - backingVec3.x) * mix;
    backingVec3.y += (other.backingVec3.y - backingVec3.y) * mix;
    backingVec3.z += (other.backingVec3.z - backingVec3.z) * mix;
    return this;
  }

  public PVec3 mul(PMat4 mat4, float w) {
    float[] l_mat = mat4.values();
    float x = backingVec3.x;
    float y = backingVec3.y;
    float z = backingVec3.z;
    return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + w * l_mat[Matrix4.M03],
                    x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + w * l_mat[Matrix4.M13],
                    x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + w * l_mat[Matrix4.M23]);
  }

  public PVec3 set(float x, float y, float z) {
    backingVec3.set(x, y, z);
    return this;
  }

  public float progressAlongLineSegment(PVec3 lineStart, PVec3 lineEnd) {
    PVec3 t0 = PVec3.obtain().set(lineEnd).sub(lineStart);
    float lineLen = t0.len();
    t0.nor();
    PVec3 t1 = PVec3.obtain().set(this).sub(lineStart);
    float dotAmount = t1.dot(t0);
    t0.free();
    t1.free();
    return dotAmount / lineLen;
  }

  public static PVec3 obtain() {
    return getStaticPool().obtain();
  }

  public Vector3 putInto(Vector3 in) {
    in.set(backingVec3);
    return in;
  }

  public PVec3 rotate(float axisX, float axisY, float axisZ, float angleRad) {
    synchronized (tempMat()) {
      backingVec3.mul(tempMat().setToRotation(axisX, axisY, axisZ, angleRad).getBackingMatrix4());
    }
    return this;
  }

  public PVec3 rotate(PVec3 axis, float angleRad) {
    synchronized (tempMat()) {
      backingVec3.mul(tempMat().setToRotation(axis.x(), axis.y(), axis.z(), angleRad).getBackingMatrix4());
    }
    return this;
  }

  public PVec3 set(Vector3 other) {
    backingVec3.set(other);
    return this;
  }

  /**
   * Sets this vector to (1, 1, 1).
   * @return self for chaining.
   */
  public PVec3 setOne() {
    backingVec3.set(1, 1, 1);
    return this;
  }

  public PVec3 setXYZ(PVec4 vec4) {
    backingVec3.set(vec4.x(), vec4.y(), vec4.z());
    return this;
  }

  public PVec3 sphericalYUpZForward(float theta, float phi, float radius) {
    float sinPhi = MathUtils.sin(phi);
    // Use negative theta because we want the angle to rotate correctly when viewed from +y towards -y.
    backingVec3.set(radius * sinPhi * MathUtils.sin(-theta), radius * MathUtils.cos(phi),
                    radius * sinPhi * MathUtils.cos(-theta));
    return this;
  }

  @Override protected PPool<PVec3> staticPool() {
    return getStaticPool();
  }

  @Override public PVec3 set(PVec3 other) {
    backingVec3.set(other.backingVec3);
    return this;
  }

  @Override public String toString() {
    return "[PVec3] <" + PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingVec3.x), 7) +
           ", " + PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingVec3.y), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingVec3.z), 7) + ">";
  }

  public PVec3 x(float x) {
    backingVec3.x = x;
    return this;
  }

  public PVec3 y(float y) {
    backingVec3.y = y;
    return this;
  }

  public PVec3 z(float z) {
    backingVec3.z = z;
    return this;
  }
}
