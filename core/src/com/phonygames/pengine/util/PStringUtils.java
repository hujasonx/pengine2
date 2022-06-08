package com.phonygames.pengine.util;

import com.phonygames.pengine.math.PNumberUtils;

public class PStringUtils {
  public static String prependSpacesToLength(String s, int length) {
    if (s == null) {
      return null;
    }

    if (s.length() >= length) {
      return s;
    }

    StringBuilder builder = new StringBuilder();
    for (int a = 0; a < length - s.length(); a++) {
      builder.append(' ');
    }
    builder.append(s);
    return builder.toString();
  }

  public static String[] splitByLine(String s) {
    return s.split("\\r?\\n");
  }

  public static String roundNum(float n, int digits) {
    String s = ("" + n);
    if (s.contains("E")) {
      String[] split = s.split("E");
      int exDigits = split[1].length();
      int numDigits = PNumberUtils.clamp(digits - 1 - exDigits, 0, split[0].length());
      if (numDigits == 0) {
        return "";
      }
      if (split[0].charAt(numDigits - 1) == '.') {
        return split[0].substring(0, numDigits - 1) + "E" + split[1];
      }
      return split[0].substring(0, numDigits) + "E" + split[1];
    }
    s = s.substring(0, Math.min(s.length(), digits));
    if (s.charAt(s.length() - 1) == '.') {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }

  public static String roundNearestTenth(float n) {
    double nd = Math.round(10.0 * n);
    String s = ("" + (nd * .1));
    int in = s.indexOf('.');
    if (in != -1) {
      s = s.substring(0, Math.min(s.length(), in + 2));
    }
    return s;
  }

  public static String roundNearestHundredth(float n) {
    double nd = Math.round(100.0 * n);
    String s = ("" + (nd * .01));
    int in = s.indexOf('.');
    if (in != -1) {
      s = s.substring(0, Math.min(s.length(), in + 3));
    }
    return s;
  }

  public static String roundNearestThousandth(float n) {
    double nd = Math.round(1000.0 * n);
    String s = ("" + (nd * .001));
    int in = s.indexOf('.');
    if (in != -1) {
      s = s.substring(0, Math.min(s.length(), in + 4));
    }
    return s;
  }
}
