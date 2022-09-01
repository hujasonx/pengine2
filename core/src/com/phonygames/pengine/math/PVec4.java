package com.phonygames.pengine.math;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.utils.NumberUtils;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringUtils;
import com.phonygames.pengine.util.PWriteLockable;
import com.phonygames.pengine.util.collection.PFloatList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PVec4 extends PVec<PVec4> implements PWriteLockable<PVec4> {
  // #pragma mark - PWriteLockable
  @Getter
  @Setter
  private boolean lockWriting = false;
  // #pragma end - PWriteLockable
  public static final PVec4 ONE = new PVec4().set(1, 1, 1, 1);
  public static final PVec4 REAL = new PVec4().set(0, 0, 0, 1);
  public static final PVec4 X = new PVec4().set(1, 0, 0, 0);
  public static final PVec4 Y = new PVec4().set(0, 1, 0, 0);
  public static final PVec4 Z = new PVec4().set(0, 0, 1, 0);
  public static final PVec4 ZERO = new PVec4().set(0, 0, 0, 0);
  public static final PVec4 IDT_QUATERNION = new PVec4().setIdentityQuaternion();
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PVec4> staticPool = new PPool<PVec4>() {
    @Override protected PVec4 newObject() {
      return new PVec4();
    }
  };
  public static PVec4 W = new PVec4().set(0, 0, 0, 1);
  @Getter(value = AccessLevel.PROTECTED)
  @Accessors(fluent = true)
  private final Quaternion backingQuaterion = new Quaternion().set(0, 0, 0, 0);

  private PVec4() {}

  public static PVec4 obtain() {
    return getStaticPool().obtain();
  }

  public PVec4 a(float a) {
    this.forWriting();
    backingQuaterion.w = a;
    return this;
  }

  @Override public PVec4 add(@NonNull PVec4 other) {
    this.forWriting();
    backingQuaterion.add(other.backingQuaterion);
    return this;
  }

  @Override public PVec4 add(@NonNull PVec4 other, float scl) {
    this.forWriting();
    backingQuaterion.x += other.backingQuaterion.x * scl;
    backingQuaterion.y += other.backingQuaterion.y * scl;
    backingQuaterion.z += other.backingQuaterion.z * scl;
    backingQuaterion.w += other.backingQuaterion.w * scl;
    return this;
  }

  @Override public float len2() {
    return backingQuaterion.len2();
  }

  @Override public boolean isOnLine(@NonNull PVec4 other) {
    boolean desiredRatioSet = false;
    float desiredRatioThisOverOther = 0;
    if (backingQuaterion.x == 0 && other.backingQuaterion.x == 0) {
    } else if (backingQuaterion.x == 0 && other.backingQuaterion.x != 0) {
      return false;
    } else {
      desiredRatioSet = true;
      desiredRatioThisOverOther = backingQuaterion.x / other.backingQuaterion.x;
    }
    if (backingQuaterion.y == 0 && other.backingQuaterion.y == 0) {
    } else if (backingQuaterion.y == 0 && other.backingQuaterion.y != 0) {
      return false;
    } else {
      if (desiredRatioSet) {
        if (!PNumberUtils.epsilonEquals(backingQuaterion.y / other.backingQuaterion.y, desiredRatioThisOverOther)) {
          return false;
        }
      }
    }
    if (backingQuaterion.z == 0 && other.backingQuaterion.z == 0) {
    } else if (backingQuaterion.z == 0 && other.backingQuaterion.z != 0) {
      return false;
    } else {
      if (desiredRatioSet) {
        if (!PNumberUtils.epsilonEquals(backingQuaterion.z / other.backingQuaterion.z, desiredRatioThisOverOther)) {
          return false;
        }
      }
    }
    if (backingQuaterion.w == 0 && other.backingQuaterion.w == 0) {
    } else if (backingQuaterion.w == 0 && other.backingQuaterion.w != 0) {
      return false;
    } else {
      if (desiredRatioSet) {
        if (!PNumberUtils.epsilonEquals(backingQuaterion.w / other.backingQuaterion.w, desiredRatioThisOverOther)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override public float dot(@NonNull PVec4 other) {
    return backingQuaterion.dot(other.backingQuaterion);
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
    if (Math.abs(w()) > margin) {
      return false;
    }
    return true;
  }

  @Override public PVec4 mul(@NonNull PVec4 other) {
    this.forWriting();
    backingQuaterion.x *= other.backingQuaterion.x;
    backingQuaterion.y *= other.backingQuaterion.y;
    backingQuaterion.z *= other.backingQuaterion.z;
    backingQuaterion.w *= other.backingQuaterion.w;
    return this;
  }

  @Override public PVec4 nor() {
    this.forWriting();
    backingQuaterion.nor();
    return this;
  }

  @Override public PVec4 setZero() {
    this.forWriting();
    return set(0, 0, 0, 0);
  }

  @Override public PVec4 roundComponents(float factor) {
    this.forWriting();
    this.backingQuaterion.x = (Math.round(this.backingQuaterion.x * factor) / factor);
    this.backingQuaterion.y = (Math.round(this.backingQuaterion.y * factor) / factor);
    this.backingQuaterion.z = (Math.round(this.backingQuaterion.z * factor) / factor);
    this.backingQuaterion.w = (Math.round(this.backingQuaterion.w * factor) / factor);
    return this;
  }

  @Override public PVec4 scl(float scl) {
    this.forWriting();
    backingQuaterion.x *= scl;
    backingQuaterion.y *= scl;
    backingQuaterion.z *= scl;
    backingQuaterion.w *= scl;
    return this;
  }

  /**
   * Subtracts other from caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  @Override public PVec4 sub(@NonNull PVec4 other) {
    this.forWriting();
    backingQuaterion.x -= other.backingQuaterion.x;
    backingQuaterion.y -= other.backingQuaterion.y;
    backingQuaterion.z -= other.backingQuaterion.z;
    backingQuaterion.w -= other.backingQuaterion.w;
    return this;
  }

  public PVec4 set(float x, float y, float z, float w) {
    this.forWriting();
    backingQuaterion.set(x, y, z, w);
    return this;
  }

  public float x() {
    return backingQuaterion.x;
  }

  public float y() {
    return backingQuaterion.y;
  }

  public float z() {
    return backingQuaterion.z;
  }

  public float w() {
    return backingQuaterion.w;
  }

  public PVec3 applyAsQuat(PVec3 out) {
    backingQuaterion.transform(out.backingVec3());
    return out;
  }

  public PVec4 b(float b) {
    this.forWriting();
    backingQuaterion.z = b;
    return this;
  }

  public float[] emit(float[] out) {
    PAssert.isTrue(out.length == 4);
    out[0] = backingQuaterion.x;
    out[1] = backingQuaterion.y;
    out[2] = backingQuaterion.z;
    out[3] = backingQuaterion.w;
    return out;
  }

  public float[] emit(float[] out, int offset) {
    PAssert.isTrue(out.length == 4);
    out[offset + 0] = backingQuaterion.x;
    out[offset + 1] = backingQuaterion.y;
    out[offset + 2] = backingQuaterion.z;
    out[offset + 3] = backingQuaterion.w;
    return out;
  }

  public PFloatList emit(PFloatList out, int offset) {
    PAssert.isTrue(out.size() >= offset + 4);
    out.set(offset + 0, backingQuaterion.x);
    out.set(offset + 1, backingQuaterion.y);
    out.set(offset + 2, backingQuaterion.z);
    out.set(offset + 3, backingQuaterion.w);
    return out;
  }

  @Override public boolean equalsT(@NonNull PVec4 vec4) {
    return backingQuaterion.equals(vec4.backingQuaterion);
  }

  public PVec4 fromColor(@NonNull Color color) {
    this.backingQuaterion.set(color.r, color.g, color.b, color.a);
    return this;
  }

  public PVec4 g(float g) {
    this.forWriting();
    backingQuaterion.y = g;
    return this;
  }

  public float getAxisAngle(PVec3 outAxis) {
    return backingQuaterion.getAxisAngleRad(outAxis.backingVec3());
  }

  public PVec4 invQuat() {
    this.forWriting();
    float d = this.dot(this);
    backingQuaterion.x /= -d;
    backingQuaterion.y /= -d;
    backingQuaterion.z /= -d;
    backingQuaterion.w /= d;
    return this;
  }

  @Override public PVec4 lerp(PVec4 other, float mix) {
    this.forWriting();
    backingQuaterion.x += (other.backingQuaterion.x - backingQuaterion.x) * mix;
    backingQuaterion.y += (other.backingQuaterion.y - backingQuaterion.y) * mix;
    backingQuaterion.z += (other.backingQuaterion.z - backingQuaterion.z) * mix;
    backingQuaterion.w += (other.backingQuaterion.w - backingQuaterion.w) * mix;
    return this;
  }

  public PVec4 mul(@NonNull PMat4 mat4) {
    this.forWriting();
    float[] l_mat = mat4.values();
    return this.set(backingQuaterion.x * l_mat[Matrix4.M00] + backingQuaterion.y * l_mat[Matrix4.M01] +
                    backingQuaterion.z * l_mat[Matrix4.M02] + backingQuaterion.w * l_mat[Matrix4.M03],
                    backingQuaterion.x * l_mat[Matrix4.M10] + backingQuaterion.y * l_mat[Matrix4.M11] +
                    backingQuaterion.z * l_mat[Matrix4.M12] + backingQuaterion.w * l_mat[Matrix4.M13],
                    backingQuaterion.x * l_mat[Matrix4.M20] + backingQuaterion.y * l_mat[Matrix4.M21] +
                    backingQuaterion.z * l_mat[Matrix4.M22] + backingQuaterion.w * l_mat[Matrix4.M23],
                    backingQuaterion.x * l_mat[Matrix4.M30] + backingQuaterion.y * l_mat[Matrix4.M31] +
                    backingQuaterion.z * l_mat[Matrix4.M32] + backingQuaterion.w * l_mat[Matrix4.M33]);
  }

  public PVec4 mulQuat(PVec4 other) {
    this.forWriting();
    backingQuaterion.mul(other.backingQuaterion);
    return this;
  }

  public PVec4 r(float r) {
    this.forWriting();
    backingQuaterion.x = r;
    return this;
  }

  public PVec4 set(@NonNull Quaternion quaternion) {
    this.forWriting();
    backingQuaterion.set(quaternion);
    return this;
  }

  public PVec4 setHSVA(float hue, float saturation, float value, float a) {
    this.forWriting();
    hue = hue % 1;
    int h = (int) (hue * 6);
    float f = hue * 6 - h;
    float p = value * (1 - saturation);
    float q = value * (1 - f * saturation);
    float t = value * (1 - (1 - f) * saturation);
    switch (h) {
      case 0:
        set(value, t, p, a);
        break;
      case 1:
        set(q, value, p, a);
        break;
      case 2:
        set(p, value, t, a);
        break;
      case 3:
        set(p, q, value, a);
        break;
      case 4:
        set(t, p, value, a);
        break;
      case 5:
        set(value, p, q, a);
        break;
      default:
        throw new RuntimeException(
            "Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " +
            value);
    }
    return this;
  }

  public PVec4 setIdentityQuaternion() {
    this.forWriting();
    backingQuaterion.idt();
    return this;
  }

  public PVec4 setToRotation(PVec3 axis, float rad) {
    return this.setToRotation(axis.x(), axis.y(), axis.z(), rad);
  }

  public PVec4 setToRotation(float axisX, float axisY, float axisZ, float rad) {
    this.forWriting();
    PMat4 temp = PMat4.obtain().setToRotation(axisX, axisY, axisZ, rad);
    backingQuaterion.setFromMatrix(temp.getBackingMatrix4());
    temp.free();
    return this;
  }

  /**
   * @param in [yaw, pitch, roll]
   * @return
   */
  public PVec4 setToRotationEuler(PVec3 in) {
    this.forWriting();
    backingQuaterion.setEulerAnglesRad(in.x(), in.y(), in.z());
    return this;
  }

  public PVec4 setToRotationEuler(float yaw, float pitch, float roll) {
    this.forWriting();
    backingQuaterion.setEulerAnglesRad(yaw, pitch, roll);
    return this;
  }

  public PVec4 slerp(PVec4 other, float mix) {
    this.forWriting();
    backingQuaterion.slerp(other.backingQuaterion, mix);
    return this;
  }

  @Override public PPool<PVec4> staticPool() {
    return getStaticPool();
  }

  @Override public PVec4 set(@NonNull PVec4 other) {
    this.forWriting();
    this.backingQuaterion.set(other.backingQuaterion);
    return this;
  }

  /**
   * https://stackoverflow.com/questions/3684269/component-of-a-quaternion-rotation-around-an-axis Decompose the
   * rotation on to 2 parts. 1. Twist - rotation around the "direction" vector 2. Swing - rotation around axis that is
   * perpendicular to "direction" vector The rotation can be composed back by rotation = swing * twist
   * <p>
   * has singularity in case of swing_rotation close to 180 degrees rotation. if the input quaternion is of non-unit
   * length, the outputs are non-unit as well otherwise, outputs are both unit
   *
   * @param direction
   * @param outSwing
   * @param outTwist
   * @return
   */
  public PVec4 swingTwistDecompose(@NonNull PVec3 direction, @Nullable PVec4 outSwing, @Nullable PVec4 outTwist) {
    try (PPool.PoolBuffer pool = PPool.getBuffer()) {
      PVec3 ra = pool.vec3(x(), y(), z());
      PVec3 p = pool.vec3(ra).projectVector(direction);
      if (outTwist == null) {outTwist = pool.vec4();}
      outTwist.set(p.x(), p.y(), p.z(), w()).nor();
      if (outSwing != null) {
        outSwing.set(this).mul(pool.vec4(outTwist).conjugate());
      }
    }
    return this;
  }

  public PVec4 conjugate() {
    this.forWriting();
    backingQuaterion.conjugate();
    return this;
  }

  /**
   * Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is
   * compressed from 0-255 to use only even numbers between 0-254 to avoid using float bits in the NaN range (see {@link
   * NumberUtils#intToFloatColor(int)}). Converting a color to a float and back can be lossy for alpha.
   *
   * @return the packed color as a 32-bit float
   */
  public float toFloatBits() {
    int color = ((int) (255 * a()) << 24) | ((int) (255 * b()) << 16) | ((int) (255 * g()) << 8) | ((int) (255 * r()));
    return NumberUtils.intToFloatColor(color);
  }

  public float a() {
    return backingQuaterion.w;
  }

  public float b() {
    return backingQuaterion.z;
  }

  public float g() {
    return backingQuaterion.y;
  }

  public float r() {
    return backingQuaterion.x;
  }

  /**
   * Packs the color components into a 32-bit integer with the format ABGR.
   *
   * @return the packed color as a 32-bit int.
   */
  public int toIntBits() {
    return ((int) (255 * a()) << 24) | ((int) (255 * b()) << 16) | ((int) (255 * g()) << 8) | ((int) (255 * r()));
  }

  @Override public String toString() {
    return "[PVec4] <" +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingQuaterion.x), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingQuaterion.y), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingQuaterion.z), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(backingQuaterion.w), 7) + ">";
  }

  public PVec4 w(float w) {
    this.forWriting();
    this.backingQuaterion.w = w;
    return this;
  }

  public PVec4 x(float x) {
    this.forWriting();
    this.backingQuaterion.x = x;
    return this;
  }

  public PVec4 y(float y) {
    this.forWriting();
    this.backingQuaterion.y = y;
    return this;
  }

  public PVec4 z(float z) {
    this.forWriting();
    this.backingQuaterion.z = z;
    return this;
  }
}
