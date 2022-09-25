package com.phonygames.pengine.math;

import com.phonygames.pengine.util.PImplementsEquals;
import com.phonygames.pengine.util.PPool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/** A 3-integer vector. */
public class PInt3 implements PImplementsEquals<PInt3>, PPool.Poolable, Comparable<PInt3> {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  @Getter(value = AccessLevel.PUBLIC, lazy = true)
  private static final PPool<PInt3> staticPool = new PPool<PInt3>() {
    @Override protected PInt3 newObject() {
      return new PInt3();
    }
  };
  @Getter(value = AccessLevel.PUBLIC)
  @Setter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int x, y, z;

  private PInt3() {
  }

  public static PInt3 obtain() {
    return getStaticPool().obtain();
  }

  @Override public int compareTo(PInt3 pInt) {
    int r = PNumberUtils.compareTo(x, pInt.x);
    if (r != 0) { return r; }
    r= PNumberUtils.compareTo(y, pInt.y);
    if (r != 0) { return r; }
    return PNumberUtils.compareTo(z, pInt.z);
  }

  @Override public int hashCode() {
    return x + y + z;
  }

  @Override public boolean equals(Object o) {
    if (o instanceof PInt3) {
      return equalsT((PInt3) o);
    }
    return false;
  }

  @Override public boolean equalsT(PInt3 o) {
    return o.x == x && o.y == y && o.z == z;
  }

  @Override public void reset() {
    x = 0;
    y = 0;
    z = 0;
  }

  public PInt3 set(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z  =z ;
    return this;
  }

  public PInt3 set(PInt3 other) {
    this.x = other.x;
    this.y = other.y;
    this.z = other.z;
    return this;
  }

  public PVec3 toPVec3(PVec3 out) {
    return out.set(x, y, z);
  }
}
