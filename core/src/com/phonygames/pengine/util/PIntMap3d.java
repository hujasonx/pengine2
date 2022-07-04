package com.phonygames.pengine.util;

import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PIntMap3d<T> implements Iterable<PIntMap3d.Entry<T>> {
  private final PMap<Integer, PMap<Integer, PMap<Integer, T>>> backingMap =
      new PMap<Integer, PMap<Integer, PMap<Integer, T>>>() {
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
      };
  private final PPool<PIntMap3d.Iterator<T>> iteratorPool = new PPool<Iterator<T>>() {
    @Override protected Iterator<T> newObject() {
      return new Iterator<>(PIntMap3d.this);
    }
  };
  private @Nullable
  final PPool tPool;

  public PIntMap3d() {
    this(null);
  }

  public PIntMap3d(PPool tPool) {
    this.tPool = tPool;
  }

  public T genPooled(int x, int y, int z) {
    T existing = backingMap.genUnpooled(x).genUnpooled(y).get(z);
    if (existing == null) {
      PAssert.isNotNull(tPool);
      existing = (T) tPool.obtain();
      backingMap.genUnpooled(x).genUnpooled(y).put(z, existing);
    }
    return existing;
  }

  public T genUnpooled(int x, int y, int z) {
    T existing = backingMap.genUnpooled(x).genUnpooled(y).get(z);
    if (existing == null) {
      existing = newUnpooled(x, y, z);
      backingMap.genUnpooled(x).genUnpooled(y).put(z, existing);
    }
    return existing;
  }

  public T get(int x, int y, int z) {
    val yMap = backingMap.get(x);
    if (yMap == null) {
      return null;
    }
    val zMap = yMap.get(y);
    if (zMap == null) {
      return null;
    }
    return zMap.get(z);
  }

  @Override public Iterator<T> iterator() {
    Iterator<T> ret = iteratorPool.obtain();
    ret.xIterable = (PMap.PMapIterator<Integer, PMap<Integer, PMap<Integer, T>>>) backingMap.obtainIterator();
    ret.processNext();
    return ret;
    //    return new Iterator<>(this);
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

  public T put(int x, int y, int z, T t) {
    backingMap.genUnpooled(x).genUnpooled(y).put(z, t);
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

  public static class Iterator<T> implements java.util.Iterator<Entry<T>>, Iterable<Entry<T>>, PPool.Poolable {
    private final Entry<T> entry = new Entry();
    private final PIntMap3d<T> map;
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    private boolean hasNext = false;
    private T nextVal;
    private int nextX, nextY, nextZ;
    private PMap.PMapIterator<Integer, PMap<Integer, PMap<Integer, T>>> xIterable;
    private PMap.PMapIterator<Integer, PMap<Integer, T>> yIterable;
    private PMap.PMapIterator<Integer, T> zIterable;

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
      processNext();
      return entry;
    }

    private void processNext() {
      hasNext = true;
      if (zIterable != null && zIterable.hasNext()) {
        PMap.Entry<Integer, T> zEntry = zIterable.next();
        nextZ = zEntry.k();
        nextVal = zEntry.v();
      } else {
        if (yIterable != null && yIterable.hasNext()) {
          PMap.Entry<Integer, PMap<Integer, T>> yEntry = yIterable.next();
          nextY = yEntry.k();
          if (zIterable != null) {zIterable.close();}
          zIterable = (PMap.PMapIterator<Integer, T>) yEntry.v().obtainIterator();
          PAssert.isTrue(zIterable.hasNext());
          PMap.Entry<Integer, T> zEntry = zIterable.next();
          nextZ = zEntry.k();
          nextVal = zEntry.v();
        } else {
          if (xIterable.hasNext()) {
            PMap.Entry<Integer, PMap<Integer, PMap<Integer, T>>> xEntry = xIterable.next();
            nextX = xEntry.k();
            if (yIterable != null) {yIterable.close();}
            yIterable = (PMap.PMapIterator<Integer, PMap<Integer, T>>) xEntry.v().obtainIterator();
            PAssert.isTrue(yIterable.hasNext());
            PMap.Entry<Integer, PMap<Integer, T>> yEntry = yIterable.next();
            nextY = yEntry.k();
            if (zIterable != null) {zIterable.close();}
            zIterable = (PMap.PMapIterator<Integer, T>) yEntry.v().obtainIterator();
            PAssert.isTrue(zIterable.hasNext());
            PMap.Entry<Integer, T> zEntry = zIterable.next();
            nextZ = zEntry.k();
            nextVal = zEntry.v();
            if (nextVal == null) {
              System.out.println();
            }
          } else {
            map.iteratorPool.free(this);
          }
        }
      }
    }

    @Override public void remove() {
      PAssert.isNotNull(zIterable);
      zIterable.remove();
    }

    @Override public java.util.Iterator<Entry<T>> iterator() {
      return this;
    }

    @Override public void reset() {
      if (xIterable != null) {xIterable.close();}
      if (yIterable != null) {yIterable.close();}
      if (zIterable != null) {zIterable.close();}
      xIterable = null;
      yIterable = null;
      zIterable = null;
      nextVal = null;
      hasNext = false;
    }
  }
}
