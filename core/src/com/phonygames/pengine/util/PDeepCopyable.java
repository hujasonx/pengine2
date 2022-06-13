package com.phonygames.pengine.util;

public interface PDeepCopyable<T> {
  public T deepCopy();

  public T deepCopyFrom(T other);
}
