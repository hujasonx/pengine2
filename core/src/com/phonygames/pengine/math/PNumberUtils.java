package com.phonygames.pengine.math;

import com.badlogic.gdx.math.MathUtils;

public class PNumberUtils {
  public static float acos(float x) {
    return MathUtils.acos(clamp(x, -1, 1));
  }

  public static float clamp(float n, float min, float max) {
    return Math.max(Math.min(n, max), min);
  }

  public static float angle(float x1, float y1, float x2, float y2) {
    return MathUtils.atan2(y2 - y1, x2 - x1);
  }

  public static int clamp(int n, int min, int max) {
    return Math.max(Math.min(n, max), min);
  }

  public static float clampRad(float radians) {
    radians += MathUtils.PI;
    return mod(radians, MathUtils.PI2) - MathUtils.PI;
  }

  /**
   * Returns the true modulus a mod b.
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

  public static int compareTo(float a, float b) {
    return (a == b ? 0 : (a > b ? 1 : -1));
  }

  public static boolean epsilonEquals(float x, float y, float epsilon) {
    return Math.abs(x - y) < epsilon;
  }

  public static boolean epsilonEquals(float x, float y) {
    return Math.abs(x - y) < 0.001f;
  }

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

  public static float moveTo(float in, float goal, float speed) {
    if (goal > in) {
      return Math.min(goal, in + speed);
    } else if (goal < in) {
      return Math.max(goal, in - speed);
    }
    return goal;
  }
}
