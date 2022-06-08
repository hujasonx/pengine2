package com.phonygames.pengine.math;

import com.phonygames.pengine.logging.PLogMessage;

public class PNumberUtils {


  public static boolean isPrimeBruteForce(int number) {
    for (int i = 2; i < number; i++) {
      if (number % i == 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the true modulus a mod b.
   *
   * @param a left side.
   * @param b right side.
   * @return a mod b
   */
  public static int mod(int a, int b) {
    int out = a % b;
    if (out < 0) {
      out += b;
    }
    return out;
  }

  /**
   * Returns the true modulus a mod b.
   *
   * @param a left side.
   * @param b right side.
   * @return a mod b
   */
  public static float mod(float a, float b) {
    float out = a % b;
    if (out < 0) {
      out += b;
    }
    return out;
  }
}
