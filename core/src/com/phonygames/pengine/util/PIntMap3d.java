package com.phonygames.pengine.util;

import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public class PIntMap3d<T> extends PMap<Integer, PMap<Integer, PMap<Integer, T>>> {
  private final Iterator<T> iterator = new Iterator<>(this);
  private @Nullable
  final PPool tPool;

  public PIntMap3d() {
    this(null);
  }

  public PIntMap3d(PPool tPool) {
    this.tPool = tPool;
  }

  public T genPooled(int x, int y, int z) {
    T existing = genUnpooled(x).genUnpooled(y).get(z);
    if (existing == null) {
      PAssert.isNotNull(tPool);
      existing = (T) tPool.obtain();
      genUnpooled(x).genUnpooled(y).put(z, existing);
    }
    return existing;
  }

  public T genUnpooled(int x, int y, int z) {
    T existing = genUnpooled(x).genUnpooled(y).get(z);
    if (existing == null) {
      existing = newUnpooled(x, y, z);
      genUnpooled(x).genUnpooled(y).put(z, existing);
    }
    return existing;
  }

  /**
   * Override this to generate values with keys.
   * @param x
   * @param y
   * @param z
   * @return the new object
   */
  protected T newUnpooled(int x, int y, int z) {
    PAssert.failNotImplemented("newUnpooledObject");
    return null;
  }

  public T get(int x, int y, int z) {
    return genUnpooled(x).genUnpooled(y).get(z);
  }

  public Iterator<T> iterator3d() {
    PAssert.isFalse(iterator.hasNext, "Iterator is already in use!");
    return iterator;
  }

  @Override protected PMap<Integer, PMap<Integer, T>> newUnpooled(Integer integer) {
    return new PMap<Integer, PMap<Integer, T>>() {
      @Override protected PMap<Integer, T> newUnpooled(Integer integer) {
        return new PMap<Integer, T>() {
          @Override protected T newUnpooled(Integer integer) {
            PAssert.failNotImplemented("newUnpooled"); // TODO: FIXME
            return super.newUnpooled(integer);
          }
        };
      }
    };
  }

  public T put(int x, int y, int z, T t) {
    genUnpooled(x).genUnpooled(y).put(z, t);
    return t;
  }

  public static class Entry<T> {
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private T val;
    @Getter(value = AccessLevel.PUBLIC)
    @Accessors(fluent = true)
    private int x, y, z;
  }

  public static class Iterator<T> implements java.util.Iterator<Entry<T>>, Iterable<Entry<T>> {
    private final Entry<T> entry = new Entry();
    private final PIntMap3d<T> map;
    private boolean hasNext = false;
    private T nextVal;
    private int nextX, nextY, nextZ;
    private PMapIterator<Integer, PMap<Integer, PMap<Integer, T>>> xIterable;
    private PMapIterator<Integer, PMap<Integer, T>> yIterable;
    private PMapIterator<Integer, T> zIterable;

    private Iterator(PIntMap3d<T> map) {
      this.map = map;
    }

    @Override public boolean hasNext() {
      return hasNext;
    }

    @Override public Entry<T> next() {
      entry.val = nextVal;
      entry.x = nextX;
      entry.y = nextY;
      entry.z = nextZ;
      if (zIterable.hasNext()) {
        PMap.Entry<Integer, T> zEntry = zIterable.next();
        nextZ = zEntry.k();
        nextVal = zEntry.v();
      } else {
        if (yIterable.hasNext()) {
          PMap.Entry<Integer, PMap<Integer, T>> yEntry = yIterable.next();
          nextY = yEntry.k();
          zIterable = yEntry.v().iterator();
          if (zIterable.hasNext()) {
            PMap.Entry<Integer, T> zEntry = zIterable.next();
            nextZ = zEntry.k();
            nextVal = zEntry.v();
          }
        } else {
          if (xIterable.hasNext()) {
            PMap.Entry<Integer, PMap<Integer, PMap<Integer, T>>> xEntry = xIterable.next();
            nextX = xEntry.k();
            yIterable = xEntry.v().iterator();
            if (yIterable.hasNext()) {
              PMap.Entry<Integer, PMap<Integer, T>> yEntry = yIterable.next();
              nextY = yEntry.k();
              zIterable = yEntry.v().iterator();
              if (zIterable.hasNext()) {
                PMap.Entry<Integer, T> zEntry = zIterable.next();
                nextZ = zEntry.k();
                nextVal = zEntry.v();
              }
            }
          } else {
            hasNext = false;
            nextVal = null;
          }
        }
      }
      return entry;
    }

    @Override public void remove() {
      PAssert.failNotImplemented("remove"); // TODO: FIXME
    }

    @Override public java.util.Iterator<Entry<T>> iterator() {
      reset();
      return this;
    }

    private void reset() {
      xIterable = map.iterator();
      entry.val = null;
      nextVal = null;
      hasNext = false;
      PAssert.isNotNull(xIterable);
      if (xIterable.hasNext()) {
        PMap.Entry<Integer, PMap<Integer, PMap<Integer, T>>> xVal = xIterable.next();
        nextX = xVal.k();
        yIterable = xVal.v().iterator();
        if (yIterable.hasNext()) {
          PMap.Entry<Integer, PMap<Integer, T>> yVal = yIterable.next();
          nextY = yVal.k();
          zIterable = yVal.v().iterator();
          if (zIterable.hasNext()) {
            PMap.Entry<Integer, T> zVal = zIterable.next();
            nextZ = zVal.k();
            nextVal = zVal.v();
            hasNext = true;
          }
        }
      }
    }
  }
}
