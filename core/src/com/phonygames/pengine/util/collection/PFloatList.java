package com.phonygames.pengine.util.collection;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;
import com.phonygames.pengine.util.PPool;

import java.util.Arrays;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class PFloatList implements PPool.Poolable {
  // #pragma mark - PPool.Poolable
  @Getter
  @Setter
  private PPool ownerPool, sourcePool;
  // #pragma end - PPool.Poolable
  private static final PPool<PFloatList> staticPool = new PPool<PFloatList>() {
    @Override protected PFloatList newObject() {
      return new PFloatList();
    }
  };

  @Getter(value = AccessLevel.PUBLIC)
  @Accessors(fluent = true)
  private int size = 0;
  private static final int MIN_ARRAY_SIZE = 64;
  private static final int MIN_ARRAY_SIZE_FOR_REGEN_ON_RESET = 256 * 256;
  private float[] values = new float[MIN_ARRAY_SIZE];

  private PFloatList() {
  }

  public PFloatList add(float f) {
    ensureCapacity(size + 1);
    values[size] = f;
    size++;
    return this;
  }

  /** Adds all floats from the array to the list. */
  public PFloatList addAll(float[] ar) {
    ensureCapacity(size + ar.length);
    System.arraycopy(ar,0, values,size,ar.length);
    size += ar.length;
    return this;
  }

  /** Deletes the first n floats from the list. */
  public PFloatList delFirstN(int n) {
    for (int a = 0; a < size - n; a++) {
      values[a] = values[a + n];
    }
    size -= n;
    return this;
  }

  public float sum() {
    float sum = 0;
    for (int a = 0; a < size; a++) {
      sum += values[a];
    }
    return sum;
  }

  public float get(int index) {
    if (index >= size) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return values[PNumberUtils.mod(index, size)];
  }

  public float getOrDefault(int index, float defaultValue) {
    if (index >= size || index < 0) {
      return defaultValue;
    }
    return values[PNumberUtils.mod(index, size)];
  }

  public boolean contains(float value, float epsilon) {
    for (int a = 0; a < size; a++) {
      if (value - epsilon < values[a] && value + epsilon > values[a]) {
        return true;
      }
    }
    return false;
  }

  public PFloatList del(int index) {
    if (index < 0 || index >= size) { return this; }
    for (int a = index + 1; a < size; a++) {
      values[a - 1] = values[a];
    }
    values[size - 1] = 0;
    size--;
    return this;
  }

  public PFloatList add(int index, float value) {
    PAssert.isFalse(index < 0);
    ensureCapacity(index + 1);
    for (int a = size; a > index; a--) {
      values[a] = values[a - 1];
    }
    values[index] = value;
    size++;
    return this;
  }

  public PFloatList set(int index, float value) {
    PAssert.isFalse(index < 0);
    ensureCapacity(index + 1);
    values[index] = value;
    return this;
  }

  public PFloatList sortAscending() {
    Arrays.sort(values,0,size);
    return this;
  }

  public PFloatList clear() {
    if (values.length >= MIN_ARRAY_SIZE_FOR_REGEN_ON_RESET) {
      values = new float[MIN_ARRAY_SIZE];
    } else {
      Arrays.fill(values,0);
    }
    size = 0;
    return this;
  }

  public PFloatList addAll(PFloatList other) {
    ensureCapacity(size + other.size());
    System.arraycopy(other.values,0, values,size(), other.size());
    this.size = size + other.size();
    return this;
  }

  private void ensureCapacity(int capacity) {
    if (capacity >= values.length) {
      float[] curValues = values;
      values = new float[capacity * 3 / 2];
      System.arraycopy(curValues,0,values,0,size);
    }
  }

  public float[] emit() {
    return emitTo(new float[size()], 0);
  }

  public float[] emitTo(float[] arr, int offsetInArray) {
    return emitTo(arr, offsetInArray,0,size);
  }

  public float[] emitTo(float[] arr, int offsetInArray, int offsetInSelf, int count) {
//    int lastIndexInSelfToEmit = Math.min(arr.length + offsetInSelf, Math.min(size, offsetInSelf + count));
//    int indexInArray = offsetInArray;
//    for (int a = offsetInSelf; a < lastIndexInSelfToEmit; a++) {
//      arr[indexInArray++] = get(a);
//    }
    System.arraycopy(values,offsetInSelf, arr, offsetInArray, count);
    return arr;
  }

  public static PFloatList obtain() {
    return staticPool.obtain();
  }

  @Override public void reset() {
    clear();
  }
}