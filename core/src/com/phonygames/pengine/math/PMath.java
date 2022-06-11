package com.phonygames.pengine.math;

public class PMath {
  public static float quadraticFormulaPositive(float a, float b, float c) {
    return (float) (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
  }
}
