package com.phonygames.pengine.util;

import com.badlogic.gdx.math.MathUtils;

import java.lang.reflect.Array;

public class PArrayUtils {
  public static float sum(float[] floats) {
    float ret = 0;
    for (int a = 0; a < floats.length; a++) {
      ret += floats[a];
    }
    return ret;
  }

  public static int randomIndexWithWeights(float[] weights) {
    float r = MathUtils.random(sum(weights));
    float s = 0;
    for (int a = 0; a < weights.length; a++) {
      s += weights[a];
      if (s >= r) {
        return a;
      }
    }
    return weights.length - 1;
  }
}
