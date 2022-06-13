package com.phonygames.pengine.math;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PBasic;
import com.phonygames.pengine.util.PPool;

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

  @Override public boolean isOnLine(PVec2 other) {
    return (y == 0 && other.y == 0) ? true : (x / y == other.x / other.y);
  }

  private PVec2() { }

  public static PVec2 obtain() {
    return getStaticPool().obtain();
  }

  /**
   * Adds other to caller into caller.
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
   * @param other
   * @param scale
   * @return caller for chaining
   */
  @Override public PVec2 add(PVec2 other, float scale) {
    x += scale * other.x;
    y += scale * other.y;
    return this;
  }

  /**
   * Performs a dot product.
   * @param other
   * @return caller (dot) other
   */
  @Override public float dot(PVec2 other) {
    return x * other.x + y * other.y;
  }

  @Override public boolean equalsT(PVec2 vec2) {
    return PNumberUtils.epsilonEquals(x, vec2.x) && PNumberUtils.epsilonEquals(y, vec2.y);
  }

  @Override public float len2() {
    return x * x + y * y;
  }

  /**
   * Multiplies other with caller into caller.
   * @param other
   * @return caller for chaining
   */
  @Override public PVec2 mul(PVec2 other) {
    x *= other.x;
    y *= other.y;
    return this;
  }

  @Override public PVec2 scl(float scl) {
    x *= scl;
    y *= scl;
    return this;
  }

  public PVec2 set(float x, float y) {
    this.x = x;
    this.y = y;
    return this;
  }

  @Override public PVec2 set(PVec2 other) {
    this.x = other.x;
    this.y = other.y;
    return this;
  }

  @Override public PVec2 setZero() {
    x = 0;
    y = 0;
    return this;
  }

  @Override protected PPool staticPool() {
    return getStaticPool();
  }

  @Override public PVec2 sub(PVec2 other) {
    x -= other.x;
    y -= other.y;
    return this;
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

  public float x() {
    return x;
  }

  public PVec2 x(float x) {
    this.x = x;
    return this;
  }

  public float y() {
    return y;
  }

  public PVec2 y(float y) {
    this.y = y;
    return this;
  }
}
