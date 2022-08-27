package com.phonygames.pengine.util;

import android.support.annotation.Nullable;

import com.badlogic.gdx.utils.Array;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PMat4;
import com.phonygames.pengine.math.PVec1;
import com.phonygames.pengine.math.PVec2;
import com.phonygames.pengine.math.PVec3;
import com.phonygames.pengine.math.PVec4;
import com.phonygames.pengine.util.collection.PList;

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
   * A counter that goes up for every obtain and down for every free.
   */
  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int obtainFreeCounter = 0;

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
    t.setSourcePool(this);
    obtainFreeCounter++;
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
    obtainFreeCounter--;
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
    default void free() {
      if (getSourcePool() != null) {
        getSourcePool().free(this);
      } else {
        PAssert.warn("Attempted to free a poolable with no source pool!");
      }
    }
    @Nullable PPool getSourcePool();
    void setSourcePool(PPool pool);
    /* Usage:
     @Getter @Setter private PPool ownerPool, sourcePool;
     */
    @Nullable PPool getOwnerPool();
    void setOwnerPool(PPool pool);
    void reset();
  }

  public final static class PoolBuffer implements Poolable, AutoCloseable {
    // #pragma mark - PPool.Poolable
    @Getter
    @Setter
    private PPool ownerPool, sourcePool;
    // #pragma end - PPool.Poolable
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PMat4> mat4s = new PList<>(PMat4.getStaticPool());
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PVec1> vec1s = new PList<>(PVec1.getStaticPool());
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PVec2> vec2s = new PList<>(PVec2.getStaticPool());
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PVec3> vec3s = new PList<>(PVec3.getStaticPool());
    @Getter(value = AccessLevel.PRIVATE, lazy = true)
    private final PList<PVec4> vec4s = new PList<>(PVec4.getStaticPool());

    private PoolBuffer() {
    }

    @Override public void close() {
      free();
    }

    public boolean isValid() {
      // If the owner pool is unset, that means this is being used somewhere.
      return ownerPool == null;
    }

    public PMat4 mat4() {
      return getMat4s().genPooledAndAdd();
    }

    @Override public void reset() {
      getMat4s().clearAndFreePooled();
      getVec1s().clearAndFreePooled();
      getVec2s().clearAndFreePooled();
      getVec3s().clearAndFreePooled();
      getVec4s().clearAndFreePooled();
    }

    public PVec1 vec1() {
      return getVec1s().genPooledAndAdd();
    }

    public PVec1 vec1(PVec1 vec) {
      return getVec1s().genPooledAndAdd().set(vec);
    }

    public PVec1 vec1(float x) {
      return getVec1s().genPooledAndAdd().set(x);
    }

    public PVec2 vec2() {return getVec2s().genPooledAndAdd();}

    public PVec2 vec2(PVec2 vec) {
      return getVec2s().genPooledAndAdd().set(vec);
    }

    public PVec2 vec2(float x, float y) {
      return getVec2s().genPooledAndAdd().set(x, y);
    }

    public PVec3 vec3() {
      return getVec3s().genPooledAndAdd();
    }

    public PVec3 vec3(PVec3 vec) {
      return getVec3s().genPooledAndAdd().set(vec);
    }

    public PVec3 vec3(float x, float y, float z) {
      return getVec3s().genPooledAndAdd().set(x, y, z);
    }

    public PVec4 vec4() {
      return getVec4s().genPooledAndAdd();
    }

    public PVec4 vec4(PVec4 vec) {
      return getVec4s().genPooledAndAdd().set(vec);
    }

    public PVec4 vec4(float x, float y, float z, float w) {
      return getVec4s().genPooledAndAdd().set(x, y, z, w);
    }

    public PMat4 mat4(PMat4 mat) {
      return getMat4s().genPooledAndAdd().set(mat);
    }
  }
}