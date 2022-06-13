package com.phonygames.pengine.math;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;

public class PVec4 extends PVec<PVec4> {
  @Getter(value = AccessLevel.PROTECTED, lazy = true)
  private static final PPool<PVec4> staticPool = new PPool<PVec4>() {
    @Override protected PVec4 newObject() {
      return new PVec4();
    }
  };
  private float x, y, z, w;

  private PVec4() { }

  @Override public boolean isOnLine(PVec4 other) {
    boolean desiredRatioSet = false;
    float desiredRatioThisOverOther = 0;
    if (x == 0 && other.x == 0) {
    } else if (x == 0 && other.x != 0) {
      return false;
    } else {
      desiredRatioSet = true;
      desiredRatioThisOverOther = x / other.x;
    }
    if (y == 0 && other.y == 0) {
    } else if (y == 0 && other.y != 0) {
      return false;
    } else {
      if (desiredRatioSet) {
        if (!PNumberUtils.epsilonEquals(y / other.y, desiredRatioThisOverOther)) {
          return false;
        }
      }
    }
    if (z == 0 && other.z == 0) {
    } else if (z == 0 && other.z != 0) {
      return false;
    } else {
      if (desiredRatioSet) {
        if (!PNumberUtils.epsilonEquals(z / other.z, desiredRatioThisOverOther)) {
          return false;
        }
      }
    }
    if (w == 0 && other.w == 0) {
    } else if (w == 0 && other.w != 0) {
      return false;
    } else {
      if (desiredRatioSet) {
        if (!PNumberUtils.epsilonEquals(w / other.w, desiredRatioThisOverOther)) {
          return false;
        }
      }
    }
    return true;
  }

  public static PVec4 obtain() {
    return getStaticPool().obtain();
  }

  @Override public PVec4 add(PVec4 other) {
    x += other.x;
    y += other.y;
    z += other.z;
    w += other.w;
    return this;
  }

  @Override public float dot(PVec4 other) {
    return x * other.x + y * other.y + z * other.z + w * other.w;
  }

  @Override public boolean equalsT(PVec4 vec4) {
    return PNumberUtils.epsilonEquals(x, vec4.x) && PNumberUtils.epsilonEquals(y, vec4.y) &&
           PNumberUtils.epsilonEquals(z, vec4.z) && PNumberUtils.epsilonEquals(w, vec4.w);
  }

  public PVec4 fromColor(Color color) {
    this.x = color.r;
    this.y = color.g;
    this.z = color.b;
    this.w = color.a;
    return this;
  }

  @Override public float len2() {
    return x * x + y * y + z * z + w * w;
  }

  @Override public PVec4 mul(PVec4 other) {
    x *= other.x;
    y *= other.y;
    z *= other.z;
    w *= other.w;
    return this;
  }

  public PVec4 mul(PMat4 mat4) {
    float[] l_mat = mat4.values();
    return this.set(x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + w * l_mat[Matrix4.M03],
                    x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + w * l_mat[Matrix4.M13],
                    x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + w * l_mat[Matrix4.M23],
                    x * l_mat[Matrix4.M30] + y * l_mat[Matrix4.M31] + z * l_mat[Matrix4.M32] + w * l_mat[Matrix4.M33]);
  }

  @Override public PVec4 add(PVec4 other, float scl) {
    x += other.x * scl;
    y += other.y * scl;
    z += other.z * scl;
    w += other.w * scl;
    return this;
  }

  @Override public PVec4 scl(float scl) {
    x *= scl;
    y *= scl;
    z *= scl;
    w *= scl;
    return this;
  }

  public PVec4 set(float x, float y, float z, float w) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
    return this;
  }

  @Override public PVec4 set(PVec4 other) {
    this.x = other.x;
    this.y = other.y;
    this.z = other.z;
    this.w = other.w;
    return this;
  }

  @Override public PVec4 setZero() {
    return set(0, 0, 0, 0);
  }

  @Override public PPool<PVec4> staticPool() {
    return getStaticPool();
  }

  /**
   * Subtracts other from caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec4 sub(PVec4 other) {
    x -= other.x;
    y -= other.y;
    z -= other.z;
    w -= other.w;
    return this;
  }

  public float w() {
    return w;
  }

  public PVec4 w(float w) {
    this.w = w;
    return this;
  }

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
}
