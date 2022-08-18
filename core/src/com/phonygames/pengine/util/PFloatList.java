package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;
import com.phonygames.pengine.math.PNumberUtils;

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

  private void ensureCapacity(int capacity) {
    if (capacity >= size) {
      float[] curValues = values;
      values = new float[capacity * 3 / 2];
      System.arraycopy(values,0,curValues,0,size);
    }
  }

  public static PFloatList obtain() {
    return staticPool.obtain();
  }

  @Override public void reset() {
    clear();
  }
}