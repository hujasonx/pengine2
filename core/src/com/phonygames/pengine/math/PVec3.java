package com.phonygames.pengine.math;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.phonygames.pengine.graphics.shader.PShader;
import com.phonygames.pengine.util.PList;
import com.phonygames.pengine.util.PStringUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public class PVec3 extends PVec {
  public static PVec3 ZERO = new PVec3().set(0, 0, 0);
  public static PVec3 X = new PVec3().set(1, 0, 0);
  public static PVec3 Y = new PVec3().set(0, 1, 0);
  public static PVec3 Z = new PVec3().set(0, 0, 1);

  @Override
  public String toString() {
    return "<" + PStringUtils.prependSpacesToLength(PStringUtils.roundNearestHundredth(backingVec3.x), 6) +
        ", " + PStringUtils.prependSpacesToLength(PStringUtils.roundNearestHundredth(backingVec3.y), 6) +
        ", " + PStringUtils.prependSpacesToLength(PStringUtils.roundNearestHundredth(backingVec3.z), 6) + ">";
  }

  @Getter
  private final Vector3 backingVec3 = new Vector3();
  private static Pool<TempBuffer<PVec3>> tempBuffers = new Pool<TempBuffer<PVec3>>() {
    @Override
    protected TempBuffer<PVec3> newObject() {
      return new TempBuffer<PVec3>() {
        @Override
        public PVec3 genNewObject() {
          return new PVec3();
        }

        @Override
        protected void finishInternal() {
          tempBuffers.free(this);
        }
      };
    }
  };

  static TempBuffer<PVec3> staticTempBuffer = tempBuffers.obtain();

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
   * Multiplies scale with caller into caller.
   *
   * @param scale
   * @return caller for chaining
   */
  public PVec3 scl(float scale) {
    backingVec3.x *= scale;
    backingVec3.y *= scale;
    backingVec3.z *= scale;
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

  public PVec3 set(Vector3 other) {
    backingVec3.set(other);
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

  // New stuff.

  public Vector3 putInto(Vector3 in) {
    in.set(backingVec3);
    return in;
  }

  public PVec3 crs(PVec3 other) {
    backingVec3.crs(other.backingVec3);
    return this;
  }

  public PVec3 nor() {
    backingVec3.nor();
    return this;
  }

  public float dst(PVec3 other) {
    return backingVec3.dst(other.backingVec3);
  }

  public float dst2(PVec3 other) {
    return backingVec3.dst2(other.backingVec3);
  }

  public static PVec3 temp() {
    return staticTempBuffer.pool.obtain();
  }

  public void freeTemp() {
    staticTempBuffer.pool.free(this);
  }

  public static TempBuffer<PVec3> tempBuffer() {
    return tempBuffers.obtain();
  }

  public boolean isCollinear(PVec3 other) {
    return backingVec3.isCollinear(other.backingVec3);
  }


  public PVec3 sphericalYUpZForward(float theta, float phi, float radius) {
    float sinPhi = MathUtils.sin(phi);
    // Use negative theta because we want the angle to rotate correctly when viewed from +y towards -y.
    backingVec3.set(radius * sinPhi * MathUtils.sin(-theta), radius * MathUtils.cos(phi), radius * sinPhi * MathUtils.cos(-theta));
    return this;
  }

  public static boolean isCollinear(PVec3 v0, PVec3 v1, PVec3 v2) {
    boolean retVal = false;
    PVec3 t0 = PVec3.temp().set(v1).sub(v0);
    PVec3 t1 = PVec3.temp().set(v2).sub(v0);
    retVal = t0.isCollinear(t1);
    t0.freeTemp();
    t1.freeTemp();
    return retVal;
  }
}
