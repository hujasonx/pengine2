package com.phonygames.pengine.util;

public class PSort {
  public int compareTo(String c1, String c2) {
    return c1.compareTo(c2);
  }

  public int compareTo(float f1, float f2) {
    if (f1 < f2) {
      return -1;
    }
    if (f1 > f2) {
      return 1;
    }
    return 0;
  }

  public int compareTo(int i1, int i2) {
    if (i1 < i2) {
      return -1;
    }
    if (i1 > i2) {
      return 1;
    }
    return 0;
  }
}
