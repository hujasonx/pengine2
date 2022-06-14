package com.phonygames.pengine.util;

public class PApplicationUtils {
  public static float runtimeUsedMemoryMb() {
    return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
  }
  //  public static class
  //
  //  @Getter(value = AccessLevel.PUBLIC)
}
