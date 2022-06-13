package com.phonygames.pengine.util;

import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.phonygames.pengine.exception.PAssert;

import java.util.Iterator;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;

/**
 * List class whose iterator() does not allocate.
 */
public class PMap<K, V> implements Iterable<PMap.Entry<K, V>>, PPool.Poolable {
  private final PMapIterator<K, V> backingIterator = new PMapIterator<K, V>();
  @Getter(lazy = true)
  private final PList<V> genedValues = new PList<>();
  // If set, then genPooled() will work.
  private final PPool genedValuesPool;
  private final MapInterface<K, V> mapInterface;
  @Setter
  private PPool ownerPool;

  public PMap() {
    this.genedValuesPool = null;
    mapInterface = new ArrayMapInterface<>();
  }

  public PMap(@NonNull MapInterface<K, V> mapInterface) {
    this.genedValuesPool = null;
    this.mapInterface = mapInterface;
  }

  public PMap(PPool genedValuesPool) {
    this.genedValuesPool = genedValuesPool;
    mapInterface = new ArrayMapInterface<>();
  }

  public PMap(PPool genedValuesPool, @NonNull MapInterface<K, V> mapInterface) {
    this.genedValuesPool = genedValuesPool;
    this.mapInterface = mapInterface;
  }

  /**
   * Clears the contents of the map.
   */
  public void clear() {
    for (val e : this) {
      if (e.v() instanceof PMap) {
        PAssert.warn("Called clear() on a PMap with PMap children, consider using clearRecursive() instead");
      }
      freeIfManaged(e.v());
    }
    mapInterface.clear();
  }

  /**
   * Frees the value back into the pool if it was generated by this map.
   * @param v
   */
  private final void freeIfManaged(@NonNull V v) {
    if (v instanceof PPool.Poolable && genedValues != null) {
      int indexOfV = getGenedValues().indexOf(v, true);
      if (indexOfV != -1) {
        if (genedValuesPool != null) {
          genedValuesPool.free((PPool.Poolable) v);
        }
        getGenedValues().removeIndex(indexOfV);
      }
    }
  }

  /**
   * Clears the contents of the map.
   */
  public final void clearRecursive() {
    for (val e : this) {
      if (e.v() instanceof PMap) {
        val map = (PMap) e.v();
        map.clearRecursive();
      }
      freeIfManaged(e.v());
    }
    mapInterface.clear();
  }

  public final void delAll(@NonNull Iterable<K> keys) {
    for (K k : keys) {
      del(k);
    }
  }

  public final V del(@NonNull K k) {
    V v = mapInterface.del(k);
    if (v != null) {
      freeIfManaged(v);
    }
    return v;
  }

  /**
   * @param k
   * @return
   */
  public final V genPooled(@NonNull K k) {
    V v = get(k);
    if (v != null) {
      return v;
    }
    PAssert.isNotNull(genedValuesPool, "PMap cannot use genPooled when no pool was set.");
    v = (V) genedValuesPool.obtain();
    getGenedValues().add(v);
    mapInterface.put(k, v);
    return v;
  }

  public V get(@NonNull K k) {
    return mapInterface.get(k);
  }

  /**
   * Caller is responsible for dealing with the results of the allocation.
   * @param k
   * @return
   */
  public final V genUnpooled(@NonNull K k) {
    V v = get(k);
    if (v != null) {
      return v;
    }
    // Generate the result into the object setter.
    v = newUnpooled(k);
    mapInterface.put(k, v);
    return v;
  }

  /**
   * Override this to generate values with keys.
   * @param k key to generate using
   * @return the new object
   */
  protected V newUnpooled(K k) {
    PAssert.failNotImplemented("newUnpooledObject");
    return null;
  }

  public final boolean has(@NonNull K k) {
    return mapInterface.get(k) != null;
  }

  @Override public final PMapIterator<K, V> iterator() {
    PAssert.isTrue(backingIterator.isReadyToReuse());
    backingIterator.backingIterator = mapInterface.iterator();
    backingIterator.mapInterface = mapInterface;
    return backingIterator;
  }

  /**
   * Returns if the value was generated by this map AND should be expected to be pooled when cleared.
   * @param v
   * @return if the value is pooled by this map
   */
  public final boolean owns(V v) {
    return getGenedValues().contains(v, true);
  }

  public final void putAll(@NonNull Iterable<Entry<K, V>> other) {
    for (val e : other) {
      put(e.k(), e.v());
    }
  }

  /**
   * @param k
   * @param v
   * @return the previously existing value at the key.
   */
  public final V put(@NonNull K k, @NonNull V v) {
    V alreadyThere = mapInterface.get(k);
    if (alreadyThere != null) {
      if (alreadyThere == v) {
        return v;
      } else {
        freeIfManaged(alreadyThere);
      }
    }
    return mapInterface.put(k, v);
  }

  @Override public void reset() {
    clearRecursive();
  }

  public PPool getOwnerPool() {
    return ownerPool;
  }

  /**
   * @param other
   * @return whether or not we were able to deepcopy everything.
   */
  public final boolean tryDeepCopyAllFrom(PMap<K, V> other) {
    boolean isTrueDeepCopy = true;
    for (val e : other) {
      K k = e.k();
      if (k instanceof PDeepCopyable) {
        k = ((PDeepCopyable<K>) k).deepCopy();
      } else if (k instanceof String) {
      } else {
        isTrueDeepCopy = false;
      }
      V v = e.v();
      if (v instanceof PDeepCopyable) {
        v = ((PDeepCopyable<V>) v).deepCopy();
      } else if (v instanceof String) {
      } else {
        isTrueDeepCopy = false;
      }
      put(k, v);
    }
    return isTrueDeepCopy;
  }

  protected static final class ArrayMapInterface<K, V> extends MapInterface<K, V> {
    private final ArrayMap<K, V> backingMap = new ArrayMap<>();

    @Override void clear() {
      backingMap.clear();
    }

    @Override V del(K k) {
      V ret = backingMap.removeKey(k);
      return ret;
    }

    @Override V get(K k) {
      return backingMap.get(k);
    }

    @Override K getIteratorKey(Object mapEntry) {
      return ((ObjectMap.Entry<K, V>) mapEntry).key;
    }

    @Override V getIteratorVal(Object mapEntry) {
      return ((ObjectMap.Entry<K, V>) mapEntry).value;
    }

    @Override boolean isStableOrdered() {
      return true;
    }

    @Override V put(K k, V v) {
      V ret = backingMap.get(k);
      backingMap.put(k, v);
      return ret;
    }

    @Override int size() {
      return backingMap.size;
    }

    @Override public Iterator iterator() {
      return backingMap.iterator();
    }
  }

  public static class Entry<K, V> {
    private int index = -1;
    private K k;
    private V v;

    public int index() {
      PAssert.isFalse(index == -1, "PMap.Entry.index() was called but index was not set");
      return index;
    }

    public K k() {
      return k;
    }

    public V v() {
      return v;
    }
  }

  protected static abstract class MapInterface<K, V> implements Iterable {
    abstract void clear();
    abstract V del(K k);
    abstract V get(K k);
    abstract K getIteratorKey(Object mapEntry);
    abstract V getIteratorVal(Object mapEntry);
    abstract boolean isStableOrdered();
    abstract V put(K k, V v);
    abstract int size();
  }

  public static class PMapIterator<K, V> implements Iterator<Entry<K, V>> {
    private final PMap.Entry<K, V> entry = new PMap.Entry<>();
    private Iterator backingIterator;
    private int index;
    private MapInterface<K, V> mapInterface;

    private PMapIterator() {}

    @Override public boolean hasNext() {
      if (backingIterator == null) {
        return false;
      }
      boolean hasNext = backingIterator.hasNext();
      if (!hasNext) {
        reset();
      }
      return hasNext;
    }

    @Override public Entry<K, V> next() {
      PAssert.isNotNull(backingIterator);
      PAssert.isNotNull(mapInterface);
      Object o = backingIterator.next();
      entry.index = mapInterface.isStableOrdered() ? index : -1;
      entry.k = mapInterface.getIteratorKey(o);
      entry.v = mapInterface.getIteratorVal(o);
      index++;
      return entry;
    }

    public void reset() {
      index = 0;
      backingIterator = null;
      mapInterface = null;
    }

    private final boolean isReadyToReuse() {
      return backingIterator == null;
    }
  }
}