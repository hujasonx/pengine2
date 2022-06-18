package com.phonygames.pengine.util;

import android.support.annotation.Nullable;

import com.badlogic.gdx.utils.Array;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

public abstract class PPool<T extends PPool.Poolable> {
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final static PPool<PoolBuffer> staticPoolBufferPool = new PPool<PoolBuffer>() {
    @Override protected PoolBuffer newObject() {
      return new PPool.PoolBuffer();
    }
  };
  /**
   * The maximum number of objects that will be PPooled.
   */
  public final int max;
  private final Array<T> freeObjects;
  /**
   * The highest number of free objects. Can be reset any time.
   */
  public int peak;

  /**
   * Creates a PPool with an initial capacity of 16 and no maximum.
   */
  public PPool() {
    this(16, Integer.MAX_VALUE);
  }

  /**
   * @param initialCapacity The initial size of the array supporting the PPool. No objects are created/pre-allocated.
   *                        Use
   *                        {@link #fill(int)} after instantiation if needed.
   * @param max             The maximum number of free objects to store in this PPool.
   */
  public PPool(int initialCapacity, int max) {
    freeObjects = new Array(false, initialCapacity);
    this.max = max;
  }

  /**
   * Creates a PPool with the specified initial capacity and no maximum.
   */
  public PPool(int initialCapacity) {
    this(initialCapacity, Integer.MAX_VALUE);
  }

  public static PoolBuffer getBuffer() {
    PoolBuffer b = staticPoolBufferPool().obtain();
    return b;
  }

  /**
   * Returns an object from this PPool. The object may be new (from {@link #newObject()}) or reused (previously
   * {@link #free(T) freed}).
   */
  public T obtain() {
    T t;
    synchronized (freeObjects) {
      t = freeObjects.size == 0 ? newObject() : freeObjects.pop();
    }
    t.setOwnerPool(null);
    return t;
  }

  abstract protected T newObject();

  /**
   * Adds the specified number of new free objects to the PPool. Usually called early on as a pre-allocation
   * mechanism but can be
   * used at any time.
   * @param size the number of objects to be added
   */
  public void fill(int size) {
    synchronized (freeObjects) {
      for (int i = 0; i < size; i++) {if (freeObjects.size < max) {freeObjects.add(newObject());}}
      peak = Math.max(peak, freeObjects.size);
    }
  }

  public void free(@NonNull T t) {
    freeInternal(t);
  }

  private void freeInternal(@NonNull T t) {
    beforeReset(t);
    t.reset();
    PAssert.isNull(t.getOwnerPool(), "Freeing an object that is already held in a pool!");
    t.setOwnerPool(this);
    synchronized (freeObjects) {
      if (freeObjects.size < max) {
        freeObjects.add(t);
      }
    }
  }

  protected void beforeReset(T t) {
  }

  /**
   * Puts the specified objects in the PPool. Null objects within the array are silently ignored.
   * <p>
   * The PPool does not check if an object is already freed, so the same object must not be freed multiple times.
   */
  public void freeAll(@NonNull Array<T> objects) {
    for (T t : objects) {
      freeInternal(t);
    }
    synchronized (freeObjects) {
      peak = Math.max(peak, freeObjects.size);
    }
  }

  public int numFree() {
    synchronized (freeObjects) {
      return freeObjects.size;
    }
  }

  public interface Poolable {
    /* Usage:
     @Getter @Setter private PPool ownerPool;
     */
    @Nullable PPool getOwnerPool();
    void setOwnerPool(PPool pool);
    void reset();
  }

  public final static class PoolBuffer implements Poolable {
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PMat4> mat4s = new PList<>();
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PVec1> vec1s = new PList<>();
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PVec2> vec2s = new PList<>();
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PVec3> vec3s = new PList<>();
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PVec4> vec4s = new PList<>();
    @Getter
    @Setter
    private PPool ownerPool;

    private PoolBuffer() {
    }

    public void finish() {
      staticPoolBufferPool().free(this);
    }

    public boolean isValid() {
      // If the owner pool is unset, that means this is being used somewhere.
      return ownerPool == null;
    }

    public final PMat4 mat4() {
      PMat4 v = PMat4.obtain();
      getMat4s().add(v);
      return v;
    }

    @Override public void reset() {
      for (PVec1 vec1 : getVec1s()) {
        vec1.free();
      }
      getVec1s().clear();
      for (PVec2 vec2 : getVec2s()) {
        vec2.free();
      }
      getVec2s().clear();
      for (PVec3 vec3 : getVec3s()) {
        vec3.free();
      }
      getVec3s().clear();
      for (PVec4 vec4 : getVec4s()) {
        vec4.free();
      }
      getVec4s().clear();
      for (PMat4 mat4 : getMat4s()) {
        mat4.free();
      }
      getMat4s().clear();
    }

    public final PVec3 vec3() {
      PVec3 v = PVec3.obtain();
      getVec3s().add(v);
      return v;
    }

    public final PVec4 vec4() {
      PVec4 v = PVec4.obtain();
      getVec4s().add(v);
      return v;
    }
  }
}