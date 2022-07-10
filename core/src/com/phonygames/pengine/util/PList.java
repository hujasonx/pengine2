package com.phonygames.pengine.util;

import com.badlogic.gdx.utils.Array;
import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PList<E> extends PPooledIterable<E> implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private final Array<E> backingArray = new Array<>();
  @Getter(value = AccessLevel.PRIVATE, lazy = true)
  @Accessors(fluent = true)
  private final PPool<PPooledIterable.PPoolableIterator<E>> iteratorPool =
      new PPool<PPooledIterable.PPoolableIterator<E>>() {
        @Override protected PPooledIterable.PPoolableIterator<E> newObject() {
          return new PListIterator<>();
        }
      };
  private PPool genedValuesPool;

  public PList() {
  }

  public PList(PPool genedValuesPool) {
    this.genedValuesPool = genedValuesPool;
  }

  public PList<E> addAll(PList<E> es) {
    backingArray.addAll(es.backingArray);
    return this;
  }

  public PList<E> addAll(E[] es) {
    for (int a = 0; a < es.length; a++) {
      backingArray.add(es[a]);
    }
    return this;
  }

  public E peek() {
    return backingArray.peek();
  }

  public E pop() {
    return backingArray.pop();
  }

  public PList<E> clearAndFreePooled() {
    if (genedValuesPool != null) {
      for (int a = 0; a < size(); a++) {
        genedValuesPool.free((PPool.Poolable) get(a));
      }
    }
    backingArray.clear();
    return this;
  }

  public int size() {
    return backingArray.size;
  }

  public E get(int index) {
    return backingArray.get(PNumberUtils.mod(index, backingArray.size));
  }

  public PList<E> clear() {
    if (genedValuesPool != null) {
      PAssert.warn("Called clear() on a PList with a gened values pool, consider using clearAndFreePooled() instead.");
    }
    backingArray.clear();
    return this;
  }

  public PList<E> fillToCapacityWith(int capacity, E e) {
    while (size() < capacity) {
      add(e);
    }
    return this;
  }

  public PList<E> add(E e) {
    backingArray.add(e);
    return this;
  }

  public PList<E> fillToCapacityWithPooledValues(int capacity) {
    PAssert.isNotNull(genedValuesPool, "The new item pool wasn't set.");
    while (size() < capacity) {
      genPooledAndAdd();
    }
    return this;
  }

  public E genPooledAndAdd() {
    E ret = (E) genedValuesPool.obtain();
    add(ret);
    return ret;
  }

  public boolean has(E e, boolean identity) {
    return backingArray.contains(e, identity);
  }

  public boolean hasAll(PList<? extends E> es, boolean identity) {
    return backingArray.containsAll(es.backingArray, identity);
  }

  public int indexOf(E e, boolean identity) {
    return backingArray.indexOf(e, identity);
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  @Override public PPoolableIterator<E> obtainIterator() {
    PListIterator<E> ret = (PListIterator<E>) iteratorPool().obtain();
    ret.returnPool = iteratorPool();
    ret.list = this;
    return ret;
  }

  public E removeIndex(int index) {
    return backingArray.removeIndex(index);
  }

  public E removeLast() {
    if (backingArray.size == 0) {
      return null;
    }
    return backingArray.removeIndex(size() - 1);
  }

  public boolean removeValue(E e, boolean identity) {
    return backingArray.removeValue(e, identity);
  }

  @Override public void reset() {
    backingArray.clear();
  }

  public PList<E> set(int index, E e) {
    backingArray.set(index, e);
    return this;
  }

  public PList<E> sort() {
    backingArray.sort();
    return this;
  }

  public <T> T[] toArray(T[] ts) {
    PAssert.failNotImplemented("toArray");
    return null;
  }

  private class PListIterator<E> extends PPooledIterable.PPoolableIterator<E> {
    private boolean active = false;
    private int currentIndex = -1;
    private PList<E> list;
    // -1 if not found/unset, -2 if at end.
    private int nextIndex = 0;

    public PListIterator() {
    }

    @Override public boolean hasNext() {
      return calcNextIndex() >= 0;
    }

    private int calcNextIndex() {
      if (list.size() <= currentIndex + 1) {
        return -1;
      }
      return currentIndex + 1;
    }

    @Override public E next() {
      currentIndex = nextIndex;
      nextIndex = calcNextIndex();
      return list.get(currentIndex);
    }

    @Override public void remove() {
      list.removeIndex(currentIndex);
    }

    @Override public void reset() {
      currentIndex = -1;
      nextIndex = -1;
      list = null;
    }
  }
}
