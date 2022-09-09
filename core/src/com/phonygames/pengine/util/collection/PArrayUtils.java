package com.phonygames.pengine.util.collection;

import com.badlogic.gdx.math.MathUtils;

public class PArrayUtils {
  public static float sum(float[] floats) {
    float ret = 0;
    for (int a = 0; a < floats.length; a++) {
      ret += floats[a];
    }
    return ret;
  }

  public static float[] floatListToArray(PList<Float> f) {
    float[] ret = new float[f.size()];
    for (int a = 0; a < f.size(); a++) {
      ret[a] = f.get(a);
    }
    return ret;
  }

  public static short[] shortListToArray(PList<Short> f) {
    short[] ret = new short[f.size()];
    for (int a = 0; a < f.size(); a++) {
      ret[a] = f.get(a);
    }
    return ret;
  }

  public static int[] intListToArray(PList<Integer> f) {
    int[] ret = new int[f.size()];
    for (int a = 0; a < f.size(); a++) {
      ret[a] = f.get(a);
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

  public static int randomIndexWithWeights(PFloatList weights) {
    float r = MathUtils.random(weights.sum());
    float s = 0;
    for (int a = 0; a < weights.size(); a++) {
      s += weights.get(a);
      if (s >= r) {
        return a;
      }
    }
    return weights.size() - 1;
  }
}
