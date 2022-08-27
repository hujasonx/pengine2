package com.phonygames.pengine.util.collection;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.util.PPool;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PShortList implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private static final PPool<PShortList> staticPool = new PPool<PShortList>() {
    @Override protected PShortList newObject() {
      return new PShortList();
    }
  };

  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int size = 0;
  private static final int MIN_ARRAY_SIZE = 64;
  private static final int MIN_ARRAY_SIZE_FOR_REGEN_ON_RESET = 256 * 256;
  private short[] values = new short[MIN_ARRAY_SIZE];

  private PShortList() {
  }

  public PShortList add(short f) {
    ensureCapacity(size + 1);
    values[size] = f;
    size++;
    return this;
  }

  public short get(int index) {
    if (index >= size) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return values[PNumberUtils.mod(index, size)];
  }

  public short getOrDefault(int index, short defaultValue) {
    if (index >= size || index < 0) {
      return defaultValue;
    }
    return values[PNumberUtils.mod(index, size)];
  }

  public boolean contains(short value) {
    for (int a = 0; a < size; a++) {
      if (value == values[a]) {
        return true;
      }
    }
    return false;
  }

  public PShortList del(int index) {
    if (index < 0 || index >= size) { return this; }
    for (int a = index + 1; a < size; a++) {
      values[a - 1] = values[a];
    }
    values[size - 1] = 0;
    size--;
    return this;
  }

  public PShortList add(int index, short value) {
    PAssert.isFalse(index < 0);
    ensureCapacity(index + 1);
    for (int a = size; a > index; a--) {
      values[a] = values[a - 1];
    }
    values[index] = value;
    size++;
    return this;
  }

  public PShortList set(int index, short value) {
    PAssert.isFalse(index < 0);
    ensureCapacity(index + 1);
    values[index] = value;
    return this;
  }

  public PShortList sortAscending() {
    Arrays.sort(values,0,size);
    return this;
  }

  public PShortList clear() {
    if (values.length >= MIN_ARRAY_SIZE_FOR_REGEN_ON_RESET) {
      values = new short[MIN_ARRAY_SIZE];
    } else {
      Arrays.fill(values,(short)0);
    }
    size = 0;
    return this;
  }

  public PShortList addAll(PShortList other) {
    ensureCapacity(size + other.size());
    System.arraycopy(other.values,0, values,size(), other.size());
    this.size = size + other.size();
    return this;
  }

  private void ensureCapacity(int capacity) {
    if (capacity >= size) {
      short[] curValues = values;
      values = new short[capacity * 3 / 2];
      System.arraycopy(curValues,0,values,0,size);
    }
  }

  public short[] emit() {
    return emitTo(new short[size()], 0);
  }

  public short[] emitTo(short[] arr, int offsetInArray) {
    return emitTo(arr, offsetInArray,0,size);
  }

  public short[] emitTo(short[] arr, int offsetInArray, int offsetInSelf, int count) {
//    int lastIndexInSelfToEmit = Math.min(arr.length + offsetInSelf, Math.min(size, offsetInSelf + count));
//    int indexInArray = offsetInArray;
//    for (int a = offsetInSelf; a < lastIndexInSelfToEmit; a++) {
//      arr[indexInArray++] = get(a);
//    }
    System.arraycopy(values,offsetInSelf, arr, offsetInArray, count);
    return arr;
  }

  public static PShortList obtain() {
    return staticPool.obtain();
  }

  @Override public void reset() {
    clear();
  }
}