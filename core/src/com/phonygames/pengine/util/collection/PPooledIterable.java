package com.phonygames.pengine.util.collection;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.util.PPool;

import java.util.Iterator;

import lombok.Getter;
import lombok.Setter;

public abstract class PPooledIterable<T> {
  public abstract PPoolableIterator<T> obtainIterator();

  public static abstract class PPoolableIterator<T> implements Iterator<T>, PPool.Poolable, AutoCloseable {
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    protected PPool<PPoolableIterator<T>> returnPool;

    protected PPoolableIterator() {}

    @Override public void close() {
      PAssert.isNotNull(returnPool);
      returnPool.free(this);
      returnPool = null;
    }
  }
}
