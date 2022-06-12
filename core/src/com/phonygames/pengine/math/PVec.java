package com.phonygames.pengine.math;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public abstract class PVec implements Pool.Poolable {
  public abstract int numComponents();

  public float len() {
    return (float)Math.sqrt(len2());
  }

  public abstract float len2();

  public static abstract class TempBuffer<T extends PVec> {
    protected Array<T> buffer = new Array<>();
    protected Pool<T> pool = new Pool<T>() {
      @Override
      protected T newObject() {
        return TempBuffer.this.newObject();
      }
    };

    public T obtain() {
      T t = pool.obtain();
      buffer.add(t);
      return t;
    }

    public TempBuffer<T> freeAll() {
      pool.freeAll(buffer);
      buffer.clear();
      return this;
    }

    public final void finish() {
      freeAll();
      finishInternal();
    }

    protected abstract void finishInternal();

    public abstract T newObject();
  }
}
