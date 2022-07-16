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

  public static int compareTo(float a, float b) {
    return (a == b ? 0 : (a > b ? 1 : -1));
  }

  public static boolean epsilonEquals(float x, float y, float epsilon) {
    return Math.abs(x - y) < epsilon;
  }

  public static boolean epsilonEquals(float x, float y) {
    return Math.abs(x - y) < 0.001f;
  }

  // Generalized smoothstep
  public static float generalSmoothStep(int N, float x) {
    x = clamp(x, 0, 1); // x must be equal to or between 0 and 1
    float result = 0;
    for (int n = 0; n <= N; ++n) {
      result += (float) (pascalTriangle(-N - 1, n) * pascalTriangle(2 * N + 1, N - n) * Math.pow(x, N + n + 1));
    }
    return result;
  }

  /** Returns binomial coefficient without explicit use of factorials, which can't be used with negative integers. */
  public static float pascalTriangle(float a, int b) {
    float result = 1;
    for (int i = 0; i < b; ++i) {
      result *= (a - i) / (i + 1);
    }
    return result;
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

  public static float moveTo(float in, float goal, float speed) {
    if (goal > in) {
      return Math.min(goal, in + speed);
    } else if (goal < in) {
      return Math.max(goal, in - speed);
    }
    return goal;
  }

  public static float smin(float a, float b, float k) {
    if (k <= 0) {
      return Math.min(a, b);
    }
    float h = clamp(0.5f + 0.5f * (a - b) / k, 0.0f, 1.0f);
    return mix(a, b, h) - k * h * (1.0f - h);
  }

  public static float mix(float a, float b, float mixAmount) {
    return a + (b - a) * mixAmount;
  }

  public static float sqrt(float x) {
    return (float) Math.sqrt(x);
  }
}
