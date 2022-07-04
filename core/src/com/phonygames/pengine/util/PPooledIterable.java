package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;

import java.util.Iterator;

import lombok.Getter;
import lombok.Setter;

public abstract class PPooledIterable<T> {
  public abstract PPoolableIterator<T> obtainIterator();

  public static abstract class PPoolableIterator<T> implements Iterator<T>, PPool.Poolable, AutoCloseable {
    @Getter
    @Setter
    public PPool ownerPool;
    protected PPool<PPoolableIterator<T>> returnPool;

    protected PPoolableIterator() {}

    @Override public void close() {
      PAssert.isNotNull(returnPool);
      returnPool.free(this);
      returnPool = null;
    }
  }
}
