package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PNumberUtils;

public class PWindowedBuffer<T> {
  private int capacity;
  private Object[] data;
  private int head = 0, tail = 0;

  public PWindowedBuffer(int capacity) {
    this.capacity = capacity;
    this.data = new Object[capacity];
  }

  public void add(T newItem) {
    this.data[head] = newItem;
    head = PNumberUtils.mod(head + 1, capacity);
    if (tail == head) {
      tail = PNumberUtils.mod(tail + 1, capacity);
    }
  }

  public T get(int indexFromHead) {
    if (indexFromHead > filledSize()) {
      PLog.w("PIntWindowedBuffer indexFromHead " + indexFromHead + " is greater than size: " + filledSize()).pEngine();
    }
    int lookupIndex = PNumberUtils.mod(head - 1 - indexFromHead, capacity);
    if (data[lookupIndex] == null) {
      data[lookupIndex] = newItem();
    }
    return (T) data[lookupIndex];
  }

  public int filledSize() {
    if (head > tail) {
      return head - tail;
    }
    return head - tail + capacity;
  }

  /** Override to be allow get(T) to work. Even if nothing was there. */
  protected T newItem() {
    PAssert.failNotImplemented("newItem");
    return null;
  }
}
