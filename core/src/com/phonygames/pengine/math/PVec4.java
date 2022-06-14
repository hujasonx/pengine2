package com.phonygames.pengine.math;

import android.support.annotation.NonNull;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PVec4 extends PVec<PVec4> {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PVec4> staticPool = new PPool<PVec4>() {
    @Override protected PVec4 newObject() {
      return new PVec4();
    }
  };
  public static PVec4 W = new PVec4().set(0, 0, 0, 1);
  @Getter(value = AccessLevel.PRIVATE)
  @Accessors(fluent = true)
  public static PVec4 X = new PVec4().set(1, 0, 0, 0);
  public static PVec4 Y = new PVec4().set(0, 1, 0, 0);
  public static PVec4 Z = new PVec4().set(0, 0, 1, 0);
  public static PVec4 ZERO = new PVec4().set(0, 0, 0, 0);
  private final Quaternion backingQuaterion = new Quaternion().set(0, 0, 0, 0);

  private PVec4() {}

  public static PVec4 obtain() {
    return getStaticPool().obtain();
  }

  @Override public PVec4 add(@NonNull PVec4 other) {
    backingQuaterion.add(other.backingQuaterion);
    return this;
  }

  @Override public PVec4 add(@NonNull PVec4 other, float scl) {
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

  @Override public PVec4 mul(@NonNull PVec4 other) {
    backingQuaterion.x *= other.backingQuaterion.x;
    backingQuaterion.y *= other.backingQuaterion.y;
    backingQuaterion.z *= other.backingQuaterion.z;
    backingQuaterion.w *= other.backingQuaterion.w;
    return this;
  }

  @Override public PVec4 setZero() {
    return set(0, 0, 0, 0);
  }

  @Override public PVec4 scl(float scl) {
    backingQuaterion.x *= scl;
    backingQuaterion.y *= scl;
    backingQuaterion.z *= scl;
    backingQuaterion.w *= scl;
    return this;
  }

  /**
   * Subtracts other from caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec4 sub(@NonNull PVec4 other) {
    backingQuaterion.x -= other.backingQuaterion.x;
    backingQuaterion.y -= other.backingQuaterion.y;
    backingQuaterion.z -= other.backingQuaterion.z;
    backingQuaterion.w -= other.backingQuaterion.w;
    return this;
  }

  public PVec4 set(float x, float y, float z, float w) {
    backingQuaterion.set(x, y, z, w);
    return this;
  }

  @Override public boolean equalsT(@NonNull PVec4 vec4) {
    return backingQuaterion.equals(vec4.backingQuaterion);
  }

  public PVec4 fromColor(@NonNull Color color) {
    this.backingQuaterion.set(color.r, color.g, color.b, color.a);
    return this;
  }

  @Override public PVec4 lerp(PVec4 other, float mix) {
    backingQuaterion.x += (other.backingQuaterion.x - backingQuaterion.x) * mix;
    backingQuaterion.y += (other.backingQuaterion.y - backingQuaterion.y) * mix;
    backingQuaterion.z += (other.backingQuaterion.z - backingQuaterion.z) * mix;
    backingQuaterion.w += (other.backingQuaterion.w - backingQuaterion.w) * mix;
    return this;
  }

  public PVec4 slerp(PVec4 other, float mix) {
    backingQuaterion.slerp(other.backingQuaterion, mix);
    return this;
  }

  public PVec4 mul(@NonNull PMat4 mat4) {
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

  public PVec4 mulQuaternion(@NonNull PVec4 other) {
    backingQuaterion.mul(other.backingQuaterion);
    return this;
  }

  public PVec4 set(@NonNull Quaternion quaternion) {
    backingQuaterion.set(quaternion);
    return this;
  }

  public PVec4 setIdentityQuaternion() {
    backingQuaterion.idt();
    return this;
  }

  @Override public PPool<PVec4> staticPool() {
    return getStaticPool();
  }

  @Override public PVec4 set(@NonNull PVec4 other) {
    this.backingQuaterion.set(other.backingQuaterion);
    return this;
  }

  public float w() {
    return backingQuaterion.w;
  }

  public PVec4 w(float w) {
    this.backingQuaterion.w = w;
    return this;
  }

  public float x() {
    return backingQuaterion.x;
  }

  public PVec4 x(float x) {
    this.backingQuaterion.x = x;
    return this;
  }

  public float y() {
    return backingQuaterion.y;
  }

  public PVec4 y(float y) {
    this.backingQuaterion.y = y;
    return this;
  }

  public float z() {
    return backingQuaterion.z;
  }

  public PVec4 z(float z) {
    this.backingQuaterion.z = z;
    return this;
  }
}
