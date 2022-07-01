package com.phonygames.pengine.util;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Collections;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.reflect.ArrayReflection;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PArrayMap<K, V> implements Iterable<ObjectMap.Entry<K, V>> {
  private static final Object dummy = new Object();
  public K[] keys;
  public boolean ordered;
  public int size;
  public V[] values;
  private transient PArrayMap.Entries entries1, entries2;
  private transient PArrayMap.Keys keys1, keys2;
  private transient PArrayMap.Values values1, values2;

  /** Creates an ordered map with a capacity of 16. */
  public PArrayMap() {
    this(true, 16);
  }

  /**
   * @param ordered  If false, methods that remove elements may change the order of other elements in the arrays,
   *                 which avoids a
   *                 memory copy.
   * @param capacity Any elements added beyond this will cause the backing arrays to be grown.
   */
  public PArrayMap(boolean ordered, int capacity) {
    this.ordered = ordered;
    keys = (K[]) new Object[capacity];
    values = (V[]) new Object[capacity];
  }

  /** Creates an ordered map with the specified capacity. */
  public PArrayMap(int capacity) {
    this(true, capacity);
  }

  /** Creates an ordered map with {@link #keys} and {@link #values} of the specified type and a capacity of 16. */
  public PArrayMap(Class keyArrayType, Class valueArrayType) {
    this(false, 16, keyArrayType, valueArrayType);
  }

  /**
   * Creates a new map with {@link #keys} and {@link #values} of the specified type.
   * @param ordered  If false, methods that remove elements may change the order of other elements in the arrays,
   *                 which avoids a
   *                 memory copy.
   * @param capacity Any elements added beyond this will cause the backing arrays to be grown.
   */
  public PArrayMap(boolean ordered, int capacity, Class keyArrayType, Class valueArrayType) {
    this.ordered = ordered;
    keys = (K[]) ArrayReflection.newInstance(keyArrayType, capacity);
    values = (V[]) ArrayReflection.newInstance(valueArrayType, capacity);
  }

  /**
   * Creates a new map containing the elements in the specified map. The new map will have the same type of backing
   * arrays and
   * will be ordered if the specified map is ordered. The capacity is set to the number of elements, so any
   * subsequent elements
   * added will cause the backing arrays to be grown.
   */
  public PArrayMap(PArrayMap array) {
    this(array.ordered, array.size, array.keys.getClass().getComponentType(),
         array.values.getClass().getComponentType());
    size = array.size;
    System.arraycopy(array.keys, 0, keys, 0, size);
    System.arraycopy(array.values, 0, values, 0, size);
  }

  /** Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger. */
  public void clear(int maximumCapacity) {
    if (keys.length <= maximumCapacity) {
      clear();
      return;
    }
    size = 0;
    resize(maximumCapacity);
  }

  public void clear() {
    Arrays.fill(keys, 0, size, null);
    Arrays.fill(values, 0, size, null);
    size = 0;
  }

  protected void resize(int newSize) {
    K[] newKeys = (K[]) ArrayReflection.newInstance(keys.getClass().getComponentType(), newSize);
    System.arraycopy(keys, 0, newKeys, 0, Math.min(size, newKeys.length));
    this.keys = newKeys;
    V[] newValues = (V[]) ArrayReflection.newInstance(values.getClass().getComponentType(), newSize);
    System.arraycopy(values, 0, newValues, 0, Math.min(size, newValues.length));
    this.values = newValues;
  }

  public boolean containsKey(K key) {
    K[] keys = this.keys;
    int i = size - 1;
    if (key == null) {
      while (i >= 0) {if (keys[i--] == key) {return true;}}
    } else {
      while (i >= 0) {if (key.equals(keys[i--])) {return true;}}
    }
    return false;
  }

  /** @param identity If true, == comparison will be used. If false, .equals() comparison will be used. */
  public boolean containsValue(V value, boolean identity) {
    V[] values = this.values;
    int i = size - 1;
    if (identity || value == null) {
      while (i >= 0) {if (values[i--] == value) {return true;}}
    } else {
      while (i >= 0) {if (value.equals(values[i--])) {return true;}}
    }
    return false;
  }

  /**
   * Increases the size of the backing arrays to accommodate the specified number of additional entries. Useful
   * before adding
   * many entries to avoid multiple backing array resizes.
   */
  public void ensureCapacity(int additionalCapacity) {
    if (additionalCapacity < 0) {
      throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
    }
    int sizeNeeded = size + additionalCapacity;
    if (sizeNeeded > keys.length) {resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));}
  }

  /** Uses == for comparison of each value. */
  public boolean equalsIdentity(Object obj) {
    if (obj == this) {return true;}
    if (!(obj instanceof PArrayMap)) {return false;}
    PArrayMap other = (PArrayMap) obj;
    if (other.size != size) {return false;}
    K[] keys = this.keys;
    V[] values = this.values;
    for (int i = 0, n = size; i < n; i++) {if (values[i] != other.get(keys[i], dummy)) {return false;}}
    return true;
  }

  /**
   * Returns the value (which may be null) for the specified key, or the default value if the key is not in the map.
   * Note this
   * does a .equals() comparison of each key in reverse order until the specified key is found.
   */
  public @Null V get(K key, @Null V defaultValue) {
    Object[] keys = this.keys;
    int i = size - 1;
    if (key == null) {
      for (; i >= 0; i--) {if (keys[i] == key) {return values[i];}}
    } else {
      for (; i >= 0; i--) {if (key.equals(keys[i])) {return values[i];}}
    }
    return defaultValue;
  }

  public K firstKey() {
    if (size == 0) {throw new IllegalStateException("Map is empty.");}
    return keys[0];
  }

  public V firstValue() {
    if (size == 0) {throw new IllegalStateException("Map is empty.");}
    return values[0];
  }

  /**
   * Returns the key for the specified value. Note this does a comparison of each value in reverse order until the
   * specified
   * value is found.
   * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
   */
  public @Null K getKey(V value, boolean identity) {
    Object[] values = this.values;
    int i = size - 1;
    if (identity || value == null) {
      for (; i >= 0; i--) {if (values[i] == value) {return keys[i];}}
    } else {
      for (; i >= 0; i--) {if (value.equals(values[i])) {return keys[i];}}
    }
    return null;
  }

  public K getKeyAt(int index) {
    if (index >= size) {throw new IndexOutOfBoundsException(String.valueOf(index));}
    return keys[index];
  }

  public V getValueAt(int index) {
    if (index >= size) {throw new IndexOutOfBoundsException(String.valueOf(index));}
    return values[index];
  }

  public int hashCode() {
    K[] keys = this.keys;
    V[] values = this.values;
    int h = 0;
    for (int i = 0, n = size; i < n; i++) {
      K key = keys[i];
      V value = values[i];
      if (key != null) {h += key.hashCode() * 31;}
      if (value != null) {h += value.hashCode();}
    }
    return h;
  }

  public boolean equals(Object obj) {
    if (obj == this) {return true;}
    if (!(obj instanceof PArrayMap)) {return false;}
    PArrayMap other = (PArrayMap) obj;
    if (other.size != size) {return false;}
    K[] keys = this.keys;
    V[] values = this.values;
    for (int i = 0, n = size; i < n; i++) {
      K key = keys[i];
      V value = values[i];
      if (value == null) {
        if (other.get(key, dummy) != null) {return false;}
      } else {
        if (!value.equals(other.get(key))) {return false;}
      }
    }
    return true;
  }

  /**
   * Returns the value (which may be null) for the specified key, or null if the key is not in the map. Note this does a
   * .equals() comparison of each key in reverse order until the specified key is found.
   */
  public @Null V get(K key) {
    return get(key, null);
  }

  public String toString() {
    if (size == 0) {return "{}";}
    K[] keys = this.keys;
    V[] values = this.values;
    StringBuilder buffer = new StringBuilder(32);
    buffer.append('{');
    buffer.append(keys[0]);
    buffer.append('=');
    buffer.append(values[0]);
    for (int i = 1; i < size; i++) {
      buffer.append(", ");
      buffer.append(keys[i]);
      buffer.append('=');
      buffer.append(values[i]);
    }
    buffer.append('}');
    return buffer.toString();
  }

  public int indexOfValue(V value, boolean identity) {
    Object[] values = this.values;
    if (identity || value == null) {
      for (int i = 0, n = size; i < n; i++) {if (values[i] == value) {return i;}}
    } else {
      for (int i = 0, n = size; i < n; i++) {if (value.equals(values[i])) {return i;}}
    }
    return -1;
  }

  public void insert(int index, K key, V value) {
    if (index > size) {throw new IndexOutOfBoundsException(String.valueOf(index));}
    if (size == keys.length) {resize(Math.max(8, (int) (size * 1.75f)));}
    if (ordered) {
      System.arraycopy(keys, index, keys, index + 1, size - index);
      System.arraycopy(values, index, values, index + 1, size - index);
    } else {
      keys[size] = keys[index];
      values[size] = values[index];
    }
    size++;
    keys[index] = key;
    values[index] = value;
  }

  /** Returns true if the map is empty. */
  public boolean isEmpty() {
    return size == 0;
  }

  @Override public Iterator<ObjectMap.Entry<K, V>> iterator() {
    return entries();
  }

  /**
   * Returns an iterator for the entries in the map. Remove is supported.
   * <p>
   * If {@link Collections#allocateIterators} is false, the same iterator instance is returned each time this method
   * is called.
   * Use the {@link PArrayMap.Entries} constructor for nested or multithreaded iteration.
   * @see Collections#allocateIterators
   */
  public PArrayMap.Entries<K, V> entries() {
    if (Collections.allocateIterators) {return new PArrayMap.Entries(this);}
    if (entries1 == null) {
      entries1 = new PArrayMap.Entries(this);
      entries2 = new PArrayMap.Entries(this);
    }
    if (!entries1.valid) {
      entries1.index = 0;
      entries1.valid = true;
      entries2.valid = false;
      return entries1;
    }
    entries2.index = 0;
    entries2.valid = true;
    entries1.valid = false;
    return entries2;
  }

  /**
   * Returns an iterator for the keys in the map. Remove is supported.
   * <p>
   * If {@link Collections#allocateIterators} is false, the same iterator instance is returned each time this method
   * is called.
   * Use the {@link PArrayMap.Entries} constructor for nested or multithreaded iteration.
   * @see Collections#allocateIterators
   */
  public PArrayMap.Keys<K> keys() {
    if (Collections.allocateIterators) {return new PArrayMap.Keys(this);}
    if (keys1 == null) {
      keys1 = new PArrayMap.Keys(this);
      keys2 = new PArrayMap.Keys(this);
    }
    if (!keys1.valid) {
      keys1.index = 0;
      keys1.valid = true;
      keys2.valid = false;
      return keys1;
    }
    keys2.index = 0;
    keys2.valid = true;
    keys1.valid = false;
    return keys2;
  }

  /** Returns true if the map has one or more items. */
  public boolean notEmpty() {
    return size > 0;
  }

  /** Returns the last key. */
  public K peekKey() {
    return keys[size - 1];
  }

  /** Returns the last value. */
  public V peekValue() {
    return values[size - 1];
  }

  public int put(K key, V value) {
    int index = indexOfKey(key);
    if (index == -1) {
      if (size == keys.length) {resize(Math.max(8, (int) (size * 1.75f)));}
      index = size++;
    }
    keys[index] = key;
    values[index] = value;
    return index;
  }

  public int indexOfKey(K key) {
    Object[] keys = this.keys;
    if (key == null) {
      for (int i = 0, n = size; i < n; i++) {if (keys[i] == key) {return i;}}
    } else {
      for (int i = 0, n = size; i < n; i++) {if (key.equals(keys[i])) {return i;}}
    }
    return -1;
  }

  public int put(K key, V value, int index) {
    int existingIndex = indexOfKey(key);
    if (existingIndex != -1) {removeIndex(existingIndex);} else if (size == keys.length) //
    {resize(Math.max(8, (int) (size * 1.75f)));}
    System.arraycopy(keys, index, keys, index + 1, size - index);
    System.arraycopy(values, index, values, index + 1, size - index);
    keys[index] = key;
    values[index] = value;
    size++;
    return index;
  }

  /** Removes and returns the key/values pair at the specified index. */
  public void removeIndex(int index) {
    if (index >= size) {throw new IndexOutOfBoundsException(String.valueOf(index));}
    Object[] keys = this.keys;
    size--;
    if (ordered) {
      System.arraycopy(keys, index + 1, keys, index, size - index);
      System.arraycopy(values, index + 1, values, index, size - index);
    } else {
      keys[index] = keys[size];
      values[index] = values[size];
    }
    keys[size] = null;
    values[size] = null;
  }

  public void putAll(PArrayMap<? extends K, ? extends V> map) {
    putAll(map, 0, map.size);
  }

  public void putAll(PArrayMap<? extends K, ? extends V> map, int offset, int length) {
    if (offset + length > map.size) {
      throw new IllegalArgumentException(
          "offset + length must be <= size: " + offset + " + " + length + " <= " + map.size);
    }
    int sizeNeeded = size + length - offset;
    if (sizeNeeded >= keys.length) {resize(Math.max(8, (int) (sizeNeeded * 1.75f)));}
    System.arraycopy(map.keys, offset, keys, size, length);
    System.arraycopy(map.values, offset, values, size, length);
    size += length;
  }

  public @Null V removeKey(K key) {
    Object[] keys = this.keys;
    if (key == null) {
      for (int i = 0, n = size; i < n; i++) {
        if (keys[i] == key) {
          V value = values[i];
          removeIndex(i);
          return value;
        }
      }
    } else {
      for (int i = 0, n = size; i < n; i++) {
        if (key.equals(keys[i])) {
          V value = values[i];
          removeIndex(i);
          return value;
        }
      }
    }
    return null;
  }

  public boolean removeValue(V value, boolean identity) {
    Object[] values = this.values;
    if (identity || value == null) {
      for (int i = 0, n = size; i < n; i++) {
        if (values[i] == value) {
          removeIndex(i);
          return true;
        }
      }
    } else {
      for (int i = 0, n = size; i < n; i++) {
        if (value.equals(values[i])) {
          removeIndex(i);
          return true;
        }
      }
    }
    return false;
  }

  public void reverse() {
    for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
      int ii = lastIndex - i;
      K tempKey = keys[i];
      keys[i] = keys[ii];
      keys[ii] = tempKey;
      V tempValue = values[i];
      values[i] = values[ii];
      values[ii] = tempValue;
    }
  }

  public void setKey(int index, K key) {
    if (index >= size) {throw new IndexOutOfBoundsException(String.valueOf(index));}
    keys[index] = key;
  }

  public void setValue(int index, V value) {
    if (index >= size) {throw new IndexOutOfBoundsException(String.valueOf(index));}
    values[index] = value;
  }

  /**
   * Reduces the size of the backing arrays to the size of the actual number of entries. This is useful to release
   * memory when
   * many items have been removed, or if it is known that more entries will not be added.
   */
  public void shrink() {
    if (keys.length == size) {return;}
    resize(size);
  }

  public void shuffle() {
    for (int i = size - 1; i >= 0; i--) {
      int ii = MathUtils.random(i);
      K tempKey = keys[i];
      keys[i] = keys[ii];
      keys[ii] = tempKey;
      V tempValue = values[i];
      values[i] = values[ii];
      values[ii] = tempValue;
    }
  }

  /**
   * Reduces the size of the arrays to the specified size. If the arrays are already smaller than the specified size,
   * no action
   * is taken.
   */
  public void truncate(int newSize) {
    if (size <= newSize) {return;}
    for (int i = newSize; i < size; i++) {
      keys[i] = null;
      values[i] = null;
    }
    size = newSize;
  }

  /**
   * Returns an iterator for the values in the map. Remove is supported.
   * <p>
   * If {@link Collections#allocateIterators} is false, the same iterator instance is returned each time this method
   * is called.
   * Use the {@link PArrayMap.Entries} constructor for nested or multithreaded iteration.
   * @see Collections#allocateIterators
   */
  public PArrayMap.Values<V> values() {
    if (Collections.allocateIterators) {return new PArrayMap.Values(this);}
    if (values1 == null) {
      values1 = new PArrayMap.Values(this);
      values2 = new PArrayMap.Values(this);
    }
    if (!values1.valid) {
      values1.index = 0;
      values1.valid = true;
      values2.valid = false;
      return values1;
    }
    values2.index = 0;
    values2.valid = true;
    values1.valid = false;
    return values2;
  }

  static public class Entries<K, V> implements Iterable<ObjectMap.Entry<K, V>>, Iterator<ObjectMap.Entry<K, V>> {
    private final PArrayMap<K, V> map;
    ObjectMap.Entry<K, V> entry = new ObjectMap.Entry();
    int index;
    boolean valid = true;

    public Entries(PArrayMap<K, V> map) {
      this.map = map;
    }

    @Override public boolean hasNext() {
      if (!valid) {throw new GdxRuntimeException("#iterator() cannot be used nested.");}
      return index < map.size;
    }

    /** Note the same entry instance is returned each time this method is called. */
    @Override public ObjectMap.Entry<K, V> next() {
      if (index >= map.size) {throw new NoSuchElementException(String.valueOf(index));}
      if (!valid) {throw new GdxRuntimeException("#iterator() cannot be used nested.");}
      entry.key = map.keys[index];
      entry.value = map.values[index++];
      return entry;
    }

    @Override public void remove() {
      index--;
      map.removeIndex(index);
    }

    @Override public Iterator<ObjectMap.Entry<K, V>> iterator() {
      return this;
    }

    public void reset() {
      index = 0;
    }
  }

  static public class Keys<K> implements Iterable<K>, Iterator<K> {
    private final PArrayMap<K, Object> map;
    int index;
    boolean valid = true;

    public Keys(PArrayMap<K, Object> map) {
      this.map = map;
    }

    @Override public boolean hasNext() {
      if (!valid) {throw new GdxRuntimeException("#iterator() cannot be used nested.");}
      return index < map.size;
    }

    @Override public K next() {
      if (index >= map.size) {throw new NoSuchElementException(String.valueOf(index));}
      if (!valid) {throw new GdxRuntimeException("#iterator() cannot be used nested.");}
      return map.keys[index++];
    }

    @Override public void remove() {
      index--;
      map.removeIndex(index);
    }

    @Override public Iterator<K> iterator() {
      return this;
    }

    public void reset() {
      index = 0;
    }

    public Array<K> toArray() {
      return new Array(true, map.keys, index, map.size - index);
    }

    public Array<K> toArray(Array array) {
      array.addAll(map.keys, index, map.size - index);
      return array;
    }
  }

  static public class Values<V> implements Iterable<V>, Iterator<V> {
    private final PArrayMap<Object, V> map;
    int index;
    boolean valid = true;

    public Values(PArrayMap<Object, V> map) {
      this.map = map;
    }

    @Override public boolean hasNext() {
      if (!valid) {throw new GdxRuntimeException("#iterator() cannot be used nested.");}
      return index < map.size;
    }

    @Override public V next() {
      if (index >= map.size) {throw new NoSuchElementException(String.valueOf(index));}
      if (!valid) {throw new GdxRuntimeException("#iterator() cannot be used nested.");}
      return map.values[index++];
    }

    @Override public void remove() {
      index--;
      map.removeIndex(index);
    }

    @Override public Iterator<V> iterator() {
      return this;
    }

    public void reset() {
      index = 0;
    }

    public Array<V> toArray() {
      return new Array(true, map.values, index, map.size - index);
    }

    public Array<V> toArray(Array array) {
      array.addAll(map.values, index, map.size - index);
      return array;
    }
  }
}
