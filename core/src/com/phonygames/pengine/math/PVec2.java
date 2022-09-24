package com.phonygames.pengine.math;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PPool;
import com.phonygames.pengine.util.PStringUtils;
import com.phonygames.pengine.util.collection.PFloatList;

import lombok.AccessLevel;
import lombok.Getter;

public class PVec2 extends PVec<PVec2> {
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PVec2> staticPool = new PPool<PVec2>() {
    @Override protected PVec2 newObject() {
      return new PVec2();
    }
  };
  private float x, y;

  private PVec2() {}

  public static PVec2 obtain() {
    return getStaticPool().obtain();
  }

  /**
   * Adds other to caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  @Override public PVec2 add(PVec2 other) {
    x += other.x;
    y += other.y;
    return this;
  }

  /**
   * Adds scale * other to caller into caller.
   *
   * @param other
   * @param scale
   * @return caller for chaining
   */
  @Override public PVec2 add(PVec2 other, float scale) {
    x += scale * other.x;
    y += scale * other.y;
    return this;
  }

  @Override public float len2() {
    return x * x + y * y;
  }

  @Override public boolean isOnLine(PVec2 other) {
    return (y == 0 && other.y == 0) ? true : (x / y == other.x / other.y);
  }

  /**
   * Performs a dot product.
   *
   * @param other
   * @return caller (dot) other
   */
  @Override public float dot(PVec2 other) {
    return x * other.x + y * other.y;
  }

  @Override public boolean isZero(float margin) {
    if (Math.abs(x()) > margin) {
      return false;
    }
    if (Math.abs(y()) > margin) {
      return false;
    }
    return true;
  }

  /**
   * Multiplies other with caller into caller.
   *
   * @param other
   * @return caller for chaining
   */
  @Override public PVec2 mul(PVec2 other) {
    x *= other.x;
    y *= other.y;
    return this;
  }

  @Override public PVec2 nor() {
    float len = len();
    if (len == 0) {return this;}
    x /= len;
    y /= len;
    return this;
  }

  @Override public PVec2 setZero() {
    x = 0;
    y = 0;
    return this;
  }

  @Override public PVec2 roundComponents(float factor) {
    this.x = (Math.round(this.x * factor) / factor);
    this.y = (Math.round(this.y * factor) / factor);
    return this;
  }

  @Override public PVec2 scl(float scl) {
    x *= scl;
    y *= scl;
    return this;
  }

  @Override public PVec2 sub(PVec2 other) {
    x -= other.x;
    y -= other.y;
    return this;
  }

  public float x() {
    return x;
  }

  public float y() {
    return y;
  }

  public PVec2 add(float x, float y) {
    this.x += x;
    this.y += y;
    return this;
  }

  public float[] emit(float[] out) {
    PAssert.isTrue(out.length == 2);
    out[0] = x;
    out[1] = y;
    return out;
  }

  public float[] emit(float[] out, int offset) {
    PAssert.isTrue(out.length >= offset + 2);
    out[offset + 0] = x;
    out[offset + 1] = y;
    return out;
  }

  public PFloatList emit(PFloatList out, int offset) {
    PAssert.isTrue(out.size() >= offset + 2);
    out.set(offset + 0, x);
    out.set(offset + 1, y);
    return out;
  }

  @Override public boolean equalsT(PVec2 vec2) {
    return PNumberUtils.epsilonEquals(x, vec2.x) && PNumberUtils.epsilonEquals(y, vec2.y);
  }

  @Override public PVec2 lerp(PVec2 other, float mix) {
    x += (other.x - x) * mix;
    y += (other.y - y) * mix;
    return this;
  }

  public PVec2 set(float x, float y) {
    this.x = x;
    this.y = y;
    return this;
  }

  @Override protected PPool staticPool() {
    return getStaticPool();
  }

  @Override public PVec2 set(PVec2 other) {
    this.x = other.x;
    this.y = other.y;
    return this;
  }

  @Override public String toString() {
    return "<" + PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(x), 7) + ", " +
           PStringUtils.prependSpacesToLength(PStringUtils.roundNearestThousandth(y), 7) + ">";
  }

  public float u() {
    return x;
  }

  public PVec2 u(float u) {
    this.x = u;
    return this;
  }

  public float v() {
    return y;
  }

  public PVec2 v(float v) {
    this.y = v;
    return this;
  }

  public PVec2 x(float x) {
    this.x = x;
    return this;
  }

  public PVec2 y(float y) {
    this.y = y;
    return this;
  }
}
