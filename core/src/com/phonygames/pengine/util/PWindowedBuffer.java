package com.phonygames.pengine.util;

import com.phonygames.pengine.logging.PLog;
import com.phonygames.pengine.math.PNumberUtils;

public class PWindowedBuffer {
  private int capacity;
  private int[] data;
  private int head = 0, tail = 0;

  public PWindowedBuffer(int capacity) {
    this.capacity = capacity;
    this.data = new int[capacity];
  }

  public void addInt(int data) {
    this.data[head] = data;
    head = PNumberUtils.mod(head + 1, capacity);
    if (tail == head) {
      tail = PNumberUtils.mod(tail + 1, capacity);
    }
  }

  public int get(int indexFromHead) {
    if (indexFromHead > filledSize()) {
      PLog.w("PIntWindowedBuffer indexFromHead " + indexFromHead + " is greater than size: " + filledSize()).pEngine();
    }
    return data[PNumberUtils.mod(head - 1 - indexFromHead, capacity)];
  }

  public int filledSize() {
    if (head > tail) {
      return head - tail;
    }
    return head - tail + capacity;
  }
}
