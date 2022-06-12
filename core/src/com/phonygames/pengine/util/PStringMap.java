package com.phonygames.pengine.util;

public class PStringMap<V> extends PMap<String, V> {
  @Override
  public String deepCopyKey(String s) {
    return s;
  }
}
