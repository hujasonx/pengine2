package com.phonygames.pengine.math;

import com.phonygames.pengine.util.PBasic;
import com.phonygames.pengine.util.PPool;

import lombok.Getter;
import lombok.Setter;

public abstract class PVec<V extends PVec> extends PBasic<V> implements PLerpable<V> {
  @Getter
  @Setter
  private PPool ownerPool;

  public PVec() {}

  public abstract V add(V other);
  public abstract V add(V other, float scl);

  @Override public final int compareTo(V v) {
    return PNumberUtils.compareTo(len2(), v.len2());
  }

  public abstract float len2();

  /**
   * @return isOnLine(other, epsilon) && hasSameDirection(other)
   */
  public final boolean isCollinear(V other) {
    return isOnLine(other) && hasSameDirection(other);
  }

  public abstract boolean isOnLine(V other);

  public final boolean hasSameDirection(V other) {
    return dot(other) > 0;
  }

  public abstract float dot(V other);

  public final boolean isZero() {
    return isZero(0.0001f);
  }

  public abstract boolean isZero(float margin);

  public float len() {
    return (float) Math.sqrt(len2());
  }

  public abstract V mul(V other);
  public abstract V nor();

  @Override public final void reset() {
    setZero();
  }

  public abstract V setZero();
  /**
   * Rounds the components of this vector to the reciprocal nearest factor (i.e. input 10 for nearest 10th).
   * @param factor
   * @return
   */
  public abstract V roundComponents(float factor);
  public abstract V scl(float scl);
  public abstract V sub(V other);
}
