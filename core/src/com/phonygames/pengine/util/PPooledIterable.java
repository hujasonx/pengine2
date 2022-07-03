package com.phonygames.pengine.util;

import java.util.Iterator;

import lombok.Getter;
import lombok.Setter;

public abstract class PPooledIterable<T> {
  public abstract PPool<PPoolableIterator<T>> getIteratorPool();

  public final PPoolableIterator<T> obtainIterator() {
    return getIteratorPool().obtain();
  }

  public static abstract class PPoolableIterator<T> implements Iterator<T>, PPool.Poolable, AutoCloseable {
    @Getter
    @Setter
    public PPool ownerPool;
    private PPooledIterable<T> pooledIterable;

    protected PPoolableIterator(PPooledIterable<T> pooledIterable) {
      this.pooledIterable = pooledIterable;
    }

    @Override
    public void close() {
      pooledIterable.getIteratorPool().free(this);
    }
  }
}
