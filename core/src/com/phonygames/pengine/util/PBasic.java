package com.phonygames.pengine.util;

import lombok.Getter;
import lombok.Setter;

/**
 * Any class with basic properties that should also have a static pool should use a PBasic.
 * @param <T>
 */
public abstract class PBasic<T extends PBasic>
    implements PDeepCopyable<T>, PPool.Poolable, Comparable<T>, PImplementsEquals<T> {
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;

  @Override public T deepCopy() {
    return (T) staticPool().obtain().deepCopyFrom(this);
  }

  @Override public final T deepCopyFrom(T other) {
    return set(other);
  }

  protected abstract PPool<T> staticPool();
  public abstract T set(T other);
}
