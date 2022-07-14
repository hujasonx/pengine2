package com.phonygames.pengine.math;

import com.phonygames.pengine.util.PImplementsEquals;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class PInt implements PImplementsEquals<PInt>, PPool.Poolable, Comparable<PInt> {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PInt> staticPool = new PPool<PInt>() {
    @Override protected PInt newObject() {
      return new PInt();
    }
  };
  private int i;

  private PInt() {
  }

  public static PInt obtain() {
    return getStaticPool().obtain();
  }

  @Override public int compareTo(PInt pInt) {
    return PNumberUtils.compareTo(i, pInt.i);
  }

  @Override public int hashCode() {
    return i;
  }

  @Override public boolean equals(Object o) {
    if (o instanceof PInt) {
      return equalsT((PInt) o);
    }
    return false;
  }

  @Override public boolean equalsT(PInt o) {
    return o.i == i;
  }

  @Override public void reset() {
    i = 0;
  }

  public PInt set(int v) {
    this.i = v;
    return this;
  }

  public PInt set(PInt other) {
    this.i = other.i;
    return this;
  }

  public int valueOf() {
    return i;
  }
}
