package com.phonygames.pengine.util;

public class Duple<K, V> {
  private K k;
  private V v;

  public Duple(K k, V v) {
    this.k = k;
    this.v = v;
  }

  public K getKey() {
    return k;
  }

  public void setKey(K k) {
    this.k = k;
  }

  public V getValue() {
    return v;
  }

  public void setValue(V v) {
    this.v = v;
  }
}
