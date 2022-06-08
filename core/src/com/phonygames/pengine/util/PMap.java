package com.phonygames.pengine.util;

import com.phonygames.pengine.exception.PAssert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import lombok.NonNull;
import lombok.val;

/**
 * List class whose iterator() does not allocate.
 */
public class PMap<K, V> extends HashMap<K, V> implements Iterable<Map.Entry<K, V>> {
  public PMap<K, V> tryDeepCopyFrom(PMap<K, V> target) {
    for (val e : target.entrySet()) {
      put(tryDeepCopyKey(e.getKey()), tryDeepCopyValue(e.getValue()));
    }

    return this;
  }

  public PMap<K, V> tryDeepCopy() {
    PMap<K, V> ret = new PMap<>();
    return ret.tryDeepCopyFrom(this);
  }

  // Can be overridden to help with deep copies.
  public K tryDeepCopyKey(K k) {
    return k;
  }

  // Can be overridden to help with deep copies.
  public V tryDeepCopyValue(V v) {
    return v;
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return entrySet().iterator();
  }
}

//implements Map<K, V> {
//  // Shared iterator, so no allocations should occur.
////  private final PListIterator<E> iterator = new PListIterator<>(this);
//
//  private int keySetBackingElementLengthPrev = -1;
//  private int keySetNumInvalidPrev = -1;
//  private final PSet<Entry<K, V>> entries = new PSet<>();
//  private final PSet<K> keySet = new PSet<>();
//  private final PList<V> valueList = new PList<>();
//
//  @Override
//  public Set<K> keySet() {
//    return keySet;
//  }
//
//  @Override
//  public Set<Entry<K, V>> entrySet() {
//    return entries;
//  }
//
//  @Override
//  public int size() {
//    return keySet.size();
//  }
//
//  @Override
//  public boolean isEmpty() {
//    return keySet.isEmpty();
//  }
//
//  @Override
//  public boolean containsKey(Object o) {
//    return keySet.contains(o);
//  }
//
//  @Override
//  public boolean containsValue(Object o) {
//    return valueList.contains(o);
//  }
//
//  @Override
//  public V get(Object o) {
//    int index = keySet.indexOfElementInternal((K)o);
//    if (index == -1) {
//      return null;
//    }
//
//    return valueList.get(index);
//  }
//
//  @Override
//  public V put(K k, V v) {
//    int index = keySet.indexOfElementInternal(k);
//    PAssert.isTrue(index != -1);
//
//    valueList.add(index, v);
//    return v;
//  }
//
//  @Override
//  public V remove(Object o) {
//    return null;
//  }
//
//  @Override
//  public void putAll(Map<? extends K, ? extends V> map) {
//    for (val entry : map.entrySet()) {
//      put(entry.getKey(), entry.getValue());
//    }
//  }
//
//  @Override
//  public void clear() {
//    keySet.clear();
//    valueList.clear();
//  }
//
//  @Override
//  public Collection<V> values() {
//    return valueList;
//  }
//
//  @Override
//  public V getOrDefault(Object o, V v) {
//    V ret = get(o);
//    return ret == null ? v : ret;
//  }
//
//  @Override
//  public void forEach(BiConsumer<? super K, ? super V> biConsumer) {
//    PAssert.fail("Not Implemented");
//
//  }
//
//  @Override
//  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> biFunction) {
//    PAssert.fail("Not Implemented");
//
//  }
//
//  @Override
//  public V putIfAbsent(K k, V v) {
//    PAssert.fail("Not Implemented");
//    return null;
//  }
//
//  @Override
//  public boolean remove(Object o, Object o1) {
//    PAssert.fail("Not Implemented");
//    return false;
//  }
//
//  @Override
//  public boolean replace(K k, V v, V v1) {
//    PAssert.fail("Not Implemented");
//    return false;
//  }
//
//  @Override
//  public V replace(K k, V v) {
//    PAssert.fail("Not Implemented");
//    return null;
//  }
//
//  @Override
//  public V computeIfAbsent(K k, Function<? super K, ? extends V> function) {
//    PAssert.fail("Not Implemented");
//    return null;
//  }
//
//  @Override
//  public V computeIfPresent(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
//    PAssert.fail("Not Implemented");
//    return null;
//  }
//
//  @Override
//  public V compute(K k, BiFunction<? super K, ? super V, ? extends V> biFunction) {
//    PAssert.fail("Not Implemented");
//    return null;
//  }
//
//  @Override
//  public V merge(K k, V v, BiFunction<? super V, ? super V, ? extends V> biFunction) {
//    PAssert.fail("Not Implemented");
//    return null;
//  }
//}
