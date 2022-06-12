package com.phonygames.pengine.util;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

import lombok.NonNull;

abstract public class PPool<T> {
  /**
   * The maximum number of objects that will be PPooled.
   */
  public final int max;
  /**
   * The highest number of free objects. Can be reset any time.
   */
  public int peak;

  private final Array<T> freeObjects;

  /**
   * Creates a PPool with an initial capacity of 16 and no maximum.
   */
  public PPool() {
    this(16, Integer.MAX_VALUE);
  }

  /**
   * Creates a PPool with the specified initial capacity and no maximum.
   */
  public PPool(int initialCapacity) {
    this(initialCapacity, Integer.MAX_VALUE);
  }

  /**
   * @param initialCapacity The initial size of the array supporting the PPool. No objects are created/pre-allocated. Use
   *                        {@link #fill(int)} after instantiation if needed.
   * @param max             The maximum number of free objects to store in this PPool.
   */
  public PPool(int initialCapacity, int max) {
    freeObjects = new Array(false, initialCapacity);
    this.max = max;
  }

  abstract protected T newObject();

  /**
   * Returns an object from this PPool. The object may be new (from {@link #newObject()}) or reused (previously
   * {@link #free(Object) freed}).
   */
  public T obtain() {
    return freeObjects.size == 0 ? newObject() : freeObjects.pop();
  }

  /**
   * Puts the specified object in the PPool, making it eligible to be returned by {@link #obtain()}. If the PPool already contains
   * {@link #max} free objects, the specified object is {@link #discard(Object) discarded} and not added to the PPool.
   * <p>
   * The PPool does not check if an object is already freed, so the same object must not be freed multiple times.
   */
  public void free(T object) {
    if (object == null) { throw new IllegalArgumentException("object cannot be null."); }
    if (freeObjects.size < max) {
      freeObjects.add(object);
      peak = Math.max(peak, freeObjects.size);
      reset(object);
    } else {
      discard(object);
    }
  }

  /**
   * Adds the specified number of new free objects to the PPool. Usually called early on as a pre-allocation mechanism but can be
   * used at any time.
   *
   * @param size the number of objects to be added
   */
  public void fill(int size) {
    for (int i = 0; i < size; i++) { if (freeObjects.size < max) { freeObjects.add(newObject()); } }
    peak = Math.max(peak, freeObjects.size);
  }

  /**
   * Called when an object is freed to clear the state of the object for possible later reuse. The default implementation calls
   * {@link Pool.Poolable#reset()} if the object is {@link Pool.Poolable}.
   */
  protected void reset(T object) {
    if (object instanceof Pool.Poolable) {
      ((Pool.Poolable) object).reset();
    }
  }

  /**
   * Called when an object is discarded. This is the case when an object is freed, but the maximum capacity of the PPool is reached,
   * and when the PPool is {@link #clear() cleared}
   */
  protected void discard(T object) {
  }

  /**
   * Puts the specified objects in the PPool. Null objects within the array are silently ignored.
   * <p>
   * The PPool does not check if an object is already freed, so the same object must not be freed multiple times.
   *
   * @see #free(Object)
   */
  public void freeAll(Array<T> objects) {
    if (objects == null) { throw new IllegalArgumentException("objects cannot be null."); }
    Array<T> freeObjects = this.freeObjects;
    int max = this.max;
    for (int i = 0, n = objects.size; i < n; i++) {
      T object = objects.get(i);
      if (object == null) { continue; }
      if (freeObjects.size < max) {
        freeObjects.add(object);
        reset(object);
      } else {
        discard(object);
      }
    }
    peak = Math.max(peak, freeObjects.size);
  }

  /**
   * Removes and discards all free objects from this PPool.
   */
  public void clear() {
    for (int i = 0; i < freeObjects.size; i++) {
      T obj = freeObjects.pop();
      discard(obj);
    }
  }

  /**
   * The number of objects available to be obtained.
   */
  public int getFree() {
    return freeObjects.size;
  }

  /**
   * Objects implementing this interface will have {@link #reset()} called when passed to {@link com.badlogic.gdx.utils.PPool#free(Object)}.
   */
  public static interface Poolable {
    /**
     * Resets the object for reuse. Object references should be nulled and fields may be set to default values.
     */
    public void reset();
  }

  /**
   * @param <V>
   */
  public static abstract class BufferListTemplate<V> extends PList<V> {
    private final PPool<V> backingPool;
    private final PPool<BufferListTemplate<V>> sourcePool;

    protected BufferListTemplate(PPool<BufferListTemplate<V>> sourcePool, @NonNull PPool<V> backingPool) {
      this.sourcePool = sourcePool;
      this.backingPool = backingPool;
    }

    public final V obtain() {
      V v = backingPool.obtain();
      add(v);
      return v;
    }

    public final void freeAll() {
      if (size == 0) {
        return;
      }

      backingPool.freeAll(this);
      clear();
    }

    public final void free() {
      freeAll();
      if (sourcePool != null) {
        sourcePool.free(this);
      }
    }

    @Override
    public void reset() {
      clear();
    }
  }
}