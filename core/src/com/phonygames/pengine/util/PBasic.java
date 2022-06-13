package com.phonygames.pengine.util;

import lombok.Getter;
import lombok.Setter;

/**
 * Any class with basic properties that should also have a static pool should use a PBasic.
 *
 * @param <T>
 */
public abstract class PBasic<T extends PBasic>
    implements PDeepCopyable<T>, PPool.Poolable, Comparable<T>, PImplementsEquals<T> {
  @Getter
  @Setter
  private PPool ownerPool;

  @Override
  public T deepCopy() {
    return (T)staticPool().obtain().deepCopyFrom(this);
  }

  @Override
  public final T deepCopyFrom(T other) {
    return set(other);
  }

  /**
   * Frees the object into the given static pool.
   */
  public final void free() {
    staticPool().free((T) this);
  }

  public abstract T set(T other);

  protected abstract PPool<T> staticPool();
}
