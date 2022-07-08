package com.phonygames.pengine.util;

import android.support.annotation.Nullable;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.aabb.PIntAABB;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

public class PIntMap3d<T> extends PPooledIterable<PIntMap3d.Entry<T>> {
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
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PPool<PPooledIterable.PPoolableIterator<PIntMap3d.Entry<T>>> iteratorPool =
      new PPool<PPooledIterable.PPoolableIterator<PIntMap3d.Entry<T>>>() {
        @Override protected Iterator<T> newObject() {
          return new Iterator<>();
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

  public PList<T> genValuesList() {
    PList<T> ret = new PList<>();
    try (val it = obtainIterator()) {
      while (it.hasNext()) {
        ret.add(it.next().val());
      }
    }
    return ret;
  }

  public void putAll3d(PIntMap3d<T> other) {
    try (val it = other.obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        put(e.x, e.y, e.z,e.val);
      }
    }
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

  public PIntAABB keyBounds(PIntAABB out) {
    boolean setData = false;
    int minX = 0, maxX = 0, minY = 0, maxY = 0, minZ = 0, maxZ = 0;
    try (val it = obtainIterator()) {
      while (it.hasNext()) {
        val e = it.next();
        if (setData) {
          minX = Math.min(minX, e.x());
          maxX = Math.max(maxX, e.x());
          minY = Math.min(minY, e.y());
          maxY = Math.max(maxY, e.y());
          minZ = Math.min(minZ, e.z());
          maxZ = Math.max(maxZ, e.z());
        } else {
          minX = e.x();
          maxX = e.x();
          minY = e.y();
          maxY = e.y();
          minZ = e.z();
          maxZ = e.z();
        }
        setData = true;
      }
    }
    return out.set(minX, minY, minZ, maxX, maxY, maxZ);
  }

  @Override public final PPooledIterable.PPoolableIterator<PIntMap3d.Entry<T>> obtainIterator() {
    PIntMap3d.Iterator<T> ret = (PIntMap3d.Iterator<T>) iteratorPool().obtain();
    ret.returnPool = iteratorPool();
    ret.map = this;
    ret.xIterable = (PMap.PMapIterator<Integer, PMap<Integer, PMap<Integer, T>>>) backingMap.obtainIterator();
    ret.processNext();
    return ret;
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

  public static class Iterator<T> extends PPooledIterable.PPoolableIterator<PIntMap3d.Entry<T>> {
    private final Entry<T> entry = new Entry();
    private boolean hasNext = false;
    private PIntMap3d<T> map;
    private T nextVal;
    private int nextX, nextY, nextZ;
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    private PMap.PMapIterator<Integer, PMap<Integer, PMap<Integer, T>>> xIterable;
    private PMap.PMapIterator<Integer, PMap<Integer, T>> yIterable;
    private PMap.PMapIterator<Integer, T> zIterable;

    private Iterator() {
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
            reset();
          }
        }
      }
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
      map = null;
    }

    @Override public void remove() {
      PAssert.isNotNull(zIterable);
      zIterable.remove();
    }
  }
}
