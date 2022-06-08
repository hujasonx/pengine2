package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * List class whose iterator() does not allocate and will always be in order.
 */
public class PList<E> extends ArrayList<E> {
  // Shared iterator, so no allocations should occur.
  private final PListIterator<E> iterator = new PListIterator<>(this);

  public PList() {
  }

  private class PListIterator<E> implements ListIterator<E> {
    private final PList<E> list;
    private int currentIndex = 0;

    public PListIterator(PList<E> list) {
      this.list = list;
    }

    @Override
    public boolean hasNext() {
      return currentIndex + 1 <= size() - 1;
    }

    @Override
    public E next() {
      currentIndex++;
      return list.get(currentIndex);
    }

    @Override
    public boolean hasPrevious() {
      return currentIndex > 0;
    }

    @Override
    public E previous() {
      return list.get(currentIndex - 1);
    }

    @Override
    public int nextIndex() {
      return currentIndex + 1;
    }

    @Override
    public int previousIndex() {
      return currentIndex - 1;
    }

    @Override
    public void remove() {
      list.remove(currentIndex);
    }

    @Override
    public void set(E e) {
      list.set(currentIndex, e);
    }

    @Override
    public void add(E e) {
      list.add(currentIndex, e);
    }
  }

  @Override
  public ListIterator<E> listIterator(int i) {
    iterator.currentIndex = i;
    return iterator;
  }

  @Override
  public ListIterator<E> listIterator() {
    return listIterator(0);
  }

  @Override
  public Iterator<E> iterator() {
    return listIterator();
  }

  public E last() {
    return get(size() - 1);
  }

  @Override
  public void add(int i, E e) {
    super.ensureCapacity(i + 1);
    super.add(i, e);
  }
}
